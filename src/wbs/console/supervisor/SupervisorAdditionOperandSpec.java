package wbs.console.supervisor;

import lombok.Data;
import lombok.experimental.Accessors;
import wbs.console.module.ConsoleModuleData;
import wbs.framework.application.annotations.PrototypeComponent;
import wbs.framework.data.annotations.DataAttribute;
import wbs.framework.data.annotations.DataClass;
import wbs.framework.data.annotations.DataParent;

@Accessors (fluent = true)
@Data
@DataClass ("operand")
@PrototypeComponent ("supervisorAdditionOperandSpec")
@ConsoleModuleData
public
class SupervisorAdditionOperandSpec {

	@DataParent
	SupervisorAdditionStatsResolverSpec supervisorAdditionStatsResolverSpec;

	@DataAttribute
	Long coefficient = 1l;

	@DataAttribute
	Long value = 1l;

	@DataAttribute (
		name = "resolver")
	String resolverName;

}
