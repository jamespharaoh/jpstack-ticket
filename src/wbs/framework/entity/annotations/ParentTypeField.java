package wbs.framework.entity.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import wbs.framework.entity.annotations.meta.FieldMeta;
import wbs.framework.entity.model.ModelFieldType;

@Retention (RetentionPolicy.RUNTIME)
@Target (ElementType.FIELD)
@FieldMeta (
	modelFieldType = ModelFieldType.parentType,
	treeParent = true)
public
@interface ParentTypeField {

}