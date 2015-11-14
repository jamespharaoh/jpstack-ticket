package wbs.platform.affiliate.fixture;

import static wbs.framework.utils.etc.Misc.camelToUnderscore;
import static wbs.framework.utils.etc.Misc.codify;
import static wbs.framework.utils.etc.Misc.ifNull;
import static wbs.framework.utils.etc.Misc.stringFormat;
import static wbs.framework.utils.etc.Misc.underscoreToCamel;

import java.sql.SQLException;

import javax.inject.Inject;

import lombok.Cleanup;
import lombok.NonNull;
import lombok.extern.log4j.Log4j;

import wbs.framework.application.annotations.PrototypeComponent;
import wbs.framework.builder.Builder;
import wbs.framework.builder.annotations.BuildMethod;
import wbs.framework.builder.annotations.BuilderParent;
import wbs.framework.builder.annotations.BuilderSource;
import wbs.framework.builder.annotations.BuilderTarget;
import wbs.framework.database.Database;
import wbs.framework.database.Transaction;
import wbs.framework.entity.helper.EntityHelper;
import wbs.framework.entity.meta.ModelMetaBuilderHandler;
import wbs.framework.entity.meta.ModelMetaSpec;
import wbs.framework.entity.model.Model;
import wbs.framework.record.GlobalId;
import wbs.platform.affiliate.metamodel.AffiliateTypeSpec;
import wbs.platform.affiliate.model.AffiliateTypeObjectHelper;
import wbs.platform.object.core.model.ObjectTypeObjectHelper;
import wbs.platform.object.core.model.ObjectTypeRec;

@Log4j
@PrototypeComponent ("affiliateTypeBuilder")
@ModelMetaBuilderHandler
public
class AffiliateTypeBuilder {

	// dependencies

	@Inject
	AffiliateTypeObjectHelper affiliateTypeHelper;

	@Inject
	Database database;

	@Inject
	EntityHelper entityHelper;

	@Inject
	ObjectTypeObjectHelper objectTypeHelper;

	// builder

	@BuilderParent
	ModelMetaSpec parent;

	@BuilderSource
	AffiliateTypeSpec spec;

	@BuilderTarget
	Model model;

	// build

	@BuildMethod
	public
	void build (
			@NonNull Builder builder) {

		try {

			log.info (
				stringFormat (
					"Create affiliate type %s.%s",
					camelToUnderscore (
						ifNull (
							spec.subject (),
							parent.name ())),
					codify (
						spec.name ())));

			createAffiliateType ();

		} catch (Exception exception) {

			throw new RuntimeException (
				stringFormat (
					"Error creating affiliate type %s.%s",
					camelToUnderscore (
						ifNull (
							spec.subject (),
							parent.name ())),
					codify (
						spec.name ())),
				exception);

		}

	}

	private
	void createAffiliateType ()
		throws SQLException {

		@Cleanup
		Transaction transaction =
			database.beginReadWrite (
				this);

		String parentTypeCode =
			camelToUnderscore (
				ifNull (
					spec.subject (),
					parent.name ()));

		ObjectTypeRec parentType =
			objectTypeHelper.findByCode (
				GlobalId.root,
				parentTypeCode);

		Model parentModel =
			entityHelper.modelsByName ().get (
				underscoreToCamel (
					parentTypeCode));

		if (parentModel == null) {

			throw new RuntimeException (
				stringFormat (
					"No model for %s",
					parentTypeCode));

		}

		affiliateTypeHelper.insert (
			affiliateTypeHelper.createInstance ()

			.setParentType (
				parentType)

			.setCode (
				codify (
					spec.name ()))

			.setName (
				spec.name ())

			.setDescription (
				spec.description ())

		);

		transaction.commit ();

	}

}
