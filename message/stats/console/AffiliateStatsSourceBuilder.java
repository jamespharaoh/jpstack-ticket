package wbs.sms.message.stats.console;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;

import lombok.NonNull;

import wbs.console.helper.manager.ConsoleObjectManager;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeDependency;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.component.manager.ComponentProvider;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.entity.record.Record;
import wbs.framework.logging.LogContext;

import wbs.platform.affiliate.model.AffiliateRec;

import wbs.sms.object.stats.ObjectStatsSourceBuilder;

@SingletonComponent ("affiliateStatsSourceBuilder")
public
class AffiliateStatsSourceBuilder
	implements ObjectStatsSourceBuilder {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ConsoleObjectManager objectManager;

	// prototype dependencies

	@PrototypeDependency
	ComponentProvider <SmsStatsSourceImplementation> smsStatsSourceProvider;

	// implementation

	@Override
	public
	SmsStatsSource buildStatsSource (
			@NonNull Transaction parentTransaction,
			@NonNull Record <?> parent) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"buildStatsSource");

		) {

			List <AffiliateRec> affiliates;

			if ((Object) parent instanceof AffiliateRec) {

				affiliates =
					Collections.singletonList (
						(AffiliateRec)
						parent);

			} else {

				affiliates =
					objectManager.getChildren (
						transaction,
						parent,
						AffiliateRec.class);

			}

			if (affiliates.isEmpty ())
				return null;

			Set<Long> affiliateIds =
				affiliates.stream ()

				.map (
					AffiliateRec::getId)

				.collect (
					Collectors.toSet ());

			return smsStatsSourceProvider.provide (
				transaction,
				smsStatsSource ->
					smsStatsSource

				.fixedCriteriaMap (
					ImmutableMap.of (
						SmsStatsCriteria.affiliate,
						affiliateIds))

			);

		}

	}

}
