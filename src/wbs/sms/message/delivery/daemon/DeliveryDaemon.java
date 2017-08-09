package wbs.sms.message.delivery.daemon;

import static wbs.utils.etc.OptionalUtils.optionalAbsent;
import static wbs.utils.string.StringUtils.stringFormat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.component.manager.ComponentProvider;
import wbs.framework.database.Database;
import wbs.framework.database.OwnedTransaction;
import wbs.framework.entity.record.GlobalId;
import wbs.framework.exception.ExceptionLogger;
import wbs.framework.exception.GenericExceptionResolution;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

import wbs.platform.daemon.AbstractDaemonService;
import wbs.platform.daemon.QueueBuffer;

import wbs.sms.message.delivery.model.DeliveryObjectHelper;
import wbs.sms.message.delivery.model.DeliveryRec;
import wbs.sms.message.delivery.model.DeliveryTypeObjectHelper;
import wbs.sms.message.delivery.model.DeliveryTypeRec;

@SingletonComponent ("deliveryDaemon")
public
class DeliveryDaemon
	extends AbstractDaemonService {

	// singleton dependencies

	@SingletonDependency
	Database database;

	@SingletonDependency
	DeliveryObjectHelper deliveryHelper;

	@SingletonDependency
	DeliveryTypeObjectHelper deliveryTypeHelper;

	@SingletonDependency
	ExceptionLogger exceptionLogger;

	@SingletonDependency
	Map <String, ComponentProvider <DeliveryHandler>> handlersByBeanName;

	@ClassSingletonDependency
	LogContext logContext;

	// properties

	@Getter @Setter
	int bufferSize = 128;

	@Getter @Setter
	int numWorkerThreads = 4;

	// state

	QueueBuffer <Long, DeliveryRec> buffer;
	Map <Long, DeliveryHandler> handlersById;

	// details

	@Override
	protected
	String friendlyName () {
		return "SMS message delivery";
	}

	// implementation

	@Override
	protected
	void setupService (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTransaction transaction =
				database.beginReadOnly (
					logContext,
					parentTaskLogger,
					"setupService");

		) {

			buffer =
				new QueueBuffer<> (
					bufferSize);

			handlersById =
				new HashMap<> ();

			for (
				Map.Entry <String, ComponentProvider <DeliveryHandler>>
					handlerEntry
						: handlersByBeanName.entrySet ()
			) {

				DeliveryHandler handler =
					handlerEntry.getValue ().provide (
						transaction);

				for (
					String deliveryTypeCode
						: handler.getDeliveryTypeCodes ()
				) {

					DeliveryTypeRec deliveryType =
						deliveryTypeHelper.findByCodeRequired (
							transaction,
							GlobalId.root,
							deliveryTypeCode);

					handlersById.put (
						deliveryType.getId (),
						handler);

				}

			}

		}

	}

	@Override
	protected
	void serviceTeardown (
			@NonNull TaskLogger parentTaskLogger) {

		buffer = null;

		handlersById = null;

	}

	@Override
	protected
	String getThreadName () {
		throw new UnsupportedOperationException ();
	}

	@Override
	protected
	void createThreads (
			@NonNull TaskLogger parentTaskLogger) {

		Thread thread = threadManager.makeThread (new QueryThread ());
		thread.setName ("DelivQ");
		thread.start ();
		registerThread (thread);

		for (int i = 0; i < numWorkerThreads; i++) {
			thread = threadManager.makeThread(new WorkerThread ());
			thread.setName ("Deliv" + i);
			thread.start ();
			registerThread (thread);
		}
	}

	class QueryThread
		implements Runnable {

		@Override
		public
		void run () {

			try {

				while (true) {

					Set <Long> activeIds =
						buffer.getKeys ();

					int numFound =
						pollDatabase (
							activeIds);

					if (numFound < buffer.getFullSize ()) {

						Thread.sleep (
							1000);

					} else {

						buffer.waitNotFull ();

					}

				}

			} catch (InterruptedException exception) {

				return;

			}

		}

		int pollDatabase (
				@NonNull Set <Long> activeIds) {

			try (

				OwnedTaskLogger taskLogger =
					logContext.createTaskLogger (
						"pollDatabase");

			) {

				return pollDatabaseReal (
					taskLogger,
					activeIds);

			}

		}

		int pollDatabaseReal (
				@NonNull TaskLogger parentTaskLogger,
				@NonNull Set <Long> activeIds) {

			try (

				OwnedTransaction transaction =
					database.beginReadOnly (
						logContext,
						parentTaskLogger,
						"QueryThread.pollDatabase");

			) {

				int numFound = 0;

				List <DeliveryRec> deliveries =
					deliveryHelper.findAllLimit (
						transaction,
						buffer.getFullSize ());

				for (
					DeliveryRec delivery
						: deliveries
				) {

					numFound ++;

					// if this one is already being worked on, skip it

					if (activeIds.contains (
							delivery.getId ()))
						continue;

					// make sure the delivery notice type is not a proxy

					transaction.fetch (
						delivery,
						delivery.getMessage (),
						delivery.getMessage ().getDeliveryType ());

					// and add this to the buffer

					buffer.add (
						delivery.getId (),
						delivery);

				}

				return numFound;

			}

		}

	}

	class WorkerThread
		implements Runnable {

		@Override
		public
		void run () {

			while (true) {

				DeliveryRec delivery;

				try {
					delivery = buffer.next ();
				} catch (InterruptedException e) {
					return;
				}

				try (

					OwnedTaskLogger taskLogger =
						logContext.createTaskLogger (
							"worker.run");

				) {

					DeliveryTypeRec deliveryType =
						delivery.getMessage ().getDeliveryType ();

					DeliveryHandler handler =
						handlersById.get (
							deliveryType.getId ());

					try {

						if (handler == null) {

							throw new RuntimeException (
								stringFormat (
									"No delivery notice handler for %s",
									deliveryType.getCode ()));

						}

						handler.handle (
							taskLogger,
							delivery.getId (),
							delivery.getMessage ().getRef ());

					} catch (Exception exception) {

						exceptionLogger.logThrowable (
							taskLogger,
							"daemon",
							"Delivery notice daemon",
							exception,
							optionalAbsent (),
							GenericExceptionResolution.tryAgainLater);

					}

					buffer.remove (
						delivery.getId ());

				}

			}

		}

	}

}
