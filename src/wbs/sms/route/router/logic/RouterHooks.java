package wbs.sms.route.router.logic;

import static wbs.utils.etc.Misc.doesNotContain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.NonNull;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.NormalLifecycleSetup;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.Database;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.OwnedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.entity.record.Record;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.TaskLogger;
import wbs.framework.object.ObjectHelper;
import wbs.framework.object.ObjectHooks;

import wbs.platform.object.core.model.ObjectTypeDao;
import wbs.platform.object.core.model.ObjectTypeRec;

import wbs.sms.route.router.model.RouterRec;
import wbs.sms.route.router.model.RouterTypeDao;
import wbs.sms.route.router.model.RouterTypeRec;

public
class RouterHooks
	implements ObjectHooks <RouterRec> {

	// singleton dependencies

	@SingletonDependency
	Database database;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ObjectTypeDao objectTypeDao;

	@SingletonDependency
	RouterTypeDao routerTypeDao;

	// state

	Map <Long, List <Long>> routerTypeIdsByParentTypeId =
		new HashMap<> ();

	// lifecycle

	@NormalLifecycleSetup
	public
	void setup (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTransaction transaction =
				database.beginReadOnly (
					logContext,
					parentTaskLogger,
					"setup");

		) {

			routerTypeIdsByParentTypeId =
				routerTypeDao.findAll (
					transaction)

				.stream ()

				.collect (
					Collectors.groupingBy (

					routerType ->
						routerType.getParentType ().getId (),

					Collectors.mapping (
						routerType ->
							routerType.getId (),
						Collectors.toList ())

				)

			);

		}

	}

	// implementation

	@Override
	public
	void createSingletons (
			@NonNull Transaction parentTransaction,
			@NonNull ObjectHelper <RouterRec> routerHelper,
			@NonNull ObjectHelper <?> parentHelper,
			@NonNull Record <?> parent) {

		if (
			doesNotContain (
				routerTypeIdsByParentTypeId.keySet (),
				parentHelper.objectTypeId ())
		) {
			return;
		}

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"createSingletons");

		) {

			ObjectTypeRec parentType =
				objectTypeDao.findById (
					transaction,
					parentHelper.objectTypeId ());

			for (
				Long routerTypeId
					: routerTypeIdsByParentTypeId.get (
						parentHelper.objectTypeId ())
			) {

				RouterTypeRec routerType =
					routerTypeDao.findRequired (
						transaction,
						routerTypeId);

				routerHelper.insert (
					transaction,
					routerHelper.createInstance ()

					.setRouterType (
						routerType)

					.setCode (
						routerType.getCode ())

					.setParentType (
						parentType)

					.setParentId (
						parent.getId ())

				);

			}

		}

	}

}