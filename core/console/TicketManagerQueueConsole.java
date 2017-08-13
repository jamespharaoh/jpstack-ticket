package wbs.services.ticket.core.console;

import java.util.List;

import com.google.common.collect.ImmutableList;

import lombok.NonNull;

import wbs.console.context.ConsoleContext;
import wbs.console.helper.manager.ConsoleObjectManager;
import wbs.console.module.ConsoleManager;
import wbs.console.priv.UserPrivChecker;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.NamedDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.PrototypeDependency;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.component.manager.ComponentProvider;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

import wbs.platform.queue.console.QueueConsolePlugin;
import wbs.platform.queue.model.QueueItemRec;

import wbs.web.responder.WebResponder;

@PrototypeComponent ("ticketManagerQueueConsole")
public
class TicketManagerQueueConsole
	implements QueueConsolePlugin {

	// singleton dependencies

	@SingletonDependency
	ConsoleManager consoleManager;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ConsoleObjectManager objectManager;

	@SingletonDependency
	UserPrivChecker privChecker;

	// prototype dependencies

	@PrototypeDependency
	@NamedDependency ("ticketPendingFormResponder")
	ComponentProvider <WebResponder> pendingFormResponderProvider;

	// implementation

	@Override
	public
	List <String> queueTypeCodes (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"queueTypeCodes");

		) {

			return ImmutableList.of (
				"ticket_state.not_processed");

		}

	}

	@Override
	public
	WebResponder makeResponder (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull QueueItemRec queueItem) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"makeResponder");

		) {

			ConsoleContext targetContext =
				consoleManager.context (
					"ticket.pending",
					true);

			consoleManager.changeContext (
				taskLogger,
				privChecker,
				targetContext,
				"/" + queueItem.getRefObjectId ());

			return pendingFormResponderProvider.provide (
				taskLogger);

		}

	}

}