package wbs.services.ticket.core.console;

import static wbs.utils.etc.NumberUtils.integerToDecimalString;
import static wbs.web.utils.HtmlAttributeUtils.htmlAttribute;
import static wbs.web.utils.HtmlAttributeUtils.htmlClassAttribute;
import static wbs.web.utils.HtmlAttributeUtils.htmlDataAttribute;
import static wbs.web.utils.HtmlAttributeUtils.htmlIdAttribute;
import static wbs.web.utils.HtmlBlockUtils.htmlHeadingThreeWrite;
import static wbs.web.utils.HtmlBlockUtils.htmlHeadingTwoWrite;
import static wbs.web.utils.HtmlBlockUtils.htmlParagraphClose;
import static wbs.web.utils.HtmlBlockUtils.htmlParagraphOpen;
import static wbs.web.utils.HtmlFormUtils.htmlFormClose;
import static wbs.web.utils.HtmlFormUtils.htmlFormOpenPostAction;
import static wbs.web.utils.HtmlScriptUtils.htmlScriptBlockClose;
import static wbs.web.utils.HtmlScriptUtils.htmlScriptBlockOpen;
import static wbs.web.utils.HtmlStyleUtils.htmlStyleBlockClose;
import static wbs.web.utils.HtmlStyleUtils.htmlStyleBlockOpen;
import static wbs.web.utils.HtmlStyleUtils.htmlStyleRuleEntry;
import static wbs.web.utils.HtmlStyleUtils.htmlStyleRuleWrite;
import static wbs.web.utils.HtmlTableUtils.htmlTableCellClose;
import static wbs.web.utils.HtmlTableUtils.htmlTableCellOpen;
import static wbs.web.utils.HtmlTableUtils.htmlTableCellWriteHtml;
import static wbs.web.utils.HtmlTableUtils.htmlTableClose;
import static wbs.web.utils.HtmlTableUtils.htmlTableHeaderRowWrite;
import static wbs.web.utils.HtmlTableUtils.htmlTableOpen;
import static wbs.web.utils.HtmlTableUtils.htmlTableRowClose;
import static wbs.web.utils.HtmlTableUtils.htmlTableRowOpen;
import static wbs.web.utils.HtmlUtils.htmlEncodeNonBreakingWhitespace;
import static wbs.web.utils.HtmlUtils.htmlLinkWrite;

import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import lombok.NonNull;

import wbs.console.context.ConsoleContextScriptRef;
import wbs.console.html.ScriptRef;
import wbs.console.priv.UserPrivChecker;
import wbs.console.request.ConsoleRequestContext;
import wbs.console.responder.ConsoleHtmlResponder;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.logging.LogContext;

import wbs.platform.currency.logic.CurrencyLogic;

import wbs.utils.string.FormatWriter;

import wbs.services.ticket.core.model.TicketRec;
import wbs.services.ticket.core.model.TicketStateRec;
import wbs.services.ticket.core.model.TicketTemplateRec;

@PrototypeComponent ("ticketPendingFormResponder")
public
class TicketPendingFormResponder
	extends ConsoleHtmlResponder {

	// singleton dependencies

	@SingletonDependency
	CurrencyLogic currencyLogic;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	UserPrivChecker privChecker;

	@SingletonDependency
	ConsoleRequestContext requestContext;

	@SingletonDependency
	TicketConsoleHelper ticketHelper;

	// state

	TicketRec ticket;
	TicketStateRec ticketState;

	List <TicketTemplateRec> templates;

	String summaryUrl;

	boolean manager;

	// details

	@Override
	public
	Set <ScriptRef> scriptRefs () {

		return ImmutableSet.<ScriptRef> of (

			ConsoleContextScriptRef.javascript (
				"/js/jquery-1.11.2.js"),

			ConsoleContextScriptRef.javascript (
				"/js/TicketPending.js"));

	}

	// implementation

	@Override
	protected
	void prepare (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"prepare");

		) {

			ticket =
				ticketHelper.findFromContextRequired (
					transaction);

			ticketState =
				ticket.getTicketState ();

			summaryUrl =
				requestContext.resolveApplicationUrlFormat (
					"/ticket.pending",
					"/%u",
					integerToDecimalString (
						ticket.getId ()),
					"/ticket.pending.history");

			ImmutableList.Builder <TicketTemplateRec> templatesBuilder =
				ImmutableList.<TicketTemplateRec> builder ();

			for (
				TicketTemplateRec template
					: ticket.getTicketManager ().getTicketTemplates ()
			) {

				if (template.getDeleted ())
					continue;

				templatesBuilder.add (
					template);

			}

			templates =
				templatesBuilder.build ();

		}

	}

	@Override
	public
	void renderHtmlHeadContents (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"renderHtmlHeadContents");

		) {

			super.renderHtmlHeadContents (
				transaction,
				formatWriter);

			// script block

			htmlScriptBlockOpen (
				formatWriter);

			formatWriter.writeLineFormat (
				"top.show_inbox (true);");

			formatWriter.writeLineFormat (
				"top.frames ['main'].location = 'about:blank';");

			formatWriter.writeLineFormatIncreaseIndent (
				"window.setTimeout (function () {");

			formatWriter.writeLineFormat (
				"top.frames ['main'].location = '%j';",
				summaryUrl);

			formatWriter.writeLineFormatDecreaseIndent (
				"}, 1);");

			htmlScriptBlockClose (
				formatWriter);

			// style block

			htmlStyleBlockOpen (
				formatWriter);

			htmlStyleRuleWrite (
				formatWriter,
				".template-chars.error",
				htmlStyleRuleEntry (
					"background-color",
					"darkred"),
				htmlStyleRuleEntry (
					"color",
					"white"),
				htmlStyleRuleEntry (
					"padding-left",
					"10px"),
				htmlStyleRuleEntry (
					"padding-right",
					"10px"));

			htmlStyleBlockClose (
				formatWriter);

		}

	}

	@Override
	public
	void renderHtmlBodyContents (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"renderHtmlBodyContents");

		) {

			requestContext.flushNotices (
				formatWriter);

			// links

			htmlParagraphOpen (
				formatWriter,
				htmlClassAttribute (
					"links"));

			htmlLinkWrite (
				formatWriter,
				requestContext.resolveApplicationUrl (
					"/queues/queue.home"),
				"Queues");

			htmlLinkWrite (
				formatWriter,
				summaryUrl,
				"Summary",
				htmlAttribute (
					"target",
					"main"));

			htmlLinkWrite (
				formatWriter,
				"javascript:top.show_inbox (false);",
				"Close");

			htmlParagraphClose (
				formatWriter);

			// header

			htmlHeadingTwoWrite (
				formatWriter,
				"Ticket management");

			// form open

			htmlFormOpenPostAction (
				formatWriter,
				requestContext.resolveApplicationUrlFormat (
					"/ticket.pending",
					"/%u",
					integerToDecimalString (
						ticket.getId ()),
					"/ticket.pending.form"));

			// table open

			htmlTableOpen (
				formatWriter,
				htmlIdAttribute (
					"templates"),
				htmlClassAttribute (
					"list"),
				htmlStyleRuleEntry (
					"width",
					"100%"));

			// table header

			htmlTableHeaderRowWrite (
				formatWriter,
				"",
				"Name",
				"New State",
				"Time to queue");

			List <TicketTemplateRec> templatesReversed =
				Lists.reverse (
					templates);

			for (
				TicketTemplateRec template
					: templatesReversed
			) {

				doTemplate (
					transaction,
					formatWriter,
					template);
			}

			htmlTableClose (
				formatWriter);

			addTicketNote (
				transaction,
				formatWriter);

			formatWriter.writeLineFormat (
				"<input",
				" class=\"template-submit\"",
				" type=\"submit\"",
				" name=\"send\"",
				" value=\"Send\"",
				">");

			htmlFormClose (
				formatWriter);

		}

	}

	void addTicketNote (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"addTicketNote");

		) {

			htmlHeadingThreeWrite (
				formatWriter,
				"Add new note");

			formatWriter.writeLineFormat (
				"<label for=\"note-text\">",
				"Note text",
				"</label>");

			formatWriter.writeLineFormat (
				"<input",
				" id=\"note-text\"",
				" type=\"textarea\"",
				" name=\"note-text\"",
				">");

		}

	}

	void doTemplate (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter,
			@NonNull TicketTemplateRec template) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"doTemplate");

		) {

			htmlTableRowOpen (
				formatWriter,
				htmlClassAttribute (
					"template"),
				htmlDataAttribute (
					"template",
					integerToDecimalString (
						template.getId ())));

			/*
			if (template.getTicketState()
					.getState().equals (
						ticket.getTicketState().getState())) {

				printFormat (

					"<td><input",

					" id=\"radio-template-%h\"",
					template.getId (),

					" class=\"template-radio\"",

					" type=\"radio\"",

					" name=\"template\"",

					" value=\"%h\"",
					template.getId (),

					"checked",

					"></td>\n");

			}
			else {

				printFormat (

					"<td><input",

					" id=\"radio-template-%h\"",
					template.getId (),

					" class=\"template-radio\"",

					" type=\"radio\"",

					" name=\"template\"",

					" value=\"%h\"",
					template.getId (),

					"></td>\n");

			}
			*/

			htmlTableCellWriteHtml (
				formatWriter,
				htmlEncodeNonBreakingWhitespace (
					template.getName ()));

			/*
			printFormat (
				"<td>%h</td>\n",
				template.getTicketState ()
					.getState ()
						.toString ());
			*/

			htmlTableCellOpen (
				formatWriter);

			formatWriter.writeLineFormat (
				"<input",

				" id=\"timestamp-%h\"",
				integerToDecimalString (
					template.getId ()),

				" type=\"textarea\"",

				/*
				" name=\"timestamp-%h\"",
				template.getTicketState ().getState ().toString (),
				*/

				" value=\"%h\"",
				integerToDecimalString (
					template.getTicketState ().getMinimum ()),

				">");

			htmlTableCellClose (
				formatWriter);

			htmlTableRowClose (
				formatWriter);

		}

	}

}

