package wbs.framework.entity.generate;

import static wbs.framework.utils.etc.NullUtils.ifNull;
import static wbs.framework.utils.etc.StringUtils.capitalise;
import static wbs.framework.utils.etc.StringUtils.naivePluralise;
import static wbs.framework.utils.etc.StringUtils.stringEqual;
import static wbs.framework.utils.etc.StringUtils.stringFormat;

import javax.inject.Inject;

import wbs.framework.application.annotations.PrototypeComponent;
import wbs.framework.application.scaffold.PluginManager;
import wbs.framework.application.scaffold.PluginModelSpec;
import wbs.framework.application.scaffold.PluginSpec;
import wbs.framework.builder.Builder;
import wbs.framework.builder.annotations.BuildMethod;
import wbs.framework.builder.annotations.BuilderParent;
import wbs.framework.builder.annotations.BuilderSource;
import wbs.framework.builder.annotations.BuilderTarget;
import wbs.framework.entity.meta.AssociativeCollectionSpec;
import wbs.framework.entity.meta.PropertyWriter;
import wbs.framework.utils.etc.FormatWriter;

@PrototypeComponent ("associativeCollectionWriter")
@ModelWriter
public
class AssociativeCollectionWriter {

	// dependencies

	@Inject
	PluginManager pluginManager;

	// builder

	@BuilderParent
	ModelFieldWriterContext context;

	@BuilderSource
	AssociativeCollectionSpec spec;

	@BuilderTarget
	FormatWriter javaWriter;

	// build

	@BuildMethod
	public
	void build (
			Builder builder) {

		String fullFieldTypeName;

		if (
			stringEqual (
				spec.typeName (),
				"string")
		) {

			fullFieldTypeName =
				"String";

		} else {

			PluginModelSpec fieldTypePluginModel =
				pluginManager.pluginModelsByName ().get (
					spec.typeName ());

			PluginSpec fieldTypePlugin =
				fieldTypePluginModel.plugin ();

			fullFieldTypeName =
				stringFormat (
					"%s.model.%sRec",
					fieldTypePlugin.packageName (),
					capitalise (
						spec.typeName ()));

		}

		String fieldName =
			ifNull (
				spec.name (),
				naivePluralise (
					spec.typeName ()));

		// write field

		new PropertyWriter ()

			.thisClassNameFormat (
				"%s",
				context.recordClassName ())

			.typeNameFormat (
				"Set<%s>",
				fullFieldTypeName)

			.propertyNameFormat (
				"%s",
				fieldName)

			.defaultValueFormat (
				"new LinkedHashSet<%s> ()",
				fullFieldTypeName)

			.write (
				javaWriter,
				"\t");

	}

}
