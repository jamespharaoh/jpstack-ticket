package wbs.apn.chat.date.daemon;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import lombok.Cleanup;
import lombok.NonNull;
import wbs.apn.chat.core.daemon.ChatPatterns;
import wbs.apn.chat.core.logic.ChatMiscLogic;
import wbs.apn.chat.core.model.ChatRec;
import wbs.apn.chat.date.logic.ChatDateLogic;
import wbs.apn.chat.help.logic.ChatHelpLogLogic;
import wbs.apn.chat.user.core.logic.ChatUserLogic;
import wbs.apn.chat.user.core.model.ChatUserDateMode;
import wbs.apn.chat.user.core.model.ChatUserObjectHelper;
import wbs.apn.chat.user.core.model.ChatUserRec;
import wbs.framework.application.annotations.PrototypeComponent;
import wbs.framework.database.Database;
import wbs.framework.database.Transaction;
import wbs.framework.object.ObjectManager;
import wbs.platform.affiliate.model.AffiliateRec;
import wbs.platform.service.model.ServiceObjectHelper;
import wbs.platform.service.model.ServiceRec;
import wbs.sms.command.model.CommandObjectHelper;
import wbs.sms.command.model.CommandRec;
import wbs.sms.message.core.model.MessageObjectHelper;
import wbs.sms.message.core.model.MessageRec;
import wbs.sms.message.inbox.daemon.CommandHandler;
import wbs.sms.message.inbox.daemon.ReceivedMessage;
import wbs.sms.message.inbox.logic.InboxLogic;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

@PrototypeComponent ("chatDateCommand")
public
class ChatDateCommand
	implements CommandHandler {

	// dependencies

	@Inject
	ChatDateLogic chatDateLogic;

	@Inject
	ChatHelpLogLogic chatHelpLogLogic;

	@Inject
	ChatMiscLogic chatMiscLogic;

	@Inject
	ChatUserObjectHelper chatUserHelper;

	@Inject
	ChatUserLogic chatUserLogic;

	@Inject
	CommandObjectHelper commandHelper;

	@Inject
	Database database;

	@Inject
	InboxLogic inboxLogic;

	@Inject
	MessageObjectHelper messageHelper;

	@Inject
	ObjectManager objectManager;

	@Inject
	ServiceObjectHelper serviceHelper;

	// details

	@Override
	public
	String[] getCommandTypes () {

		return new String[] {
			"chat.date_join_photo",
			"chat.date_join_text",
			"chat.date_upgrade"
		};

	}

	// implementation

	@Override
	public
	void handle (
			int commandId,
			@NonNull ReceivedMessage receivedMessage) {

		@Cleanup
		Transaction transaction =
			database.beginReadWrite ();

		CommandRec command =
			commandHelper.find (
				commandId);

		ChatRec chat =
			(ChatRec) (Object)
			objectManager.getParent (
				command);

		ServiceRec defaultService =
			serviceHelper.findByCode (
				chat,
				"default");

		MessageRec message =
			messageHelper.find (
				receivedMessage.getMessageId ());

		ChatUserRec chatUser =
			chatUserHelper.findOrCreate (
				chat,
				message);

		AffiliateRec affiliate =
			chatUserLogic.getAffiliate (
				chatUser);

		// work out date mode

		if (! validCommandCodes.contains (
				command.getCode ()))
			throw new RuntimeException ();

		ChatUserDateMode dateMode =
			dateModeByCommandCode.get (
				command.getCode ());

		// do join

		if (chatUser.getDateMode () == ChatUserDateMode.none
				&& dateMode != ChatUserDateMode.none) {

			// log request

			chatHelpLogLogic.createChatHelpLogIn (
				chatUser,
				message,
				receivedMessage.getRest (),
				command,
				false);

			// set date mode

			chatDateLogic.userDateStuff (
				chatUser,
				null,
				message,
				dateMode,
				true);

			// process inbox

			inboxLogic.inboxProcessed (
				message,
				defaultService,
				affiliate,
				command);

			transaction.commit ();

			return;

		}

		// do upgrade
		if (chatUser.getDateMode () == ChatUserDateMode.text
				&& dateMode == null
				&& ChatPatterns.yes.matcher (
					receivedMessage.getRest ()).find ()) {

			// log request

			chatHelpLogLogic.createChatHelpLogIn (
				chatUser,
				message,
				receivedMessage.getRest (),
				command,
				false);

			// update user

			chatDateLogic.userDateStuff (
				chatUser,
				null,
				message,
				dateMode,
				true);

			// process inbox

			inboxLogic.inboxProcessed (
				message,
				null,
				null,
				null);

			transaction.commit ();

			return;

		}

		// manually process

		chatHelpLogLogic.createChatHelpLogIn (
			chatUser,
			message,
			receivedMessage.getRest (),
			command,
			true);

		// process inbox

		inboxLogic.inboxProcessed (
			message,
			null,
			null,
			null);

		transaction.commit ();

	}

	final
	static Map<String,ChatUserDateMode> dateModeByCommandCode =
		ImmutableMap.<String,ChatUserDateMode>builder ()
			.put ("date_join_photo", ChatUserDateMode.photo)
			.put ("date_join_text", ChatUserDateMode.text)
			.build ();

	final
	static Set<String> validCommandCodes =
		ImmutableSet.<String>builder ()
			.addAll (dateModeByCommandCode.keySet ())
			.add ("date_upgrade")
			.build ();

}
