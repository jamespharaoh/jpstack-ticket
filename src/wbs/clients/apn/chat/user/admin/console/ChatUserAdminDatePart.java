package wbs.clients.apn.chat.user.admin.console;

import static wbs.framework.utils.etc.NullUtils.ifNull;
import static wbs.framework.utils.etc.StringUtils.objectToStringNullSafe;

import javax.inject.Inject;
import javax.inject.Named;

import wbs.clients.apn.chat.user.core.console.ChatUserConsoleHelper;
import wbs.clients.apn.chat.user.core.logic.ChatUserLogic;
import wbs.clients.apn.chat.user.core.model.ChatUserDateLogRec;
import wbs.clients.apn.chat.user.core.model.ChatUserRec;
import wbs.console.helper.ConsoleObjectManager;
import wbs.console.helper.EnumConsoleHelper;
import wbs.console.part.AbstractPagePart;
import wbs.framework.application.annotations.PrototypeComponent;
import wbs.framework.utils.TimeFormatter;

@PrototypeComponent ("chatUserAdminDatePart")
public
class ChatUserAdminDatePart
	extends AbstractPagePart {

	// dependencies

	@Inject
	ChatUserConsoleHelper chatUserHelper;

	@Inject @Named
	EnumConsoleHelper <?> chatUserDateModeConsoleHelper;

	@Inject
	ChatUserLogic chatUserLogic;

	@Inject
	ConsoleObjectManager objectManager;

	@Inject
	TimeFormatter timeFormatter;

	// state

	ChatUserRec chatUser;

	// implementation

	@Override
	public
	void prepare () {

		chatUser =
			chatUserHelper.findRequired (
				requestContext.stuffInteger (
					"chatUserId"));

	}

	@Override
	public
	void renderHtmlBodyContent () {

		if (chatUser.getBarred ()) {

			printFormat (
				"<p>This user is barred</p>");

		} else {

			printFormat (
				"<form",
				" method=\"post\"",
				" action=\"%h\"",
				requestContext.resolveLocalUrl (
					"/chatUser.admin.date"),
				">\n");

			printFormat (
				"<table class=\"details\">");

			printFormat (
				"<tr>\n",

				"<th>Date mode</th>\n",

				"<td>%s</td>\n",
				chatUserDateModeConsoleHelper.select (
					"dateMode",
					ifNull (
						requestContext.getForm ("dateMode"),
						objectToStringNullSafe (
							chatUser.getDateMode ()))),

				"</tr>\n");

			printFormat (
				"<tr>\n",

				"<th>Radius (miles)</th>\n",

				"<td><input",
				" type=\"text\"",
				" name=\"radius\"",
				" value=\"%h\"",
				ifNull (
					requestContext.getForm ("radius"),
					chatUser.getDateRadius ().toString ()),
				"></td>\n",

				"</tr>\n");

			printFormat (
				"<tr>\n",
				"<th>Start hour</th>\n",

				"<td><input",
				" type=\"text\"",
				" name=\"startHour\"",
				" value=\"%h\"",
				ifNull (
					requestContext.getForm ("startHour"),
					chatUser.getDateStartHour ().toString ()),
				"></td>\n",

				"</tr>");

			printFormat (
				"<tr>\n",
				"<th>End hour</th>\n",

				"<td><input",
				" type=\"text\"",
				" name=\"endHour\"",
				" value=\"%h\"",
				ifNull (
					requestContext.getForm ("endHour"),
					chatUser.getDateEndHour ().toString ()),
				"\"></td>\n",

				"</tr>");

			printFormat (
				"<tr>\n",
				"<th>Max profiles per day</th>\n",

				"<td><input",
				" type=\"text\"",
				" name=\"dailyMax\"",
				" value=\"%h\"",
				ifNull (
					requestContext.getForm ("dailyMax"),
					chatUser.getDateDailyMax ().toString ()),
				"></td>\n",

				"</tr>\n");

			printFormat (
				"</table>\n");

			printFormat (
				"<p><input",
				" type=\"submit\"",
				" value=\"save changes\"",
				"></p>\n");

			printFormat (
				"</form>\n");

		}

		printFormat (
			"<table class=\"list\">\n",
			"<tr>\n",
			"<th>Timestamp</th>\n",
			"<th>Source</th>\n",
			"<th>Mode</th>\n",
			"<th>Radius</th>\n",
			"<th>Hours</th>\n",
			"<th>Number</th>\n",
			"</tr>\n");

		for (
			ChatUserDateLogRec chatUserDateLogRec
				: chatUser.getChatUserDateLogs ()
		) {

			printFormat (
				"<tr>\n");

			printFormat (
				"<td>%h</td>\n",
				chatUserDateLogRec.getTimestamp () != null
					? timeFormatter.timestampTimezoneString (
						chatUserLogic.getTimezone (
							chatUser),
						chatUserDateLogRec.getTimestamp ())
					: "-");

			if (chatUserDateLogRec.getUser () != null) {

				printFormat (
					"%s\n",
					objectManager.tdForObjectMiniLink (
						chatUserDateLogRec.getUser ()));

			} else if (chatUserDateLogRec.getMessage() != null) {

				printFormat (
					"%s\n",
					objectManager.tdForObjectMiniLink (
						chatUserDateLogRec.getMessage ()));

			} else {

				printFormat (
					"<td>API</td>\n");

			}

			printFormat (

				"<td>%h</td>\n",
				chatUserDateLogRec.getDateMode (),

				"<td>%h</td>\n",
				chatUserDateLogRec.getRadius (),

				"<td>%h-%h</td>\n",
				chatUserDateLogRec.getStartHour (),
				chatUserDateLogRec.getEndHour (),

				"<td>%h</td>\n",
				chatUserDateLogRec.getDailyMax ());

		}

		printFormat (
			"</table>\n");

	}

}
