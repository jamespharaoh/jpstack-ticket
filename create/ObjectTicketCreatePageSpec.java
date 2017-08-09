package wbs.services.ticket.create;

import java.util.List;

import lombok.Data;
import lombok.experimental.Accessors;

import wbs.console.module.ConsoleModuleSpec;
import wbs.console.module.ConsoleSpec;

import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.data.annotations.DataAncestor;
import wbs.framework.data.annotations.DataAttribute;
import wbs.framework.data.annotations.DataChildren;
import wbs.framework.data.annotations.DataClass;

@Accessors (fluent = true)
@Data
@DataClass ("object-ticket-create-page")
@PrototypeComponent ("objectTicketCreatePageSpec")
public
class ObjectTicketCreatePageSpec
	implements ConsoleSpec {

	// tree attributes

	@DataAncestor
	ConsoleModuleSpec consoleSpec;

	// attributes

	@DataAttribute (
		name = "ticket-manager")
	String ticketManager;

	@DataAttribute (
		name = "fields-provider")
	String fieldsProviderName;

	@DataAttribute
	String typeCode;

	@DataAttribute
	String name;

	@DataAttribute
	String title;

	@DataAttribute (
		name = "tab")
	String tabName;

	@DataAttribute
	String tabLabel;

	@DataAttribute
	String localFile;

	@DataAttribute (
		name = "responder")
	String responderName;

	@DataAttribute (
		name = "target-context-type")
	String targetContextTypeName;

	@DataAttribute (
		name = "target-responder")
	String targetResponderName;

	@DataAttribute
	Boolean hideTab = false;

	@DataAttribute
	String createPrivDelegate;

	@DataAttribute
	String createPrivCode;

	// children

	@DataChildren (
		direct = true)
	List<ObjectTicketCreateSetFieldSpec> ticketFields;

}
