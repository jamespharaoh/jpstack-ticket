package wbs.services.ticket.create;

import static wbs.utils.etc.Misc.doNothing;

import lombok.NonNull;

import wbs.console.module.ConsoleModuleBuilderComponent;
import wbs.console.module.ConsoleModuleImplementation;

import wbs.framework.builder.Builder;
import wbs.framework.builder.annotations.BuildMethod;
import wbs.framework.builder.annotations.BuilderParent;
import wbs.framework.builder.annotations.BuilderSource;
import wbs.framework.builder.annotations.BuilderTarget;
import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.TaskLogger;

@PrototypeComponent ("objectTicketCreateSetFieldBuilder")
public
class ObjectTicketCreateSetFieldBuilder
	implements ConsoleModuleBuilderComponent {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	// builder

	@BuilderParent
	ObjectTicketCreatePageBuilder objectTicketCreatePageBuilder;

	@BuilderSource
	ObjectTicketCreatePageSpec objectTicketCreatePageSpec;

	@BuilderTarget
	ConsoleModuleImplementation consoleModule;

	// build

	@Override
	@BuildMethod
	public
	void build (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull Builder <TaskLogger> builder) {

		doNothing ();

	}

}
