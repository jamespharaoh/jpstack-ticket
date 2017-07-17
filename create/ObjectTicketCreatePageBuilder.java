package wbs.services.ticket.create;

import static wbs.utils.etc.NullUtils.ifNull;
import static wbs.utils.etc.TypeUtils.genericCastUnchecked;
import static wbs.utils.string.StringUtils.capitalise;
import static wbs.utils.string.StringUtils.stringFormat;

import java.util.Collections;
import java.util.List;

import lombok.NonNull;

import wbs.console.context.ConsoleContextBuilderContainer;
import wbs.console.context.ResolvedConsoleContextExtensionPoint;
import wbs.console.forms.core.ConsoleFormManager;
import wbs.console.forms.core.ConsoleFormType;
import wbs.console.forms.types.FieldsProvider;
import wbs.console.helper.core.ConsoleHelper;
import wbs.console.module.ConsoleMetaManager;
import wbs.console.module.ConsoleModuleBuilderComponent;
import wbs.console.module.ConsoleModuleImplementation;
import wbs.console.part.PagePartFactory;
import wbs.console.responder.ConsoleFile;
import wbs.console.tab.ConsoleContextTab;
import wbs.console.tab.TabContextResponder;

import wbs.framework.builder.Builder;
import wbs.framework.builder.annotations.BuildMethod;
import wbs.framework.builder.annotations.BuilderParent;
import wbs.framework.builder.annotations.BuilderSource;
import wbs.framework.builder.annotations.BuilderTarget;
import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.PrototypeDependency;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.component.manager.ComponentManager;
import wbs.framework.component.manager.ComponentProvider;
import wbs.framework.database.NestedTransaction;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

import wbs.platform.object.criteria.WhereDeletedCriteriaSpec;
import wbs.platform.object.criteria.WhereICanManageCriteriaSpec;
import wbs.platform.object.criteria.WhereNotDeletedCriteriaSpec;

import wbs.services.ticket.core.model.TicketManagerRec;
import wbs.services.ticket.core.model.TicketRec;
import wbs.web.mvc.WebAction;
import wbs.web.responder.WebResponder;

@PrototypeComponent ("objectTicketCreatePageBuilder")
public
class ObjectTicketCreatePageBuilder
	implements ConsoleModuleBuilderComponent {

	// singleton dependencies

	@SingletonDependency
	ComponentManager componentManager;

	@SingletonDependency
	ConsoleMetaManager consoleMetaManager;

	@SingletonDependency
	ConsoleFormManager formContextManager;

	@ClassSingletonDependency
	LogContext logContext;

	// prototype dependencies

	@PrototypeDependency
	ComponentProvider <ConsoleFile> consoleFileProvider;

	@PrototypeDependency
	ComponentProvider <ConsoleContextTab> contextTabProvider;

	@PrototypeDependency
	ComponentProvider <ObjectTicketCreateSetFieldSpec>
		ticketCreateSetFieldSpecProvider;

	@PrototypeDependency
	ComponentProvider <ObjectTicketCreatePart <TicketRec, TicketManagerRec>>
	objectTicketCreatePartProvider;

	@PrototypeDependency
	ComponentProvider <ObjectTicketCreateAction <TicketRec, TicketManagerRec>>
	objectTicketCreateActionProvider;

	@PrototypeDependency
	ComponentProvider <TabContextResponder> tabContextResponderProvider;

	@PrototypeDependency
	ComponentProvider <WhereDeletedCriteriaSpec>
		whereDeletedCriteriaSpecProvider;

	@PrototypeDependency
	ComponentProvider <WhereICanManageCriteriaSpec>
		whereICanManageCriteriaSpecProvider;

	@PrototypeDependency
	ComponentProvider <WhereNotDeletedCriteriaSpec>
		whereNotDeletedCriteriaSpecProvider;

	// builder

	@BuilderParent
	ConsoleContextBuilderContainer <TicketRec> container;

	@BuilderSource
	ObjectTicketCreatePageSpec spec;

	@BuilderTarget
	ConsoleModuleImplementation consoleModule;

	// state

	ConsoleHelper <TicketRec> consoleHelper;

	String typeCode;
	FieldsProvider <TicketRec, TicketManagerRec> fieldsProvider;
	List <ObjectTicketCreateSetFieldSpec> ticketFields;
	String name;
	String tabName;
	String tabLabel;
	String localFile;
	Boolean hideTab;
	String targetContextTypeName;
	ConsoleFormType <TicketRec> formContextBuilder;
	String createTimeFieldName;
	String createUserFieldName;
	String createPrivDelegate;
	String createPrivCode;
	String privKey;
	String ticketManagerPath;

	ComponentProvider <WebResponder> responderProvider;
	ComponentProvider <WebResponder> targetResponderProvider;

	// build

	@BuildMethod
	@Override
	public
	void build (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull Builder <TaskLogger> builder) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"build");

		) {

			setDefaults (
				taskLogger);

			for (
				ResolvedConsoleContextExtensionPoint extensionPoint
					: consoleMetaManager.resolveExtensionPoint (
						taskLogger,
						container.extensionPointName ())
			) {

				buildTab (
					taskLogger,
					extensionPoint);

				buildFile (
					taskLogger,
					extensionPoint);

			}

			buildResponder ();

		}

	}

	void buildTab (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull ResolvedConsoleContextExtensionPoint extensionPoint) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"buildTab");

		) {

			consoleModule.addContextTab (
				taskLogger,
				container.tabLocation (),
				contextTabProvider.provide (
					taskLogger)

					.name (
						tabName)

					.defaultLabel (
						tabLabel)

					.localFile (
						localFile),

				hideTab
					? Collections.<String>emptyList ()
					: extensionPoint.contextTypeNames ());

		}

	}

	void buildFile (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull ResolvedConsoleContextExtensionPoint extensionPoint) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"buildFile");

		) {

			ComponentProvider <WebAction> createActionProvider =
				taskLoggerNested ->
					objectTicketCreateActionProvider.provide (
						taskLoggerNested)

				.consoleHelper (
					consoleHelper)

				.typeCode (
					typeCode)

				.responderProvider (
					responderProvider)

				.targetContextTypeName (
					targetContextTypeName)

				.targetResponderProvider (
					targetResponderProvider)

				.createPrivDelegate (
					createPrivDelegate)

				.createPrivCode (
					createPrivCode)

				.formContextBuilder (
					formContextBuilder)

				.ticketFieldSpecs(
						ticketFields)

				.ticketManagerPath(
					ticketManagerPath)

				.createTimeFieldName (
					createTimeFieldName)

				.createUserFieldName (
					createUserFieldName)

			;

			consoleModule.addContextFile (
				localFile,
				consoleFileProvider.provide (
					taskLogger)

					.getResponderProvider (
						responderProvider)

					.postActionProvider (
						createActionProvider)

					/*.privKeys (
						Collections.singletonList (privKey)*/,

				extensionPoint.contextTypeNames ()
			);

		}

	}

	void buildResponder () {

		PagePartFactory partFactory =
			parentTransaction -> {

			try (

				NestedTransaction transaction =
					parentTransaction.nestTransaction (
						logContext,
						"buildResponder");

			) {

				return objectTicketCreatePartProvider.provide (
					transaction)

					.consoleHelper (
						consoleHelper)

					.localFile (
						localFile)

					.formContextBuilder (
						formContextBuilder)

					.ticketManagerPath (
						ticketManagerPath)

				;

			}

		};

		responderProvider =
			taskLoggerNested ->
				tabContextResponderProvider.provide (
					taskLoggerNested)

			.tab (
				tabName)

			.title (
				capitalise (
					consoleHelper.friendlyName () + " create"))

			.pagePartFactory (
				partFactory)

		;

	}

	// defaults

	void setDefaults (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"setDefaults");

		) {

			name =
				spec.name ();

			typeCode =
				spec.typeCode ();

			tabName =
				ifNull (
					spec.tabName (),
					stringFormat (
						"%s.create",
						container.pathPrefix ()));

			/*
			responderName =
				ifNull (
					spec.responderName (),
					stringFormat (
						"%sCreateResponder",
						container.newBeanNamePrefix ()));
			*/

			targetContextTypeName =
				"Ticket";

			targetResponderProvider =
				componentManager.getComponentProviderRequired (
					taskLogger,
					"ticketSettingsResponder",
					WebResponder.class);

			createPrivDelegate =
				spec.createPrivDelegate ();

			createPrivCode =
				"ticket.create";

			tabLabel =
				"Create ticket";

			localFile =
				ifNull (
					spec.localFile (),
					stringFormat (
						"%s.%s",
						container.pathPrefix (),
						name));

			hideTab =
				spec.hideTab ();

			consoleHelper =
					container.consoleHelper ();

			tabName =
				ifNull (
					spec.tabName (),
					stringFormat (
						"%s.create",
						container.pathPrefix ()));

			/*
			responderName =
				ifNull (
					spec.responderName (),
					stringFormat (
						"%sCreateResponder",
						container.newBeanNamePrefix ()));
			*/

			ticketManagerPath =
				spec.ticketManager ();

			if (spec.fieldsProviderName () != null) {

				fieldsProvider =
					genericCastUnchecked (
						componentManager.getComponentRequired (
							taskLogger,
							spec.fieldsProviderName (),
							FieldsProvider.class));

			}

			ticketFields =
				spec.ticketFields;

		}

	}

}
