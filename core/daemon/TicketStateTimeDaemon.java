package wbs.services.ticket.core.daemon;

import static wbs.utils.collection.IterableUtils.iterableMapToList;
import static wbs.utils.etc.LogicUtils.booleanEqual;
import static wbs.utils.etc.Misc.disabled;
import static wbs.utils.etc.NullUtils.isNotNull;
import static wbs.utils.etc.NumberUtils.notLessThanZero;
import static wbs.utils.string.StringUtils.keyEqualsDecimalInteger;

import java.util.List;

import lombok.NonNull;

import org.joda.time.Instant;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.Database;
import wbs.framework.database.OwnedTransaction;
import wbs.framework.exception.ExceptionLogger;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;
import wbs.framework.object.ObjectManager;

import wbs.platform.daemon.SleepingDaemonService;
import wbs.platform.queue.logic.QueueLogic;
import wbs.platform.queue.model.QueueItemRec;
import wbs.platform.service.model.ServiceObjectHelper;
import wbs.platform.text.model.TextObjectHelper;

import wbs.utils.random.RandomLogic;

import wbs.services.ticket.core.model.TicketObjectHelper;
import wbs.services.ticket.core.model.TicketRec;

@SingletonComponent ("ticketStateTimeDaemon")
public
class TicketStateTimeDaemon
	extends SleepingDaemonService {

	// singleton dependencies

	@SingletonDependency
	Database database;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ExceptionLogger exceptionLogger;

	@SingletonDependency
	ObjectManager objectManager;

	@SingletonDependency
	QueueLogic queueLogic;

	@SingletonDependency
	RandomLogic randomLogic;

	@SingletonDependency
	ServiceObjectHelper serviceHelper;

	@SingletonDependency
	TextObjectHelper textHelper;

	@SingletonDependency
	TicketObjectHelper ticketHelper;

	// details

	@Override
	protected
	String friendlyName () {
		return "Ticket state time";
	}

	@Override
	protected
	String backgroundProcessName () {
		return "ticket.state-time";
	}

	// implementation

	@Override
	protected
	void runOnce (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"runOnce");

		) {

			// TODO disabled for now

			if (disabled ()) {
				return;
			}

			taskLogger.debugFormat (
				"Getting all unqueued tickets");

			List <Long> ticketIds =
				getTicketIds (
					taskLogger);

			ticketIds.forEach (
				ticketId ->
					doTicket (
						taskLogger,
						ticketId));

		}

	}

	private
	List <Long> getTicketIds (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTransaction transaction =
				database.beginReadOnly (
					logContext,
					parentTaskLogger,
					"getTicketIds");

		) {

			return iterableMapToList (
				ticketHelper.findUnqueuedTickets (
					transaction),
				TicketRec::getId);

		}

	}

	private
	void doTicket (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull Long ticketId) {

		try (

			OwnedTransaction transaction =
				database.beginReadWrite (
					logContext,
					parentTaskLogger,
					"doTicketTImeCheck",
					keyEqualsDecimalInteger (
						"ticketId",
						ticketId));

		) {

			TicketRec ticket =
				ticketHelper.findRequired (
					transaction,
					ticketId);

			// check if the ticket is already in a queue

			if (
				isNotNull (
					ticket.getQueueItem ())
			) {
				return;
			}

			// check if the ticket is ready to be queued

			Integer timeComparison =
				Instant.now ().compareTo (
					ticket.getTimestamp ());

			if (

				notLessThanZero (
					timeComparison)

				&& booleanEqual (
					ticket.getTicketState ().getShowInQueue (),
					true)

			) {

				// create queue item

				QueueItemRec queueItem =
					queueLogic.createQueueItem (
						transaction,
						ticket.getTicketState (),
						"default",
						ticket,
						ticket,
						ticket.getCode (),
						ticket.getTicketState().toString());

				// add queue item to ticket

				ticket

					.setQueueItem (
						queueItem);

				ticket.setQueued (
					true);

			}

			transaction.commit ();

		}

	}

}
