package wbs.console.object;

import static wbs.framework.utils.etc.Misc.ifNull;
import static wbs.framework.utils.etc.Misc.naivePluralise;
import static wbs.framework.utils.etc.Misc.stringFormat;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;

import com.google.common.collect.ImmutableList;

import wbs.console.annotations.ConsoleMetaModuleBuilderHandler;
import wbs.console.context.ConsoleContextMetaBuilderContainer;
import wbs.console.context.ConsoleContextRootExtensionPoint;
import wbs.console.module.ConsoleMetaModuleImplementation;
import wbs.framework.application.annotations.PrototypeComponent;
import wbs.framework.builder.Builder;
import wbs.framework.builder.Builder.MissingBuilderBehaviour;
import wbs.framework.builder.annotations.BuildMethod;
import wbs.framework.builder.annotations.BuilderParent;
import wbs.framework.builder.annotations.BuilderSource;
import wbs.framework.builder.annotations.BuilderTarget;

@PrototypeComponent ("objectContextMetaBuilder")
@ConsoleMetaModuleBuilderHandler
public
class ObjectContextMetaBuilder {

	// prototype dependencies

	@Inject
	Provider<ConsoleContextRootExtensionPoint> rootExtensionPointProvider;

	// builder

	@BuilderParent
	ConsoleContextMetaBuilderContainer container;

	@BuilderSource
	ObjectContextSpec spec;

	@BuilderTarget
	ConsoleMetaModuleImplementation metaModule;

	// state

	String contextName;
	String beanName;

	Boolean hasListChildren;
	Boolean hasObjectChildren;

	List<String> listContextTypeNames;
	List<String> objectContextTypeNames;

	// build

	@BuildMethod
	public
	void build (
			Builder builder) {

		setDefaults ();

		// extension points

		metaModule.addExtensionPoint (
			rootExtensionPointProvider.get ()

			.name (
				contextName + ":list")

			.contextTypeNames (
				listContextTypeNames)

			.contextLinkNames (
				ImmutableList.<String>of (
					contextName))

			.parentContextNames (
				ImmutableList.<String>of (
					naivePluralise (
						contextName),
					contextName)));

		metaModule.addExtensionPoint (
			rootExtensionPointProvider.get ()

			.name (
				contextName + ":object")

			.contextTypeNames (
				objectContextTypeNames)

			.contextLinkNames (
				ImmutableList.<String>of (
					contextName))

			.parentContextNames (
				ImmutableList.<String>of (
					contextName,
					"link:" + contextName)));

		// descend

		ConsoleContextMetaBuilderContainer listContainer =
			new ConsoleContextMetaBuilderContainer ()

			.structuralName (
				contextName)

			.extensionPointName (
				contextName + ":list");

		builder.descend (
			listContainer,
			spec.listChildren (),
			metaModule,
			MissingBuilderBehaviour.ignore);

		ConsoleContextMetaBuilderContainer objectContainer =
			new ConsoleContextMetaBuilderContainer ()

			.structuralName (
				contextName)

			.extensionPointName (
				contextName + ":object");

		builder.descend (
			objectContainer,
			spec.objectChildren (),
			metaModule,
			MissingBuilderBehaviour.ignore);

	}

	// defaults

	void setDefaults () {

		contextName =
			spec.name ();

		beanName =
			ifNull (
				spec.beanName (),
				contextName);

		if (beanName.contains ("_")) {

			throw new RuntimeException (
				stringFormat (
					"Object context name %s cannot be used as bean name",
					contextName));

		}

		hasListChildren =
			! spec.listChildren ().isEmpty ();

		hasObjectChildren =
			! spec.objectChildren ().isEmpty ();

		// context type names

		listContextTypeNames =
			ImmutableList.<String>builder ()

			.add (
				naivePluralise (
					contextName))

			.add (
				contextName + "+")

			.build ();

		objectContextTypeNames =
			ImmutableList.<String>builder ()

			.add (
				contextName + "+")

			.add (
				contextName)

			.build ();

	}

}
