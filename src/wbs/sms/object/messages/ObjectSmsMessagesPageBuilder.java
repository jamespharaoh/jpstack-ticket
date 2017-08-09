package wbs.sms.object.messages;

import static wbs.utils.collection.CollectionUtils.emptyList;
import static wbs.utils.collection.CollectionUtils.singletonList;
import static wbs.utils.etc.NullUtils.ifNull;
import static wbs.utils.string.StringUtils.capitalise;
import static wbs.utils.string.StringUtils.stringFormat;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import lombok.NonNull;

import wbs.console.context.ConsoleContextBuilderContainer;
import wbs.console.context.ResolvedConsoleContextExtensionPoint;
import wbs.console.helper.core.ConsoleHelper;
import wbs.console.helper.manager.ConsoleObjectManager;
import wbs.console.module.ConsoleMetaManager;
import wbs.console.module.ConsoleModuleBuilderComponent;
import wbs.console.module.ConsoleModuleImplementation;
import wbs.console.part.PagePartFactory;
import wbs.console.request.ConsoleRequestContext;
import wbs.console.responder.ConsoleFile;
import wbs.console.tab.ConsoleContextTab;
import wbs.console.tab.TabContextResponder;

import wbs.framework.builder.Builder;
import wbs.framework.builder.annotations.BuildMethod;
import wbs.framework.builder.annotations.BuilderParent;
import wbs.framework.builder.annotations.BuilderSource;
import wbs.framework.builder.annotations.BuilderTarget;
import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.PrototypeDependency;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.component.manager.ComponentProvider;
import wbs.framework.database.Database;
import wbs.framework.database.NestedTransaction;
import wbs.framework.entity.record.Record;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

import wbs.platform.affiliate.model.AffiliateRec;
import wbs.platform.service.model.ServiceRec;

import wbs.sms.message.batch.model.BatchRec;
import wbs.sms.message.core.console.MessageSource;
import wbs.sms.message.core.console.MessageSourceImplementation;
import wbs.sms.message.core.model.MessageSearch;
import wbs.sms.route.core.model.RouteRec;

import wbs.web.responder.WebResponder;

@PrototypeComponent ("objectSmsMessagesPageBuilder")
public
class ObjectSmsMessagesPageBuilder <
	ObjectType extends Record <ObjectType>
> implements ConsoleModuleBuilderComponent {

	// singleton dependencies

	@SingletonDependency
	ConsoleMetaManager consoleMetaManager;

	@SingletonDependency
	Database database;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ConsoleObjectManager objectManager;

	@SingletonDependency
	ConsoleRequestContext requestContext;

	// prototype dependencies

	@PrototypeDependency
	ComponentProvider <ConsoleFile> consoleFileProvider;

	@PrototypeDependency
	ComponentProvider <ConsoleContextTab> contextTabProvider;

	@PrototypeDependency
	ComponentProvider <ObjectSmsMessagesPart> messageBrowserPartProvider;

	@PrototypeDependency
	ComponentProvider <MessageSourceImplementation> messageSourceProvider;

	@PrototypeDependency
	ComponentProvider <TabContextResponder> tabContextResponder;

	// builder

	@BuilderParent
	ConsoleContextBuilderContainer <ObjectType> container;

	@BuilderSource
	ObjectSmsMessagesPageSpec objectSmsMessagesPageSpec;

	@BuilderTarget
	ConsoleModuleImplementation consoleModule;

	// state

	ConsoleHelper<ObjectType> consoleHelper;
	String privKey;
	String tabName;
	String fileName;

	ComponentProvider <WebResponder> responderProvider;

	PagePartFactory partFactory;

	// build

	@Override
	@BuildMethod
	public
	void build (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull Builder <TaskLogger> builder) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"build");

		) {

			setDefaults ();

			buildPartFactory ();
			buildResponder ();

			for (
				ResolvedConsoleContextExtensionPoint resolvedExtensionPoint
					: consoleMetaManager.resolveExtensionPoint (
						taskLogger,
						container.extensionPointName ())
			) {

				buildContextTab (
					taskLogger,
					resolvedExtensionPoint);

				buildContextFile (
					taskLogger,
					resolvedExtensionPoint);

			}

		}

	}

	void buildContextTab (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull ResolvedConsoleContextExtensionPoint extensionPoint) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"buildContextTab");

		) {

			consoleModule.addContextTab (
				taskLogger,
				"end",
				contextTabProvider.provide (
					taskLogger)

					.name (
						tabName)

					.defaultLabel (
						"Messages")

					.localFile (
						fileName)

					.privKeys (
						privKey),

				extensionPoint.contextTypeNames ());

		}

	}

	void buildPartFactory () {

		partFactory =
			parentTransaction -> {

			try (

				NestedTransaction transaction =
					parentTransaction.nestTransaction (
						logContext,
						"buildPartFactory");

			) {

				Record <?> object =
					consoleHelper.lookupObject (
						transaction,
						requestContext.consoleContextStuffRequired ());

				MessageSearch search =
					new MessageSearch ();

				List <AffiliateRec> affiliates =
					emptyList ();

				List <ServiceRec> services =
					emptyList ();

				List <BatchRec> batches =
					emptyList ();

				List <RouteRec> routes =
					emptyList ();

				if (object instanceof AffiliateRec) {

					affiliates =
						Collections.singletonList (
							(AffiliateRec)
							object);

				} else if (object instanceof ServiceRec) {

					services =
						Collections.singletonList (
							(ServiceRec)
							object);

				} else if (object instanceof BatchRec) {

					batches =
						Collections.singletonList (
							(BatchRec)
							object);

				} else if (object instanceof RouteRec) {

					routes =
						Collections.singletonList (
							(RouteRec)
							object);

				} else {

					affiliates =
						objectManager.getChildren (
							transaction,
							object,
							AffiliateRec.class);

					services =
						objectManager.getChildren (
							transaction,
							object,
							ServiceRec.class);

					batches =
						objectManager.getChildren (
							transaction,
							object,
							BatchRec.class);

				}

				if (
					affiliates.isEmpty ()
					&& services.isEmpty ()
					&& batches.isEmpty ()
					&& routes.isEmpty ()
				) {

					throw new RuntimeException (
						stringFormat (
							"No affiliates, services, batches or routes ",
							"for %s",
							objectManager.objectPath (
								transaction,
								object)));

				}

				if (
					(
					  (affiliates.isEmpty () ? 0 : 1)
					+ (services.isEmpty () ? 0 : 1)
					+ (batches.isEmpty () ? 0 : 1)
					+ (routes.isEmpty () ? 0 : 1)
					) > 1
				) {
					throw new RuntimeException ();
				}

				if (! affiliates.isEmpty ()) {

					search.affiliateIdIn (
						affiliates.stream ()

						.map (
							AffiliateRec::getId)

						.collect (
							Collectors.toList ())

					);

				}

				if (! services.isEmpty ()) {

					search.serviceIdIn (
						services.stream ()

						.map (
							ServiceRec::getId)

						.collect (
							Collectors.toList ())

					);

				}

				if (! batches.isEmpty ()) {

					search.batchIdIn (
						batches.stream ()

						.map (
							BatchRec::getId)

						.collect (
							Collectors.toList ())

					);

				}

				if (! routes.isEmpty ()) {

					search.routeIdIn (
						routes.stream ()

						.map (
							RouteRec::getId)

						.collect (
								Collectors.toList ())

					);

				}

				MessageSource source =
					messageSourceProvider.provide (
						transaction)

					.searchTemplate (
						search)

				;

				return messageBrowserPartProvider.provide (
					transaction)

					.localName (
						"/" + fileName)

					.messageSource (
						source)

				;

			}

		};

	}

	void buildContextFile (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull ResolvedConsoleContextExtensionPoint extensionPoint) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"buildContextFile");

		) {

			consoleModule.addContextFile (
				fileName,
				consoleFileProvider.provide (
					taskLogger)

					.getResponderProvider (
						responderProvider)

					.privKeys (
						taskLogger,
						singletonList (
							privKey)),

				extensionPoint.contextTypeNames ()
			);

		}

	}

	void buildResponder () {

		responderProvider =
			taskLoggerNested ->
				tabContextResponder.provide (
					taskLoggerNested)

			.tab (
				tabName)

			.title (
				capitalise (
					consoleHelper.friendlyName () + " messages"))

			.pagePartFactory (
				partFactory)

		;

	}

	// defaults

	void setDefaults () {

		consoleHelper =
			container.consoleHelper ();

		privKey =
			ifNull (
				objectSmsMessagesPageSpec.privKey (),
				stringFormat (
					"%s.messages",
					consoleHelper.objectName ()));

		tabName =
			ifNull (
				objectSmsMessagesPageSpec.tabName (),
				stringFormat (
					"%s.messages",
					container.pathPrefix ()));

		fileName =
			ifNull (
				objectSmsMessagesPageSpec.fileName (),
				stringFormat (
					"%s.messages",
					container.pathPrefix ()));

		/*
		responderName =
			ifNull (
				objectSmsMessagesPageSpec.responderName (),
				stringFormat (
					"%sMessagesResponder",
					container.newBeanNamePrefix ()));
		*/

	}

}
