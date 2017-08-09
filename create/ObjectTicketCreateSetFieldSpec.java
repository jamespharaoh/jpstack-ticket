package wbs.services.ticket.create;

import lombok.Data;
import lombok.experimental.Accessors;

import wbs.console.module.ConsoleSpec;

import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.data.annotations.DataAttribute;
import wbs.framework.data.annotations.DataClass;

@Accessors (fluent = true)
@Data
@DataClass ("set-field")
@PrototypeComponent ("objectTicketCreateSetFieldSpec")
public
class ObjectTicketCreateSetFieldSpec
	implements ConsoleSpec {

	// attributes

	@DataAttribute
	String fieldTypeCode;

	@DataAttribute
	String valuePath;

}
