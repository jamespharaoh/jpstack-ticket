package wbs.sms.customer.daemon;

import static wbs.utils.etc.NullUtils.isNotNull;
import static wbs.utils.etc.OptionalUtils.optionalOf;
import static wbs.utils.etc.OptionalUtils.optionalOrNull;
import static wbs.utils.etc.TypeUtils.genericCastUnchecked;

import com.google.common.base.Optional;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.PrototypeDependency;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.component.manager.ComponentProvider;
import wbs.framework.database.Database;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.logging.LogContext;
import wbs.framework.object.ObjectManager;

import wbs.platform.service.model.ServiceObjectHelper;
import wbs.platform.service.model.ServiceRec;

import wbs.sms.command.model.CommandObjectHelper;
import wbs.sms.command.model.CommandRec;
import wbs.sms.customer.logic.SmsCustomerLogic;
import wbs.sms.customer.model.SmsCustomerManagerRec;
import wbs.sms.customer.model.SmsCustomerObjectHelper;
import wbs.sms.customer.model.SmsCustomerRec;
import wbs.sms.customer.model.SmsCustomerSessionRec;
import wbs.sms.customer.model.SmsCustomerTemplateObjectHelper;
import wbs.sms.customer.model.SmsCustomerTemplateRec;
import wbs.sms.message.core.model.MessageObjectHelper;
import wbs.sms.message.core.model.MessageRec;
import wbs.sms.message.inbox.daemon.CommandHandler;
import wbs.sms.message.inbox.logic.SmsInboxLogic;
import wbs.sms.message.inbox.model.InboxAttemptRec;
import wbs.sms.message.inbox.model.InboxRec;
import wbs.sms.message.outbox.logic.SmsMessageSender;
import wbs.sms.number.list.logic.NumberListLogic;

@Accessors (fluent = true)
@PrototypeComponent ("smsCustomerStopCommand")
public
class SmsCustomerStopCommand
	implements CommandHandler {

	// singleton dependencies

	@SingletonDependency
	CommandObjectHelper commandHelper;

	@SingletonDependency
	Database database;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	MessageObjectHelper messageHelper;

	@SingletonDependency
	NumberListLogic numberListLogic;

	@SingletonDependency
	ObjectManager objectManager;

	@SingletonDependency
	ServiceObjectHelper serviceHelper;

	@SingletonDependency
	SmsCustomerLogic smsCustomerLogic;

	@SingletonDependency
	SmsCustomerObjectHelper smsCustomerHelper;

	@SingletonDependency
	SmsCustomerTemplateObjectHelper smsCustomerTemplateHelper;

	@SingletonDependency
	SmsInboxLogic smsInboxLogic;

	// prototype dependencies

	@PrototypeDependency
	ComponentProvider <SmsMessageSender> messageSenderProvider;

	// properties

	@Getter @Setter
	InboxRec inbox;

	@Getter @Setter
	CommandRec command;

	@Getter @Setter
	Optional<Long> commandRef;

	@Getter @Setter
	String rest;

	// details

	@Override
	public
	String[] getCommandTypes () {

		return new String [] {
			"sms_customer_manager.stop",
		};

	}

	// implementation

	@Override
	public
	InboxAttemptRec handle (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"handle");

		) {

			MessageRec inboundMessage =
				inbox.getMessage ();

			SmsCustomerManagerRec customerManager =
				genericCastUnchecked (
					objectManager.getParentRequired (
						transaction,
						command));

			SmsCustomerRec customer =
				smsCustomerHelper.findOrCreate (
					transaction,
					customerManager,
					inboundMessage.getNumber ());

			ServiceRec stopService =
				serviceHelper.findByCodeRequired (
					transaction,
					customerManager,
					"stop");

			// send stop message

			SmsCustomerTemplateRec stopTemplate =
				smsCustomerTemplateHelper.findByCodeRequired (
					transaction,
					customerManager,
					"stop");

			MessageRec outboundMessage;

			if (stopTemplate == null) {

				outboundMessage = null;

			} else {

				outboundMessage =
					messageSenderProvider.provide (
						transaction)

					.threadId (
						inboundMessage.getThreadId ())

					.number (
						customer.getNumber ())

					.messageText (
						stopTemplate.getText ())

					.numFrom (
						stopTemplate.getNumber ())

					.routerResolve (
						transaction,
						stopTemplate.getRouter ())

					.service (
						stopService)

					.affiliate (
						optionalOrNull (
							smsCustomerLogic.customerAffiliate (
								transaction,
								customer)))

					.send (
						transaction);

			}

			// update session

			SmsCustomerSessionRec activeSession =
				customer.getActiveSession ();

			if (activeSession != null) {

				activeSession

					.setEndTime (
						transaction.now ())

					.setStopMessage (
						outboundMessage);

			}

			customer

				.setLastActionTime (
					transaction.now ())

				.setActiveSession (
					null);

			// add to number list

			if (
				isNotNull (
					customerManager.getStopNumberList ())
			) {

				numberListLogic.addDueToMessage (
					transaction,
					customerManager.getStopNumberList (),
					inboundMessage.getNumber (),
					inboundMessage,
					stopService);

			}

			// process message

			return smsInboxLogic.inboxProcessed (
				transaction,
				inbox,
				optionalOf (
					stopService),
				smsCustomerLogic.customerAffiliate (
					transaction,
					customer),
				command);

		}

	}

}
