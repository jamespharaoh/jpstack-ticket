package wbs.clients.apn.chat.contact.console;

import static wbs.framework.utils.etc.Misc.lessThan;
import static wbs.framework.utils.etc.NumberUtils.moreThan;
import static wbs.framework.utils.etc.OptionalUtils.isPresent;
import static wbs.framework.utils.etc.StringUtils.stringFormat;
import static wbs.sms.gsm.GsmUtils.gsmStringLength;

import javax.inject.Inject;

import lombok.Cleanup;
import wbs.clients.apn.chat.bill.logic.ChatCreditLogic;
import wbs.clients.apn.chat.contact.logic.ChatMessageLogic;
import wbs.clients.apn.chat.contact.model.ChatBlockObjectHelper;
import wbs.clients.apn.chat.contact.model.ChatBlockRec;
import wbs.clients.apn.chat.contact.model.ChatContactObjectHelper;
import wbs.clients.apn.chat.contact.model.ChatContactRec;
import wbs.clients.apn.chat.contact.model.ChatMessageRec;
import wbs.clients.apn.chat.contact.model.ChatMessageStatus;
import wbs.clients.apn.chat.contact.model.ChatMonitorInboxObjectHelper;
import wbs.clients.apn.chat.contact.model.ChatMonitorInboxRec;
import wbs.clients.apn.chat.core.model.ChatRec;
import wbs.clients.apn.chat.user.core.logic.ChatUserLogic;
import wbs.clients.apn.chat.user.core.model.ChatUserRec;
import wbs.console.action.ConsoleAction;
import wbs.console.priv.UserPrivChecker;
import wbs.console.request.ConsoleRequestContext;
import wbs.framework.application.annotations.PrototypeComponent;
import wbs.framework.database.Database;
import wbs.framework.database.Transaction;
import wbs.framework.web.Responder;
import wbs.platform.queue.logic.QueueLogic;
import wbs.platform.service.model.ServiceObjectHelper;
import wbs.platform.text.model.TextObjectHelper;
import wbs.platform.text.model.TextRec;
import wbs.platform.user.console.UserConsoleLogic;
import wbs.platform.user.model.UserObjectHelper;
import wbs.sms.gsm.GsmUtils;

@PrototypeComponent ("chatMonitorInboxFormAction")
public
class ChatMonitorInboxFormAction
	extends ConsoleAction {

	// dependencies

	@Inject
	ChatBlockObjectHelper chatBlockHelper;

	@Inject
	ChatContactObjectHelper chatContactHelper;

	@Inject
	ChatContactNoteConsoleHelper chatContactNoteHelper;

	@Inject
	ChatCreditLogic chatCreditLogic;

	@Inject
	ChatMessageConsoleHelper chatMessageHelper;

	@Inject
	ChatMessageLogic chatMessageLogic;

	@Inject
	ChatMonitorInboxObjectHelper chatMonitorInboxHelper;

	@Inject
	ChatUserLogic chatUserLogic;

	@Inject
	ConsoleRequestContext requestContext;

	@Inject
	Database database;

	@Inject
	UserPrivChecker privChecker;

	@Inject
	QueueLogic queueLogic;

	@Inject
	ServiceObjectHelper serviceHelper;

	@Inject
	TextObjectHelper textHelper;

	@Inject
	UserConsoleLogic userConsoleLogic;

	@Inject
	UserObjectHelper userHelper;

	// details

	@Override
	public
	Responder backupResponder () {

		return responder (
			"chatMonitorInboxFormResponder");

	}

	// implementation

	@Override
	protected
	Responder goReal () {

		// get stuff

		Long monitorInboxId =
			requestContext.stuffInteger (
				"chatMonitorInboxId");

		// get params

		String text =
			requestContext.parameterRequired (
				"text");

		boolean ignore =
			isPresent (
				requestContext.parameter (
					"ignore"));

		boolean note =
			isPresent (
				requestContext.parameter (
					"sendAndNote"));

		// check params

		if (! ignore) {

			if (text.length () == 0) {

				requestContext.addError (
					"Please enter a message");

				return null;

			}

			if (! GsmUtils.gsmStringIsValid (text)) {

				requestContext.addError (
					"Message text is invalid");

				return null;

			}

			if (
				moreThan (
					gsmStringLength (
						text),
					ChatMonitorInboxConsoleLogic.SINGLE_MESSAGE_LENGTH
						* ChatMonitorInboxConsoleLogic.MAX_OUT_MONITOR_MESSAGES)
			) {

				requestContext.addError (
					"Message text is too long");

				return null;

			}

		}

		@Cleanup
		Transaction transaction =
			database.beginReadWrite (
				"ChatMonitorInboxFormAction.goReal ()",
				this);

		ChatMonitorInboxRec chatMonitorInbox =
			chatMonitorInboxHelper.findRequired (
				monitorInboxId);

		ChatUserRec monitorChatUser =
			chatMonitorInbox.getMonitorChatUser ();

		ChatUserRec userChatUser =
			chatMonitorInbox.getUserChatUser ();

		ChatRec chat =
			userChatUser.getChat ();

		if (ignore) {

			// check if they can ignore

			if (! privChecker.canRecursive (
					chat,
					"manage")) {

				requestContext.addError (
					"Can't ignore");

				return null;

			}

		} else {

			if (
				lessThan (
					GsmUtils.gsmStringLength (text),
					chat.getMinMonitorMessageLength ())
			) {

				requestContext.addError (
					stringFormat (
						"Message text is too short (minimum %d)",
						chat.getMinMonitorMessageLength ()));

				return null;

			}

			ChatBlockRec chatBlock =
				chatBlockHelper.find (
					userChatUser,
					monitorChatUser);

			boolean blocked =
				chatBlock != null
				|| userChatUser.getBlockAll ();

			boolean deleted =
				chatUserLogic.deleted (
					userChatUser);

			// create a chat message

			TextRec textRec =
				textHelper.findOrCreate (
					text);

			ChatMessageRec chatMessage =
				chatMessageHelper.insert (
					chatMessageHelper.createInstance ()

				.setChat (
					chat)

				.setFromUser (
					monitorChatUser)

				.setToUser (
					userChatUser)

				.setTimestamp (
					transaction.now ())

				.setOriginalText (
					textRec)

				.setEditedText (
					blocked
						? null
						: textRec)

				.setStatus (
					blocked
						? ChatMessageStatus.blocked
						: ChatMessageStatus.sent)

				.setSender (
					userConsoleLogic.userRequired ())

			);

			// update contact entry, set monitor warning if first message

			ChatContactRec chatContact =
				chatContactHelper.findOrCreate (
					monitorChatUser,
					userChatUser);

			if (
				chatContact.getLastDeliveredMessageTime () == null
				&& ! monitorChatUser.getStealthMonitor ()
			) {

				chatMessage

					.setMonitorWarning (
						true);

			}

			chatContact

				.setLastDeliveredMessageTime (
					transaction.now ());

			// update chat user stats

			if (! (blocked || deleted)) {

				userChatUser

					.setLastReceive (
						transaction.now ());

			}

			// send message

			if (! (blocked || deleted)) {

				chatMessageLogic.chatMessageDeliverToUser (
					chatMessage);

			}

			// charge

			if (! (blocked || deleted)) {

				chatCreditLogic.userReceiveSpend (
					userChatUser,
					1);

			}

			// update monitor last action

			monitorChatUser

				.setLastAction (
					transaction.now ());

			// create a note

			if (note) {

				chatContactNoteHelper.insert (
					chatContactNoteHelper.createInstance ()

					.setUser (
						userChatUser)

					.setMonitor (
						monitorChatUser)

					.setNotes (
						text)

					.setTimestamp (
						transaction.now ())

					.setConsoleUser (
						userConsoleLogic.userRequired ())

					.setChat (
						userChatUser.getChat ())

				);

			}

		}

		// and delete the monitor inbox entry

		queueLogic.processQueueItem (
			chatMonitorInbox.getQueueItem (),
			userConsoleLogic.userRequired ());

		chatMonitorInboxHelper.remove (
			chatMonitorInbox);

		// commit transaction

		transaction.commit ();

		// add notice

		if (ignore) {

			requestContext.addNotice (
				"Message ignored");

		} else if (note) {

			requestContext.addNotice (
				"Message sent and note added");

		} else {

			requestContext.addNotice (
				"Message sent");

		}

		// and return

		return responder (
			"queueHomeResponder");

	}

}