package wbs.sms.message.inbox.console;

import static wbs.utils.etc.Misc.isNotNull;
import static wbs.utils.etc.NumberUtils.integerToDecimalString;
import static wbs.utils.string.StringUtils.stringFormat;
import static wbs.web.utils.HtmlAttributeUtils.htmlColumnSpanAttribute;
import static wbs.web.utils.HtmlAttributeUtils.htmlRowSpanAttribute;
import static wbs.web.utils.HtmlFormUtils.htmlFormClose;
import static wbs.web.utils.HtmlFormUtils.htmlFormOpenPost;
import static wbs.web.utils.HtmlTableUtils.htmlTableCellWrite;
import static wbs.web.utils.HtmlTableUtils.htmlTableCellWriteHtml;
import static wbs.web.utils.HtmlTableUtils.htmlTableClose;
import static wbs.web.utils.HtmlTableUtils.htmlTableHeaderRowWrite;
import static wbs.web.utils.HtmlTableUtils.htmlTableOpenList;
import static wbs.web.utils.HtmlTableUtils.htmlTableRowClose;
import static wbs.web.utils.HtmlTableUtils.htmlTableRowOpen;
import static wbs.web.utils.HtmlTableUtils.htmlTableRowSeparatorWrite;

import java.util.List;

import lombok.NonNull;

import wbs.console.helper.manager.ConsoleObjectManager;
import wbs.console.part.AbstractPagePart;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.TaskLogger;

import wbs.platform.user.console.UserConsoleLogic;

import wbs.sms.message.core.model.MessageRec;
import wbs.sms.message.inbox.model.InboxObjectHelper;
import wbs.sms.message.inbox.model.InboxRec;

@PrototypeComponent ("messageInboxSummaryPart")
public
class MessageInboxSummaryPart
	extends AbstractPagePart {

	// singleton dependencies

	@SingletonDependency
	InboxObjectHelper inboxHelper;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ConsoleObjectManager objectManager;

	@SingletonDependency
	UserConsoleLogic userConsoleLogic;

	// state

	List <InboxRec> inboxes;

	// implementation

	@Override
	public
	void prepare (
			@NonNull TaskLogger parentTaskLogger) {

		inboxes =
			inboxHelper.findPendingLimit (
				1000l);

	}

	@Override
	public
	void renderHtmlBodyContent (
			@NonNull TaskLogger parentTaskLogger) {

		TaskLogger taskLogger =
			logContext.nestTaskLogger (
				parentTaskLogger,
				"renderHtmlBodyContent");

		htmlFormOpenPost ();

		htmlTableOpenList ();

		htmlTableHeaderRowWrite (
			"Message",
			"From",
			"To",
			"Created",
			"Tries",
			"Next try",
			"Route",
			"Actions");

		for (
			InboxRec inbox
				: inboxes
		) {

			htmlTableRowSeparatorWrite ();

			// message

			MessageRec message =
				inbox.getMessage ();

			htmlTableRowOpen ();

			objectManager.writeTdForObjectMiniLink (
				taskLogger,
				message);

			objectManager.writeTdForObjectMiniLink (
				taskLogger,
				message.getNumber ());

			htmlTableCellWrite (
				message.getNumTo ());

			htmlTableCellWrite (
				userConsoleLogic.timestampWithoutTimezoneString (
					message.getCreatedTime ()));

			htmlTableCellWrite (
				integerToDecimalString (
					inbox.getNumAttempts ()));

			htmlTableCellWrite (
				userConsoleLogic.timestampWithoutTimezoneString (
					inbox.getNextAttempt ()));

			objectManager.writeTdForObjectMiniLink (
				taskLogger,
				message.getRoute ());

			htmlTableCellWriteHtml (
				stringFormat (
					"<input",
					" type=\"submit\"",
					" name=\"ignore_%h\"",
					integerToDecimalString (
						message.getId ()),
					" value=\"cancel\"",
					">"),
				htmlRowSpanAttribute (3l));

			htmlTableRowClose ();

			// message text

			htmlTableRowOpen ();

			htmlTableCellWrite (
				message.getText ().getText (),
				htmlColumnSpanAttribute (7l));

			htmlTableRowClose ();

			// status message

			if (
				isNotNull (
					inbox.getStatusMessage ())
			) {

				htmlTableRowOpen ();

				htmlTableCellWrite (
					inbox.getStatusMessage (),
					htmlColumnSpanAttribute (7l));

				htmlTableRowClose ();

			}

		}

		htmlTableClose ();

		htmlFormClose ();

	}

}
