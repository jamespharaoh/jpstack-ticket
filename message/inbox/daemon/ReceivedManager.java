package wbs.sms.message.inbox.daemon;

import static wbs.utils.collection.MapUtils.emptyMap;
import static wbs.utils.collection.MapUtils.mapItemForKeyOrDefault;
import static wbs.utils.etc.LogicUtils.parseBooleanYesNoRequired;
import static wbs.utils.etc.NullUtils.ifNull;
import static wbs.utils.etc.NumberUtils.integerToDecimalString;
import static wbs.utils.etc.OptionalUtils.optionalAbsent;
import static wbs.utils.etc.OptionalUtils.optionalFromNullable;
import static wbs.utils.string.StringUtils.emptyStringIfNull;
import static wbs.utils.string.StringUtils.keyEqualsClassSimple;
import static wbs.utils.string.StringUtils.keyEqualsDecimalInteger;
import static wbs.utils.string.StringUtils.stringFormat;

import java.util.List;
import java.util.Set;

import lombok.NonNull;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.component.annotations.WeakSingletonDependency;
import wbs.framework.component.config.WbsConfig;
import wbs.framework.database.Database;
import wbs.framework.database.OwnedTransaction;
import wbs.framework.exception.ExceptionLogger;
import wbs.framework.exception.GenericExceptionResolution;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

import wbs.platform.affiliate.model.AffiliateObjectHelper;
import wbs.platform.daemon.AbstractDaemonService;
import wbs.platform.daemon.QueueBuffer;
import wbs.platform.service.model.ServiceObjectHelper;

import wbs.sms.message.core.model.MessageObjectHelper;
import wbs.sms.message.core.model.MessageRec;
import wbs.sms.message.inbox.logic.SmsInboxLogic;
import wbs.sms.message.inbox.model.InboxAttemptObjectHelper;
import wbs.sms.message.inbox.model.InboxAttemptRec;
import wbs.sms.message.inbox.model.InboxObjectHelper;
import wbs.sms.message.inbox.model.InboxRec;
import wbs.sms.route.core.model.RouteRec;

import wbs.utils.thread.ThreadManager;

@SingletonComponent ("receivedManager")
public
class ReceivedManager
	extends AbstractDaemonService {

	// singleton dependencies

	@SingletonDependency
	AffiliateObjectHelper affiliateHelper;

	@WeakSingletonDependency
	CommandManager commandManager;

	@SingletonDependency
	Database database;

	@SingletonDependency
	ExceptionLogger exceptionLogger;

	@SingletonDependency
	InboxAttemptObjectHelper inboxAttemptHelper;

	@SingletonDependency
	InboxObjectHelper inboxHelper;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	SmsInboxLogic smsInboxLogic;

	@SingletonDependency
	MessageObjectHelper messageHelper;

	@SingletonDependency
	ServiceObjectHelper serviceHelper;

	@SingletonDependency
	ThreadManager threadManager;

	@SingletonDependency
	WbsConfig wbsConfig;

	// configuration

	final
	int sleepInterval = 1000;

	final
	int bufferSize = 128;

	final
	int numWorkerThreads = 8;

	// state

	QueueBuffer<Long,Long> buffer =
		new QueueBuffer<> (
			bufferSize);

	// details

	@Override
	protected
	String friendlyName () {
		return "Received manager";
	}

	/**
	 * Runnable class for a worker thread. Loops until the exit flag is set in
	 * the buffer, Processing messages and updating the database with the
	 * result.
	 */
	class ReceivedThread
		implements Runnable {

		String threadName;

		public
		ReceivedThread (
				String threadName) {

			this.threadName =
				threadName;

		}

		void dumpMessageInfo (
				@NonNull TaskLogger parentTaskLogger,
				@NonNull MessageRec message) {

			try (

				OwnedTaskLogger taskLogger =
					logContext.nestTaskLogger (
						parentTaskLogger,
						"dumpMessageInfo");

			) {

				taskLogger.noticeFormat (
					"%s %s %s %s",
					integerToDecimalString (
						message.getId ()),
					message.getNumFrom (),
					message.getNumTo (),
					message.getText ().getText ());

			}

		}

		void doMessage (
				@NonNull TaskLogger parentTaskLogger,
				@NonNull Long messageId) {

			try (

				OwnedTransaction transaction =
					database.beginReadWrite (
						logContext,
						parentTaskLogger,
						"ReceivedThread.doMessage");

			) {

				InboxRec inbox =
					inboxHelper.findRequired (
						transaction,
						messageId);

				MessageRec message =
					messageHelper.findRequired (
						transaction,
						messageId);

				dumpMessageInfo (
					transaction,
					message);

				RouteRec route =
					message.getRoute ();

				if (route.getCommand () != null) {

					InboxAttemptRec inboxAttempt =
						commandManager.handle (
							transaction,
							inbox,
							route.getCommand (),
							optionalFromNullable (
								message.getRef ()),
							message.getText ().getText ());

					if (inboxAttempt == null)
						throw new NullPointerException ();

				} else {

					smsInboxLogic.inboxNotProcessed (
						transaction,
						inbox,
						optionalAbsent (),
						optionalAbsent (),
						optionalAbsent (),
						"No command for route");

				}

				transaction.commit ();

			}

		}

		void doError (
				@NonNull TaskLogger parentTaskLogger,
				@NonNull Long messageId,
				@NonNull Throwable exception) {

			try (

				OwnedTransaction transaction =
					database.beginReadWrite (
						logContext,
						parentTaskLogger,
						"ReceivedThread.doError",
						keyEqualsDecimalInteger (
							"messageId",
							messageId),
						keyEqualsClassSimple (
							"exception",
							exception.getClass ()));

			) {

				transaction.errorFormatException (
					exception,
					"Error processing command for message %s",
					integerToDecimalString (
						messageId));

				InboxRec inbox =
					inboxHelper.findRequired (
						transaction,
						messageId);

				MessageRec message =
					inbox.getMessage ();

				RouteRec route =
					message.getRoute ();

				exceptionLogger.logThrowable (
					transaction,
					"daemon",
					stringFormat (
						"Route %s",
						route.getCode ()),
					exception,
					optionalAbsent (),
					GenericExceptionResolution.tryAgainLater);

				smsInboxLogic.inboxProcessingFailed (
					transaction,
					inbox,
					stringFormat (
						"Threw %s: %s",
						exception.getClass ().getSimpleName (),
						emptyStringIfNull (
							exception.getMessage ())));

				transaction.commit ();

			}

		}

		@Override
		public
		void run () {

			while (true) {

				// get the next message

				Long messageId;

				try {

					messageId =
						buffer.next ();

				} catch (InterruptedException interruptedException) {
					return;
				}

				// handle it

				try (

					OwnedTaskLogger taskLogger =
						logContext.createTaskLogger (
							"ReceivedThread.run");

				) {

					try {

						doMessage (
							taskLogger,
							messageId);

					} catch (Exception exception) {

						doError (
							taskLogger,
							messageId,
							exception);

					}

					// remove the item from the buffer

					buffer.remove (
						messageId);

				}

			}

		}

	}

	private
	boolean doQuery () {

		try (

			OwnedTaskLogger taskLogger =
				logContext.createTaskLogger (
					"doQuery");

		) {

			return doQueryReal (
				taskLogger);

		}

	}

	private
	boolean doQueryReal (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTransaction transaction =
				database.beginReadOnly (
					logContext,
					parentTaskLogger,
					"doQueryReal");

		) {

			Set <Long> activeMessageids =
				buffer.getKeys ();

			List <InboxRec> inboxes =
				inboxHelper.findPendingLimit (
					transaction,
					transaction.now (),
					buffer.getFullSize ());

			for (
				InboxRec inbox
					: inboxes
			) {

				MessageRec message =
					inbox.getMessage ();

				if (activeMessageids.contains (
						message.getId ()))
					continue;

				buffer.add (
					message.getId (),
					message.getId ());

			}

			return inboxes.size () == buffer.getFullSize ();

		}

	}

	class QueryThread
		implements Runnable {

		@Override
		public
		void run () {
			while (true) {

				// query database

				boolean moreMessages =
					doQuery ();

				// wait for queues to go down or for one second to elapse

				try {

					if (moreMessages) {

						buffer.waitNotFull ();

					} else {

						Thread.sleep (
							sleepInterval);

					}

				} catch (InterruptedException exception) {

					return;

				}

			}

		}

	}

	@Override
	protected
	String getThreadName () {

		throw new UnsupportedOperationException ();

	}

	@Override
	protected
	boolean checkEnabled () {

		return parseBooleanYesNoRequired (
			mapItemForKeyOrDefault (
				ifNull (
					wbsConfig.runtimeSettings (),
					emptyMap ()),
				"received-manager.enable",
				"yes"));

	}

	/**
	 * Creates and starts all threads.
	 */
	@Override
	protected
	void createThreads (
			@NonNull TaskLogger parentTaskLogger) {

		// create database query thread

		Thread thread =
			threadManager.makeThread (
				new QueryThread ());

		thread.setName (
			"RecManA");

		thread.start ();

		registerThread (
			thread);

		// create worker threads

		for (int i = 0; i < numWorkerThreads; i ++) {

			thread =
				threadManager.makeThread (
					new ReceivedThread ("" + i));

			thread.setName (
				"RecMan" + i);

			thread.start ();

			registerThread (
				thread);

		}

	}

}
