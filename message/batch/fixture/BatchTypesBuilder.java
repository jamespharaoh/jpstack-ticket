package wbs.sms.message.batch.fixture;

import lombok.NonNull;

import wbs.framework.builder.Builder;
import wbs.framework.builder.Builder.MissingBuilderBehaviour;
import wbs.framework.builder.annotations.BuildMethod;
import wbs.framework.builder.annotations.BuilderParent;
import wbs.framework.builder.annotations.BuilderSource;
import wbs.framework.builder.annotations.BuilderTarget;
import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.entity.fixtures.ModelFixtureBuilderComponent;
import wbs.framework.entity.meta.model.RecordSpec;
import wbs.framework.entity.model.Model;
import wbs.framework.logging.LogContext;

import wbs.sms.message.batch.metamodel.BatchTypesSpec;

@PrototypeComponent ("batchTypesBuilder")
public
class BatchTypesBuilder
	implements ModelFixtureBuilderComponent {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	// builder

	@BuilderParent
	RecordSpec parent;

	@BuilderSource
	BatchTypesSpec spec;

	@BuilderTarget
	Model <?> model;

	// build

	@BuildMethod
	@Override
	public
	void build (
			@NonNull Transaction parentTransaction,
			@NonNull Builder <Transaction> builder) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"build");

		) {

			builder.descend (
				transaction,
				parent,
				spec.batchTypes (),
				model,
				MissingBuilderBehaviour.error);

		}

	}

}
