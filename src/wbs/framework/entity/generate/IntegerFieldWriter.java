package wbs.framework.entity.generate;

import static wbs.framework.utils.etc.Misc.ifNull;

import java.io.IOException;

import wbs.framework.application.annotations.PrototypeComponent;
import wbs.framework.builder.Builder;
import wbs.framework.builder.annotations.BuildMethod;
import wbs.framework.builder.annotations.BuilderParent;
import wbs.framework.builder.annotations.BuilderSource;
import wbs.framework.builder.annotations.BuilderTarget;
import wbs.framework.entity.meta.AnnotationWriter;
import wbs.framework.entity.meta.IntegerFieldSpec;
import wbs.framework.entity.meta.ModelMetaSpec;
import wbs.framework.utils.etc.FormatWriter;

@PrototypeComponent ("integerFieldWriter")
@ModelWriter
public
class IntegerFieldWriter {

	// builder

	@BuilderParent
	ModelMetaSpec parent;

	@BuilderSource
	IntegerFieldSpec spec;

	@BuilderTarget
	FormatWriter javaWriter;

	// build

	@BuildMethod
	public
	void build (
			Builder builder)
		throws IOException {

		// write field annotation

		AnnotationWriter annotationWriter =
			new AnnotationWriter ()

			.name (
				"SimpleField");

		if (ifNull (spec.nullable (), false)) {

			annotationWriter.addAttributeFormat (
				"nullable",
				"true");

		}

		if (spec.columnName () != null) {

			annotationWriter.addAttributeFormat (
				"column",
				"\"%s\"",
				spec.columnName ().replace ("\"", "\\\""));

		}

		annotationWriter.write (
			javaWriter,
			"\t");

		// write field

		if (spec.defaultValue () != null) {

			javaWriter.write (

				"\tInteger %s = %s;\n",
				spec.name (),
				spec.defaultValue ());

		} else {

			javaWriter.write (

				"\tInteger %s;\n",
				spec.name ());

		}

		javaWriter.write (

			"\n");

	}

}