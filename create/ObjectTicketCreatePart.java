package wbs.services.ticket.create;

import static wbs.utils.etc.TypeUtils.genericCastUnchecked;
import static wbs.web.utils.HtmlBlockUtils.htmlParagraphClose;
import static wbs.web.utils.HtmlBlockUtils.htmlParagraphOpen;
import static wbs.web.utils.HtmlBlockUtils.htmlParagraphWriteFormat;
import static wbs.web.utils.HtmlFormUtils.htmlFormClose;
import static wbs.web.utils.HtmlFormUtils.htmlFormOpenPostAction;
import static wbs.web.utils.HtmlTableUtils.htmlTableClose;
import static wbs.web.utils.HtmlTableUtils.htmlTableOpenDetails;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import wbs.console.forms.core.ConsoleForm;
import wbs.console.forms.core.ConsoleFormType;
import wbs.console.helper.core.ConsoleHelper;
import wbs.console.helper.manager.ConsoleObjectManager;
import wbs.console.module.ConsoleManager;
import wbs.console.part.AbstractPagePart;
import wbs.console.request.ConsoleRequestContext;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.entity.record.Record;
import wbs.framework.logging.LogContext;

import wbs.utils.string.FormatWriter;

import wbs.services.ticket.core.console.TicketConsoleHelper;
import wbs.services.ticket.core.model.TicketFieldTypeObjectHelper;
import wbs.services.ticket.core.model.TicketFieldValueObjectHelper;
import wbs.services.ticket.core.model.TicketManagerRec;
import wbs.services.ticket.core.model.TicketRec;

@Accessors (fluent = true)
@PrototypeComponent ("objectTicketCreatePart")
public
class ObjectTicketCreatePart <
	ObjectType extends Record <ObjectType>,
	ParentType extends Record <ParentType>
>
	extends AbstractPagePart {

	// singleton dependencies

	@SingletonDependency
	ConsoleManager consoleManager;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ConsoleObjectManager objectManager;

	@SingletonDependency
	ConsoleRequestContext requestContext;

	@SingletonDependency
	TicketFieldTypeObjectHelper ticketFieldTypeHelper;

	@SingletonDependency
	TicketFieldValueObjectHelper ticketFieldValueHelper;

	@SingletonDependency
	TicketConsoleHelper ticketHelper;

	// properties

	@Getter @Setter
	ConsoleFormType <TicketRec> formContextBuilder;

	@Getter @Setter
	ConsoleHelper<?> consoleHelper;

	@Getter @Setter
	String localFile;

	@Getter @Setter
	String ticketManagerPath;

	// state

	ObjectTicketCreateSetFieldSpec currentTicketFieldSpec;
	TicketRec ticket;
	TicketManagerRec ticketManager;

	ConsoleForm <TicketRec> formContext;

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

			// find context object

			Record <?> contextObject =
				consoleHelper.findFromContextRequired (
					transaction);

			ticketManager =
				genericCastUnchecked (
					objectManager.dereference (
						transaction,
						contextObject,
						ticketManagerPath));

			/*
			prepareFieldSet (
				transaction);
			*/

			// create dummy instance

			ticket =
				ticketHelper.createInstance ()

				.setTicketManager (
					ticketManager);

/*
			for (
				ObjectTicketCreateSetFieldSpec ticketFieldSpec
					: ticketFieldSpecs
			) {

				TicketFieldTypeRec ticketFieldType =
					ticketFieldTypeHelper.findByCodeRequired (
						transaction,
						ticketManager,
						ticketFieldSpec.fieldTypeCode ());

				TicketFieldValueRec ticketFieldValue =
					ticketFieldValueHelper.createInstance ()

					.setTicket (
						ticket)

					.setTicketFieldType (
						ticketFieldType);

				switch (ticketFieldType.getDataType ()) {

				case string:

					ticketFieldValue.setStringValue (
						(String)
						objectManager.dereferenceObsolete (
							transaction,
							contextObject,
							ticketFieldSpec.valuePath ()));

					break;

				case number:

					ticketFieldValue.setIntegerValue (
						(Long)
						objectManager.dereferenceObsolete (
							transaction,
							contextObject,
							ticketFieldSpec.valuePath ()));

					break;

				case bool:

					ticketFieldValue.setBooleanValue (
						(Boolean)
						objectManager.dereferenceObsolete (
							transaction,
							contextObject,
							ticketFieldSpec.valuePath ()));

					break;

				case object:

					Record<?> objectValue =
						(Record<?>)
						objectManager.dereferenceObsolete (
							transaction,
							contextObject,
							ticketFieldSpec.valuePath ());

					// TODO check type

					Long objectId =
						objectValue.getId ();

					ticketFieldValue

						.setIntegerValue (
							objectId);

					break;

				default:

					throw new RuntimeException ();

				}

				ticket.setNumFields (
					ticket.getNumFields () + 1);

				ticket.getTicketFieldValues ().put (
					ticketFieldType.getId (),
					ticketFieldValue);

			}
			*/

		}

	}

	/*
	void prepareFieldSet (
			@NonNull TaskLogger parentTaskLogger) {

		formFieldSet =
			fieldsProvider.getFieldsForParent (
				parentTaskLogger,
				ticketManager);

	}
	*/

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

			htmlParagraphWriteFormat (
				formatWriter,
				"Please enter the details for the new ticket");

			htmlFormOpenPostAction (
				formatWriter,
				requestContext.resolveLocalUrl (
					"/" + localFile));

			htmlTableOpenDetails (
				formatWriter);

			formContext.outputFormRows (
				transaction,
				formatWriter);

			htmlTableClose (
				formatWriter);

			htmlParagraphOpen (
				formatWriter);

			formatWriter.writeFormat (
				"<input",
				" type=\"submit\"",
				" value=\"create ticket\"",
				">");

			htmlParagraphClose (
				formatWriter);

			htmlFormClose (
				formatWriter);

		}

	}

}
