package wbs.services.ticket.core.console;

import static wbs.utils.etc.NumberUtils.integerToDecimalString;
import static wbs.utils.string.StringUtils.stringFormat;

import lombok.NonNull;

import org.joda.time.Instant;

import wbs.console.action.ConsoleAction;
import wbs.console.request.ConsoleRequestContext;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.NamedDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.PrototypeDependency;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.component.manager.ComponentProvider;
import wbs.framework.database.Database;
import wbs.framework.database.OwnedTransaction;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

import wbs.platform.currency.logic.CurrencyLogic;
import wbs.platform.queue.logic.QueueLogic;
import wbs.platform.user.console.UserConsoleLogic;

import wbs.services.ticket.core.model.TicketRec;
import wbs.services.ticket.core.model.TicketTemplateRec;
import wbs.web.responder.WebResponder;

@PrototypeComponent ("ticketPendingFormAction")
public
class TicketPendingFormAction
	extends ConsoleAction {

	// singleton dependencies

	@SingletonDependency
	CurrencyLogic currencyLogic;

	@SingletonDependency
	Database database;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	TicketConsoleHelper ticketHelper;

	@SingletonDependency
	TicketNoteConsoleHelper ticketNoteHelper;

	@SingletonDependency
	TicketTemplateConsoleHelper ticketTemplateHelper;

	@SingletonDependency
	QueueLogic queueLogic;

	@SingletonDependency
	ConsoleRequestContext requestContext;

	@SingletonDependency
	UserConsoleLogic userConsoleLogic;

	// prototype dependencies

	@PrototypeDependency
	@NamedDependency ("ticketPendingFormResponder")
	ComponentProvider <WebResponder> pendingFormResponderProvider;

	@PrototypeDependency
	@NamedDependency ("queueHomeResponder")
	ComponentProvider <WebResponder> queueHomeResponderProvider;

	// details

	@Override
	public
	WebResponder backupResponder (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"backupResponder");

		) {

			return pendingFormResponderProvider.provide (
				taskLogger);

		}

	}

	// implementation

	@Override
	public
	WebResponder goReal (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTransaction transaction =
				database.beginReadWrite (
					logContext,
					parentTaskLogger,
					"goReal");

		) {

			// find message

			TicketRec ticket =
				ticketHelper.findFromContextRequired (
					transaction);

			// sanity check

			if (ticket.getQueueItem () == null)
				throw new RuntimeException ();

			// select template

			TicketTemplateRec template;

			// action to be performed

			template =
				ticketTemplateHelper.findRequired (
					transaction,
					requestContext.parameterIntegerRequired (
						"template"));

			// remove old queue item

			queueLogic.processQueueItem (
				transaction,
				ticket.getQueueItem (),
				userConsoleLogic.userRequired (
					transaction));

			ticket

				.setQueueItem (
					null);

			// update ticket timestamp

			String timpestampString =
				requestContext.parameterRequired (
					stringFormat (
						"timestamp-%s",
						integerToDecimalString (
							template.getTicketState ().getId ())));

			Integer timestamp =
				Integer.parseInt (
					timpestampString);

			if (
				timestamp >= template.getTicketState ().getMinimum ()
				&& timestamp <= template.getTicketState ().getMaximum ()
			) {

				// update ticket state

				ticket

					.setTicketState (
						template.getTicketState ());

				// set new timestamp

				ticket.setTimestamp(
					Instant.now ()
						.plus(timestamp * 1000));

				ticket.setQueued (
					false);

			} else {

				throw new RuntimeException (
					"Timestamp out of bounds");

			}

			// check if a new note was added

			String noteText =
				requestContext.parameterRequired (
					"note-text");

			if (! noteText.isEmpty ()) {

				ticketNoteHelper.insert (
					transaction,
					ticketNoteHelper.createInstance ()

					.setTicket (
						ticket)

					.setIndex (
						ticket.getNumNotes ())

					.setNoteText (
						noteText)

				);

				ticket

					.setNumNotes (
						ticket.getNumNotes () + 1);

			}

			// done

			transaction.commit ();

			requestContext.addNotice (
				"Ticket state changed to " +
				template.getTicketState().toString());

			// return

			return queueHomeResponderProvider.provide (
				transaction);

		}

	}

}
