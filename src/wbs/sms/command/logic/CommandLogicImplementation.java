package wbs.sms.command.logic;

import static wbs.utils.etc.OptionalUtils.optionalIsPresent;

import com.google.common.base.Optional;

import lombok.NonNull;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.entity.record.Record;
import wbs.framework.logging.LogContext;
import wbs.framework.object.ObjectManager;

import wbs.platform.object.core.model.ObjectTypeObjectHelper;
import wbs.platform.object.core.model.ObjectTypeRec;

import wbs.sms.command.model.CommandObjectHelper;
import wbs.sms.command.model.CommandRec;
import wbs.sms.command.model.CommandTypeObjectHelper;
import wbs.sms.command.model.CommandTypeRec;

@SingletonComponent ("commandLogic")
public
class CommandLogicImplementation
	implements CommandLogic {

	// singleton dependencies

	@SingletonDependency
	CommandObjectHelper commandHelper;

	@SingletonDependency
	CommandTypeObjectHelper commandTypeHelper;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ObjectTypeObjectHelper objectTypeHelper;

	@SingletonDependency
	ObjectManager objectManager;

	// implementation

	@Override
	public
	CommandRec findOrCreateCommand (
			@NonNull Transaction parentTransaction,
			@NonNull Record <?> parent,
			@NonNull String typeCode,
			@NonNull String code) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"findOrCreateCommand");

		) {

			// lookup existing command...

			Optional <CommandRec> existingCommandOptional =
				commandHelper.findByCode (
					transaction,
					parent,
					code);

			if (
				optionalIsPresent (
					existingCommandOptional)
			) {
				return existingCommandOptional.get ();
			}

			// ...or create new command

			ObjectTypeRec parentType =
				objectTypeHelper.findRequired (
					transaction,
					objectManager.getObjectTypeId (
						transaction,
						parent));

			CommandTypeRec commandType =
				commandTypeHelper.findByCodeRequired (
					transaction,
					parentType,
					typeCode);

			return commandHelper.insert (
				transaction,
				commandHelper.createInstance ()

				.setCode (
					code)

				.setCommandType (
					commandType)

				.setParentType (
					parentType)

				.setParentId (
					parent.getId ())

			);

		}

	}

}
