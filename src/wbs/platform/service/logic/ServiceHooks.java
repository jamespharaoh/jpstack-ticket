package wbs.platform.service.logic;

import static wbs.framework.utils.etc.Misc.doesNotContain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Provider;

import com.google.common.base.Optional;

import lombok.Cleanup;
import lombok.NonNull;
import wbs.framework.database.Database;
import wbs.framework.database.Transaction;
import wbs.framework.entity.record.Record;
import wbs.framework.object.ObjectHelper;
import wbs.framework.object.ObjectHooks;
import wbs.framework.object.ObjectManager;
import wbs.platform.object.core.model.ObjectTypeDao;
import wbs.platform.object.core.model.ObjectTypeRec;
import wbs.platform.scaffold.model.SliceRec;
import wbs.platform.service.model.ServiceRec;
import wbs.platform.service.model.ServiceTypeDao;
import wbs.platform.service.model.ServiceTypeRec;

public
class ServiceHooks
	implements ObjectHooks<ServiceRec> {

	// dependencies

	@Inject
	Database database;

	@Inject
	ObjectTypeDao objectTypeDao;

	@Inject
	ServiceTypeDao serviceTypeDao;

	// indirect dependencies

	@Inject
	Provider<ObjectManager> objectManagerProvider;

	// state

	Map<Long,List<Long>> serviceTypeIdsByParentTypeId =
		new HashMap<> ();

	// lifecycle

	@PostConstruct
	public
	void init () {

		@Cleanup
		Transaction transaction =
			database.beginReadOnly (
				"serviceHooks.init ()",
				this);

		// preload object types

		objectTypeDao.findAll ();

		// load service types and construct index

		serviceTypeIdsByParentTypeId =
			serviceTypeDao.findAll ().stream ()

			.collect (
				Collectors.groupingBy (

				serviceType ->
					serviceType.getParentType ().getId (),

				Collectors.mapping (
					serviceType ->
						serviceType.getId (),
					Collectors.toList ()))

			);

	}

	// implementation

	@Override
	public
	void createSingletons (
			@NonNull ObjectHelper <ServiceRec> serviceHelper,
			@NonNull ObjectHelper <?> parentHelper,
			@NonNull Record <?> parent) {

		if (
			doesNotContain (
				serviceTypeIdsByParentTypeId.keySet (),
				parentHelper.objectTypeId ())
		) {
			return;
		}

		ObjectManager objectManager =
		   objectManagerProvider.get ();

		Optional <SliceRec> slice =
			objectManager.getAncestor (
				SliceRec.class,
				parent);

		ObjectTypeRec parentType =
			objectTypeDao.findById (
				parentHelper.objectTypeId ());

		for (
			Long serviceTypeId
				: serviceTypeIdsByParentTypeId.get (
					parentHelper.objectTypeId ())
		) {

			ServiceTypeRec serviceType =
				serviceTypeDao.findRequired (
					serviceTypeId);

			serviceHelper.insert (
				serviceHelper.createInstance ()

				.setServiceType (
					serviceType)

				.setCode (
					serviceType.getCode ())

				.setParentType (
					parentType)

				.setParentId (
					parent.getId ())

				.setSlice (
					slice.orNull ())

			);

		}

	}

}