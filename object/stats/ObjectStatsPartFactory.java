package wbs.sms.object.stats;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import wbs.console.helper.manager.ConsoleObjectManager;
import wbs.console.lookup.ObjectLookup;
import wbs.console.part.PagePart;
import wbs.console.part.PagePartFactory;
import wbs.console.request.ConsoleRequestContext;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.PrototypeDependency;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.component.manager.ComponentProvider;
import wbs.framework.database.Database;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.entity.record.Record;
import wbs.framework.logging.LogContext;

import wbs.sms.message.stats.console.GenericMessageStatsPart;
import wbs.sms.message.stats.console.SmsStatsCriteria;
import wbs.sms.message.stats.console.SmsStatsSource;
import wbs.sms.message.stats.console.SmsStatsSourceImplementation;

@Accessors (fluent = true)
@PrototypeComponent ("objectStatsPartFactory")
public
class ObjectStatsPartFactory
	implements PagePartFactory {

	// singelton dependencies

	@SingletonDependency
	Database database;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ConsoleObjectManager objectManager;

	@SingletonDependency
	List <ObjectStatsSourceBuilder> objectStatsSourceBuilders;

	@SingletonDependency
	ConsoleRequestContext requestContext;

	// prototype dependencies

	@PrototypeDependency
	ComponentProvider <GenericMessageStatsPart> smsStatsPartProvider;

	@PrototypeDependency
	ComponentProvider <SmsStatsSourceImplementation> smsStatsSourceProvider;

	// properties

	@Getter @Setter
	String localName;

	@Getter @Setter
	ObjectLookup <? extends Record <?>> objectLookup;

	// implementation

	@Override
	public
	PagePart buildPagePart (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"buildPagePart");

		) {

			// lookup object

			Record <?> parent =
				objectLookup.lookupObject (
					transaction,
					requestContext.consoleContextStuffRequired ());

			// find its services

			List <SmsStatsSource> statsSources =
				new ArrayList<> ();

			for (
				ObjectStatsSourceBuilder objectStatsSourceBuilder
					: objectStatsSourceBuilders
			) {

				SmsStatsSource statsSource =
					objectStatsSourceBuilder.buildStatsSource (
						transaction,
						parent);

				if (statsSource == null)
					continue;

				statsSources.add (
					statsSource);

			}

			if (statsSources.isEmpty ()) {

				throw new RuntimeException (
					"No stats sources found");

			}

			if (statsSources.size () > 1) {

				throw new RuntimeException (
					"Multiple stats sources found");

			}

			SmsStatsSource statsSource =
				statsSources.get (0);

			// set up exclusions

			Set <SmsStatsCriteria> excludes =
				new HashSet<> ();

			// excludes.add (SmsStatsCriteria.service);

			// now create the stats part

			return smsStatsPartProvider.provide (
				transaction,
				smsStatsPart ->
					smsStatsPart

				.url (
					requestContext.resolveLocalUrl (
						localName))

				.statsSource (
					statsSource)

				.excludeCriteria (
					excludes)

			);

		}

	}

}
