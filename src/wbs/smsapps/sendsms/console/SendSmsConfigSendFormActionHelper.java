package wbs.smsapps.sendsms.console;

import static wbs.utils.etc.OptionalUtils.optionalAbsent;

import javax.inject.Provider;

import com.google.common.base.Optional;

import lombok.NonNull;

import wbs.console.combo.AbstractConsoleFormActionHelper;
import wbs.console.request.ConsoleRequestContext;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.PrototypeDependency;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.Transaction;
import wbs.framework.web.Responder;
import wbs.platform.text.console.TextConsoleHelper;
import wbs.platform.text.model.TextRec;
import wbs.platform.user.console.UserConsoleLogic;
import wbs.sms.message.core.model.MessageRec;
import wbs.sms.message.outbox.logic.SmsMessageSender;
import wbs.sms.number.core.console.NumberConsoleHelper;
import wbs.sms.number.core.model.NumberRec;
import wbs.smsapps.sendsms.model.SendSmsConfigRec;

@PrototypeComponent ("sendSmsConfigSendFormActionHelper")
public
class SendSmsConfigSendFormActionHelper
	extends AbstractConsoleFormActionHelper <SendSmsConfigSendForm> {

	// singleton dependencies

	@SingletonDependency
	ConsoleRequestContext requestContext;

	@SingletonDependency
	NumberConsoleHelper smsNumberHelper;

	@SingletonDependency
	SendSmsConfigConsoleHelper sendSmsConfigHelper;

	@SingletonDependency
	SendSmsMessageConsoleHelper sendSmsMessageHelper;

	@SingletonDependency
	TextConsoleHelper textHelper;

	@SingletonDependency
	UserConsoleLogic userConsoleLogic;

	// prototype dependencies

	@PrototypeDependency
	Provider <SmsMessageSender> smsMessageSenderProvider;

	// implementation

	@Override
	public
	SendSmsConfigSendForm constructFormState () {

		return new SendSmsConfigSendForm ();

	}

	@Override
	public
	void updatePassiveFormState (
			@NonNull SendSmsConfigSendForm form) {

		SendSmsConfigRec sendSmsConfig =
			sendSmsConfigHelper.findRequired (
				requestContext.stuffInteger (
					"sendSmsConfigId"));

		form

			.originator (
				sendSmsConfig.getOriginator ());

	}

	@Override
	public
	Optional <Responder> processFormSubmission (
			@NonNull Transaction transaction,
			@NonNull SendSmsConfigSendForm form) {

		// send message

		SendSmsConfigRec sendSmsConfig =
			sendSmsConfigHelper.findRequired (
				requestContext.stuffInteger (
					"sendSmsConfigId"));

		NumberRec smsNumber =
			smsNumberHelper.findOrCreate (
				form.number ());

		TextRec messageBodyText =
			textHelper.findOrCreate (
				form.messageBody ());

		MessageRec smsMessage =
			smsMessageSenderProvider.get ()

			.routerResolve (
				sendSmsConfig.getSmsRouter ())

			.serviceLookup (
				sendSmsConfig,
				"default")

			.numFrom (
				sendSmsConfig.getOriginator ())

			.number (
				smsNumber)

			.messageText (
				messageBodyText)

			.user (
				userConsoleLogic.userRequired ())

			.send ();

		sendSmsMessageHelper.insert (
			sendSmsMessageHelper.createInstance ()

			.setSendSmsConfig (
				sendSmsConfig)

			.setTimestamp (
				transaction.now ())

			.setNumber (
				smsNumber)

			.setOriginator (
				form.originator ())

			.setMessageBody (
				messageBodyText)

			.setUser (
				userConsoleLogic.userRequired ())

			.setMessage (
				smsMessage)

		);

		transaction.commit ();

		requestContext.addNotice (
			"Message sent");

		requestContext.setEmptyFormData ();

		return optionalAbsent ();

	}

}