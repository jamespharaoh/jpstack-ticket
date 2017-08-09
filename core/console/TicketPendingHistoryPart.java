package wbs.services.ticket.core.console;

import static wbs.utils.collection.CollectionUtils.emptyList;
import static wbs.utils.collection.MapUtils.emptyMap;
import static wbs.web.utils.HtmlBlockUtils.htmlHeadingThreeWrite;
import static wbs.web.utils.HtmlTableUtils.htmlTableClose;
import static wbs.web.utils.HtmlTableUtils.htmlTableHeaderRowWrite;
import static wbs.web.utils.HtmlTableUtils.htmlTableOpenDetails;
import static wbs.web.utils.HtmlTableUtils.htmlTableRowClose;
import static wbs.web.utils.HtmlTableUtils.htmlTableRowOpen;

import lombok.NonNull;

import wbs.console.forms.core.ConsoleForm;
import wbs.console.forms.core.ConsoleFormType;
import wbs.console.helper.manager.ConsoleObjectManager;
import wbs.console.part.AbstractPagePart;
import wbs.console.priv.UserPrivChecker;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.NamedDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.logging.LogContext;

import wbs.utils.string.FormatWriter;

import wbs.services.ticket.core.model.TicketNoteRec;
import wbs.services.ticket.core.model.TicketRec;
import wbs.services.ticket.core.model.TicketStateRec;

@PrototypeComponent ("ticketPendingHistoryPart")
public
class TicketPendingHistoryPart
	extends AbstractPagePart {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ConsoleObjectManager objectManager;

	@SingletonDependency
	@NamedDependency ("ticketPendingHistoryTicketFormType")
	ConsoleFormType <TicketRec> ticketFormType;

	@SingletonDependency
	@NamedDependency ("ticketPendingHistoryNoteFormType")
	ConsoleFormType <TicketNoteRec> ticketNoteFormType;

	@SingletonDependency
	@NamedDependency ("ticketPendingHistoryStateFormType")
	ConsoleFormType <TicketStateRec> ticketStateFormType;

	@SingletonDependency
	TicketStateConsoleHelper ticketStateHelper;

	@SingletonDependency
	TicketConsoleHelper ticketHelper;

	@SingletonDependency
	UserPrivChecker privChecker;

	// state

	ConsoleForm <TicketRec> ticketForm;
	ConsoleForm <TicketNoteRec> ticketNoteForm;
	ConsoleForm <TicketStateRec> ticketStateForm;

	TicketRec ticket;

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

			// load data

			ticket =
				ticketHelper.findFromContextRequired (
					transaction);

			// form contexts

			ticketForm =
				ticketFormType.buildResponse (
					transaction,
					emptyMap (),
					ticket);

			ticketNoteForm =
				ticketNoteFormType.buildResponse (
					transaction,
					emptyMap (),
					emptyList ());

			ticketStateForm =
				ticketStateFormType.buildResponse (
					transaction,
					emptyMap (),
					emptyList ());

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

			goSummary (
				transaction,
				formatWriter);

			goStateSummary (
				transaction,
				formatWriter);

			goTicketNotes (
				transaction,
				formatWriter);

		}

	}

	void goSummary (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"goSummary");

		) {

			htmlHeadingThreeWrite (
				formatWriter,
				"Summary");

			htmlTableOpenDetails (
				formatWriter);

			ticketForm.outputTableRows (
				transaction,
				formatWriter);

			htmlTableClose (
				formatWriter);

		}

	}

	private
	void goTicketNotes (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"goTicketNotes");

		) {

			htmlHeadingThreeWrite (
				formatWriter,
				"Notes");

			htmlTableOpenDetails (
				formatWriter);

			htmlTableHeaderRowWrite (
				formatWriter,
				"Index",
				"Text");

			for (
				TicketNoteRec ticketNote
					: ticket.getTicketNotes ()
			) {

				htmlTableRowOpen (
					formatWriter);

				ticketNoteForm.outputTableCellsList (
					transaction,
					formatWriter,
					ticketNote,
					true);

				htmlTableRowClose (
					formatWriter);

			}

			htmlTableClose (
				formatWriter);

		}

	}

	void goStateSummary (
			@NonNull Transaction parentTransaction,
			@NonNull FormatWriter formatWriter) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"goStateSummary");

		) {

			htmlHeadingThreeWrite (
				formatWriter,
				"State");

			htmlTableOpenDetails (
				formatWriter);

			ticketStateForm.outputTableRows (
				transaction,
				formatWriter,
				ticket.getTicketState ());

			htmlTableClose (
				formatWriter);

		}

	}

}
