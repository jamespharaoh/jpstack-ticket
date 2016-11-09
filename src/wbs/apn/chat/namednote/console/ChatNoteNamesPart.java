package wbs.apn.chat.namednote.console;

import static wbs.utils.etc.NumberUtils.integerToDecimalString;
import static wbs.utils.string.StringUtils.stringFormat;
import static wbs.utils.web.HtmlBlockUtils.htmlParagraphClose;
import static wbs.utils.web.HtmlBlockUtils.htmlParagraphOpen;
import static wbs.utils.web.HtmlFormUtils.htmlFormClose;
import static wbs.utils.web.HtmlFormUtils.htmlFormOpenPostAction;
import static wbs.utils.web.HtmlTableUtils.htmlTableCellClose;
import static wbs.utils.web.HtmlTableUtils.htmlTableCellOpen;
import static wbs.utils.web.HtmlTableUtils.htmlTableClose;
import static wbs.utils.web.HtmlTableUtils.htmlTableHeaderRowWrite;
import static wbs.utils.web.HtmlTableUtils.htmlTableOpenList;
import static wbs.utils.web.HtmlTableUtils.htmlTableRowClose;
import static wbs.utils.web.HtmlTableUtils.htmlTableRowOpen;

import java.util.List;

import wbs.apn.chat.core.model.ChatObjectHelper;
import wbs.apn.chat.core.model.ChatRec;
import wbs.apn.chat.namednote.model.ChatNoteNameObjectHelper;
import wbs.apn.chat.namednote.model.ChatNoteNameRec;
import wbs.console.part.AbstractPagePart;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;

@PrototypeComponent ("chatNoteNamesPart")
public
class ChatNoteNamesPart
	extends AbstractPagePart {

	// singleton dependencies

	@SingletonDependency
	ChatObjectHelper chatHelper;

	@SingletonDependency
	ChatNoteNameObjectHelper chatNoteNameHelper;

	// state

	List <ChatNoteNameRec> noteNames;

	// implementation

	@Override
	public
	void prepare () {

		ChatRec chat =
			chatHelper.findRequired (
				requestContext.stuffInteger (
					"chatId"));

		noteNames =
			chatNoteNameHelper.findNotDeleted (
				chat);

	}

	@Override
	public
	void renderHtmlBodyContent () {

		// form open

		htmlFormOpenPostAction (
			"chat.settings.noteNames");

		// form controls

		htmlParagraphOpen ();

		formatWriter.writeFormat (
			"<input",
			" type=\"submit\"",
			" name=\"saveChanges\"",
			" value=\"save changes\"",
			">");

		htmlParagraphClose ();

		// table open

		htmlTableOpenList ();

		htmlTableHeaderRowWrite (
			"Name",
			"Options");

		// table contents

		for (
			ChatNoteNameRec noteName
				: noteNames
		) {

			htmlTableRowOpen ();

			htmlTableCellOpen ();

			formatWriter.writeLineFormat (
				"<input",
				" type=\"text\"",
				" name=\"noteName%s\"",
				integerToDecimalString (
					noteName.getId ()),
				" value=\"%h\"",
				requestContext.formOrDefault (
					stringFormat (
						"noteName%s",
						integerToDecimalString (
							noteName.getId ())),
					noteName.getName ()),
				">");

			htmlTableCellClose ();

			htmlTableCellOpen ();

			formatWriter.writeLineFormat (
				"<input",
				" type=\"submit\"",
				" name=\"noteMoveUp%s\"",
				integerToDecimalString (
					noteName.getId ()),
				" value=\"&uarr;\"",
				">");

			formatWriter.writeLineFormat (
				"<input",
				" type=\"submit\"",
				" name=\"noteMoveDown%s\"",
				integerToDecimalString (
					noteName.getId ()),
				" value=\"&darr;\"",
				">");

			formatWriter.writeLineFormat (
				"<input",
				" type=\"submit\"",
				" name=\"noteDelete%s\"",
				integerToDecimalString (
					noteName.getId ()),
				" value=\"&times;\"",
				">");

			htmlTableCellClose ();

			htmlTableRowClose ();

		}

		// new note

		htmlTableRowOpen ();

		htmlTableCellOpen ();

		formatWriter.writeLineFormat (
			"<input",
			" type=\"text\"",
			" name=\"noteNameNew\"",
			" value=\"%h\"",
			requestContext.formOrEmptyString (
				"noteNameNew"),
			">");

		htmlTableCellClose ();

		htmlTableCellOpen ();

		formatWriter.writeLineFormat (
			"<input",
			" type=\"submit\"",
			" name=\"saveChanges\"",
			" value=\"add new\"",
			">");

		htmlTableCellClose ();

		htmlTableRowClose ();

		// table close

		htmlTableClose ();

		// form controls again

		htmlParagraphOpen ();

		formatWriter.writeLineFormat (
			"<input",
			" type=\"submit\"",
			" name=\"saveChanges\"",
			" value=\"save changes\"",
			">");

		htmlParagraphClose ();

		// form close

		htmlFormClose ();

	}

}
