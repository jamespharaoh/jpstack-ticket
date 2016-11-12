package wbs.platform.object.list;

import static wbs.utils.etc.NullUtils.ifNull;
import static wbs.utils.etc.TypeUtils.classEqualSafe;
import static wbs.utils.etc.TypeUtils.genericCastUnchecked;
import static wbs.utils.string.StringUtils.capitalise;
import static wbs.utils.string.StringUtils.stringFormat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Provider;

import com.google.common.collect.ImmutableMap;

import lombok.NonNull;

import wbs.console.annotations.ConsoleModuleBuilderHandler;
import wbs.console.context.ConsoleContextBuilderContainer;
import wbs.console.context.ResolvedConsoleContextExtensionPoint;
import wbs.console.forms.CodeFormFieldSpec;
import wbs.console.forms.DescriptionFormFieldSpec;
import wbs.console.forms.FieldsProvider;
import wbs.console.forms.FormFieldSet;
import wbs.console.forms.NameFormFieldSpec;
import wbs.console.forms.StaticFieldsProvider;
import wbs.console.helper.core.ConsoleHelper;
import wbs.console.module.ConsoleMetaManager;
import wbs.console.module.ConsoleModuleBuilder;
import wbs.console.module.ConsoleModuleImplementation;
import wbs.console.part.PagePart;
import wbs.console.part.PagePartFactory;
import wbs.console.responder.ConsoleFile;
import wbs.console.tab.ConsoleContextTab;
import wbs.console.tab.TabContextResponder;
import wbs.framework.builder.Builder;
import wbs.framework.builder.BuilderComponent;
import wbs.framework.builder.annotations.BuildMethod;
import wbs.framework.builder.annotations.BuilderParent;
import wbs.framework.builder.annotations.BuilderSource;
import wbs.framework.builder.annotations.BuilderTarget;
import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.PrototypeDependency;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.component.manager.ComponentManager;
import wbs.framework.entity.record.Record;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.TaskLogger;
import wbs.platform.object.criteria.WhereDeletedCriteriaSpec;
import wbs.platform.object.criteria.WhereICanManageCriteriaSpec;
import wbs.platform.object.criteria.WhereNotDeletedCriteriaSpec;
import wbs.platform.scaffold.model.SliceRec;

@PrototypeComponent ("objectListPageBuilder")
@ConsoleModuleBuilderHandler
public
class ObjectListPageBuilder <
	ObjectType extends Record <ObjectType>,
	ParentType extends Record <ParentType>
>
	implements BuilderComponent {

	// singleton dependencies

	@SingletonDependency
	ComponentManager componentManager;

	@SingletonDependency
	ConsoleModuleBuilder consoleModuleBuilder;

	@SingletonDependency
	ConsoleMetaManager consoleMetaManager;

	@ClassSingletonDependency
	LogContext logContext;

	// prototype dependencies

	@PrototypeDependency
	Provider <ConsoleFile> consoleFile;

	@PrototypeDependency
	Provider <ConsoleContextTab> contextTab;

	@PrototypeDependency
	Provider <ObjectListTabSpec> listTabSpec;

	@PrototypeDependency
	Provider <ObjectListPart <ObjectType, ParentType>> objectListPart;

	@PrototypeDependency
	Provider <TabContextResponder> tabContextResponder;

	@PrototypeDependency
	Provider <WhereDeletedCriteriaSpec> whereDeletedCriteriaSpec;

	@PrototypeDependency
	Provider <WhereICanManageCriteriaSpec> whereICanManageCriteriaSpec;

	@PrototypeDependency
	Provider <WhereNotDeletedCriteriaSpec> whereNotDeletedCriteriaSpec;

	// builder

	@BuilderParent
	ConsoleContextBuilderContainer <ObjectType> container;

	@BuilderSource
	ObjectListPageSpec spec;

	@BuilderTarget
	ConsoleModuleImplementation consoleModule;

	// state

	ConsoleHelper <ObjectType> consoleHelper;

	String typeCode;

	FieldsProvider <ObjectType, ParentType> fieldsProvider;

	FormFieldSet <ObjectType> formFieldSet;

	Map <String, ObjectListBrowserSpec> listBrowsersByFieldName;

	Map <String, ObjectListTabSpec> listTabsByName;

	// build

	@BuildMethod
	@Override
	public
	void build (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull Builder builder) {

		TaskLogger taskLogger =
			logContext.nestTaskLogger (
				parentTaskLogger,
				"build");

		setDefaults (
			taskLogger);

		for (
			ResolvedConsoleContextExtensionPoint resolvedExtensionPoint
				: consoleMetaManager.resolveExtensionPoint (
					container.extensionPointName ())
		) {

			buildContextTab (
				resolvedExtensionPoint);

			buildContextFile (
				resolvedExtensionPoint);

		}

		buildResponder ();

	}

	void buildContextTab (
			@NonNull ResolvedConsoleContextExtensionPoint extensionPoint) {

		consoleModule.addContextTab (
			container.taskLogger (),
			container.tabLocation (),

			contextTab.get ()

				.name (
					container.pathPrefix () + ".list")

				.defaultLabel (
					"List")

				.localFile (
					container.pathPrefix () + ".list"),

			extensionPoint.contextTypeNames ());

	}

	void buildContextFile (
			@NonNull ResolvedConsoleContextExtensionPoint extensionPoint) {

		consoleModule.addContextFile (

			container.pathPrefix () + ".list",

			consoleFile.get ()

				.getResponderName (
					stringFormat (
						"%sListResponder",
						container.newBeanNamePrefix ())),

			extensionPoint.contextTypeNames ());

	}

	void buildResponder () {

		PagePartFactory partFactory =
			new PagePartFactory () {

			@Override
			public
			PagePart buildPagePart (
					@NonNull TaskLogger parentTaskLogger) {

				return objectListPart.get ()

					.consoleHelper (
						consoleHelper)

					.typeCode (
						typeCode)

					.localName (
						container.pathPrefix () + ".list")

					.listTabSpecs (
						listTabsByName)

					.formFieldsProvider (
						fieldsProvider)

					.listBrowserSpecs (
						listBrowsersByFieldName)

					.targetContextTypeName (
						ifNull (
							spec.targetContextTypeName (),
							consoleHelper.objectName () + ":combo"));
			}

		};

		consoleModule.addResponder (
			container.newBeanNamePrefix () + "ListResponder",
			tabContextResponder.get ()

				.tab (
					container.pathPrefix () + ".list")

				.title (
					capitalise (consoleHelper.friendlyName () + " list"))

				.pagePartFactory (
					partFactory));

	}

	// defaults

	void setDefaults (
			@NonNull TaskLogger taskLogger) {

		consoleHelper =
			container.consoleHelper ();

		typeCode =
			spec.typeCode ();

		// if a provider name is provided

		if (spec.fieldsProviderName () != null) {

			fieldsProvider =
				genericCastUnchecked (
					componentManager.getComponentRequired (
						taskLogger,
						spec.fieldsProviderName (),
						FieldsProvider.class));

		// if a field name is provided

		} else if (spec.fieldsName () != null) {

			fieldsProvider =
				new StaticFieldsProvider <ObjectType, ParentType> ()

				.fields (
					consoleModule.formFieldSet (
						spec.fieldsName (),
						consoleHelper.objectClass ()));

		// if nothing is provided

		} else {

			fieldsProvider =
				new StaticFieldsProvider<ObjectType,ParentType> ()

				.fields (
					defaultFields (
						taskLogger));

		}


		listBrowsersByFieldName =
			ifNull (
				spec.listBrowsersByFieldName (),
				Collections.emptyMap ());

		listTabsByName =
			spec.listTabsByName () != null
			&& ! spec.listTabsByName ().isEmpty ()
				? spec.listTabsByName ()
				: defaultListTabSpecs ();

	}

	FormFieldSet <ObjectType> defaultFields (
			@NonNull TaskLogger parentTaskLogger) {

		TaskLogger taskLogger =
			logContext.nestTaskLogger (
				parentTaskLogger,
				"defaultFields");

		// create spec

		List <Object> formFieldSpecs =
			new ArrayList<> ();

		if (

			consoleHelper.parentTypeIsFixed ()

			&& classEqualSafe (
				consoleHelper.parentClass (),
				SliceRec.class)

		) {

			formFieldSpecs.add (
				new DescriptionFormFieldSpec ()

				.delegate (
					"slice")

				.label (
					"Slice")

			);

		}

		if (consoleHelper.nameIsCode ()) {

			formFieldSpecs.add (
				new CodeFormFieldSpec ());

		} else if (consoleHelper.nameExists ()) {

			formFieldSpecs.add (
				new NameFormFieldSpec ());

		}

		if (consoleHelper.descriptionExists ()) {

			formFieldSpecs.add (
				new DescriptionFormFieldSpec ());

		}

		// build

		String fieldSetName =
			stringFormat (
				"%s.list",
				consoleHelper.objectName ());

		return consoleModuleBuilder.buildFormFieldSet (
			taskLogger,
			consoleHelper,
			fieldSetName,
			formFieldSpecs);

	}

	Map<String,ObjectListTabSpec> defaultListTabSpecs () {

		if (consoleHelper.deletedExists ()) {

			return ImmutableMap.<String,ObjectListTabSpec>builder ()

				.put (
					"all",
					listTabSpec.get ()

						.name (
							"all")

						.label (
							stringFormat (
								"All %s",
								consoleHelper.shortNamePlural ()))

						.addCriteria (
							whereNotDeletedCriteriaSpec.get ()))

				.put (
					"deleted",
					listTabSpec.get ()

						.name (
							"deleted")

						.label (
							"Deleted")

						.addCriteria (
							whereDeletedCriteriaSpec.get ())

						.addCriteria (
							whereICanManageCriteriaSpec.get ()))

				.build ();

		} else {

			return ImmutableMap.<String,ObjectListTabSpec>builder ()

				.put (
					"all",
					listTabSpec.get ()

						.name (
							"all")

						.label (
							stringFormat (
								"All %s",
								consoleHelper.shortNamePlural ())))

				.build ();

		}

	}

}
