package wbs.framework.entity.meta;

import lombok.Data;
import lombok.experimental.Accessors;
import wbs.framework.application.annotations.PrototypeComponent;
import wbs.framework.data.annotations.DataAttribute;
import wbs.framework.data.annotations.DataClass;

@Accessors (fluent = true)
@Data
@DataClass ("timestamp-field")
@PrototypeComponent ("timestampFieldSpec")
@ModelMetaData
public
class TimestampFieldSpec
	implements ModelFieldSpec {

	@DataAttribute (
		required = true)
	String name;

	@DataAttribute (
		required = true)
	ColumnType columnType;

	@DataAttribute
	Boolean nullable;

	@DataAttribute (
		value = "column")
	String columnName;

	public static
	enum ColumnType {
		unix,       // time since epoch
		iso,        // iso datetime string
		sql,        // sql timestamp
		postgresql; // postgresql timestamp with time zone (java date)
	}

}