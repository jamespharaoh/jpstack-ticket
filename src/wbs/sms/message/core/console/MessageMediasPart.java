package wbs.sms.message.core.console;

import static wbs.utils.etc.LogicUtils.ifNullThenEmDash;
import static wbs.utils.etc.Misc.prettySize;
import static wbs.utils.etc.NumberUtils.integerToDecimalString;
import static wbs.web.utils.HtmlAttributeUtils.htmlClassAttribute;
import static wbs.web.utils.HtmlAttributeUtils.htmlColumnSpanAttribute;
import static wbs.web.utils.HtmlAttributeUtils.htmlDataAttribute;
import static wbs.web.utils.HtmlTableUtils.htmlTableCellClose;
import static wbs.web.utils.HtmlTableUtils.htmlTableCellOpen;
import static wbs.web.utils.HtmlTableUtils.htmlTableCellWrite;
import static wbs.web.utils.HtmlTableUtils.htmlTableClose;
import static wbs.web.utils.HtmlTableUtils.htmlTableHeaderRowWrite;
import static wbs.web.utils.HtmlTableUtils.htmlTableOpenList;
import static wbs.web.utils.HtmlTableUtils.htmlTableRowClose;
import static wbs.web.utils.HtmlTableUtils.htmlTableRowOpen;

import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import lombok.NonNull;

import wbs.console.html.MagicTableScriptRef;
import wbs.console.html.ScriptRef;
import wbs.console.misc.JqueryScriptRef;
import wbs.console.part.AbstractPagePart;
import wbs.console.request.ConsoleRequestContext;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.logging.LogContext;

import wbs.platform.media.console.MediaConsoleLogic;
import wbs.platform.media.model.MediaRec;

import wbs.sms.message.core.model.MessageRec;

import wbs.utils.string.FormatWriter;

@PrototypeComponent ("messageMediasPart")
public
class MessageMediasPart
	extends AbstractPagePart {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	MediaConsoleLogic mediaConsoleLogic;

	@SingletonDependency
	MessageConsoleHelper messageHelper;

	@SingletonDependency
	ConsoleRequestContext requestContext;

	// state

	MessageRec message;
	List <MediaRec> medias;

	// details

	@Override
	public
	Set <ScriptRef> scriptRefs () {

		return ImmutableSet.<ScriptRef>builder ()

			.addAll (
				super.scriptRefs ())

			.add (
				JqueryScriptRef.instance)

			.add (
				MagicTableScriptRef.instance)

			.build ();

	}

	// implementation

	@Override
	public
	void prepare (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"prepare");

		) {

			message =
				messageHelper.findFromContextRequired (
					transaction);

			medias =
				message.getMedias ();

		}

	}

	@Override
	public
	void renderHtmlBodyContent (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"renderHtmlBodyContent");

		) {

			htmlTableOpenList (
				formatWriter);

			htmlTableHeaderRowWrite (
				formatWriter,
				"Thumbnail",
				"Type",
				"Filename",
				"Size");

			if (medias.size () == 0) {

				htmlTableRowOpen (
					formatWriter);

				htmlTableCellWrite (
					formatWriter,
					"(no media)",
					htmlColumnSpanAttribute (4l));

				htmlTableRowClose (
					formatWriter);

			} else {

				for (
					int index = 0;
					index < medias.size ();
					index ++
				) {

					MediaRec media =
						medias.get (index);

					htmlTableRowOpen (
						formatWriter,
						htmlClassAttribute (
							"magic-table-row"),
						htmlDataAttribute (
							"target-href",
							requestContext.resolveLocalUrlFormat (
								"/message.mediaSummary",
								"?index=%u",
								integerToDecimalString (
									index))));

					htmlTableCellOpen (
						formatWriter);

					mediaConsoleLogic.writeMediaThumb100 (
						transaction,
						formatWriter,
						media);

					htmlTableCellClose (
						formatWriter);

					htmlTableCellWrite (
						formatWriter,
						media.getMediaType ().getMimeType ());

					htmlTableCellWrite (
						formatWriter,
						ifNullThenEmDash (
							media.getFilename ()));

					htmlTableCellWrite (
						formatWriter,
						prettySize (
							media.getContent().getData ().length));

					htmlTableRowClose (
						formatWriter);

				}

			}

			htmlTableClose (
				formatWriter);

		}

	}

}
