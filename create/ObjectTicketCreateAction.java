package wbs.services.ticket.create;

import static wbs.utils.collection.MapUtils.emptyMap;
import static wbs.utils.etc.OptionalUtils.optionalAbsent;
import static wbs.utils.etc.OptionalUtils.optionalGetRequired;
import static wbs.utils.etc.OptionalUtils.optionalIsNotPresent;
import static wbs.utils.etc.OptionalUtils.optionalOf;
import static wbs.utils.etc.TypeUtils.genericCastUnchecked;
import static wbs.utils.string.StringUtils.capitalise;
import static wbs.utils.string.StringUtils.stringFormat;

import java.util.List;

import com.google.common.base.Optional;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import wbs.console.action.ConsoleAction;
import wbs.console.forms.core.ConsoleForm;
import wbs.console.forms.core.ConsoleFormType;
import wbs.console.helper.core.ConsoleHelper;
import wbs.console.helper.manager.ConsoleObjectManager;
import wbs.console.module.ConsoleManager;
import wbs.console.priv.UserPrivChecker;
import wbs.console.request.ConsoleRequestContext;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.component.manager.ComponentProvider;
import wbs.framework.database.Database;
import wbs.framework.database.OwnedTransaction;
import wbs.framework.entity.record.PermanentRecord;
import wbs.framework.entity.record.Record;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

import wbs.platform.event.logic.EventLogic;
import wbs.platform.object.core.model.ObjectTypeObjectHelper;
import wbs.platform.queue.logic.QueueLogic;
import wbs.platform.scaffold.model.RootObjectHelper;
import wbs.platform.text.model.TextObjectHelper;
import wbs.platform.user.console.UserConsoleLogic;

import wbs.utils.etc.PropertyUtils;

import wbs.services.ticket.core.console.TicketConsoleHelper;
import wbs.services.ticket.core.console.TicketFieldValueConsoleHelper;
import wbs.services.ticket.core.model.TicketFieldTypeObjectHelper;
import wbs.services.ticket.core.model.TicketFieldTypeRec;
import wbs.services.ticket.core.model.TicketFieldValueRec;
import wbs.services.ticket.core.model.TicketManagerRec;
import wbs.services.ticket.core.model.TicketRec;
import wbs.web.responder.WebResponder;

@Accessors (fluent = true)
@PrototypeComponent ("objectTicketCreateAction")
public
class ObjectTicketCreateAction <
	ObjectType extends Record <ObjectType>,
	ParentType extends Record <ParentType>
>
	extends ConsoleAction {

	// singleton dependencies

	@SingletonDependency
	ConsoleManager consoleManager;

	@SingletonDependency
	Database database;

	@SingletonDependency
	EventLogic eventLogic;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ConsoleObjectManager objectManager;

	@SingletonDependency
	ObjectTypeObjectHelper objectTypeHelper;

	@SingletonDependency
	UserPrivChecker privChecker;

	@SingletonDependency
	QueueLogic queueLogic;

	@SingletonDependency
	ConsoleRequestContext requestContext;

	@SingletonDependency
	RootObjectHelper rootHelper;

	@SingletonDependency
	TextObjectHelper textHelper;

	@SingletonDependency
	TicketFieldTypeObjectHelper ticketFieldTypeHelper;

	@SingletonDependency
	TicketFieldValueConsoleHelper ticketFieldValueHelper;

	@SingletonDependency
	TicketConsoleHelper ticketHelper;

	@SingletonDependency
	UserConsoleLogic userConsoleLogic;

	// properties

	@Getter @Setter
	ConsoleHelper <TicketRec> consoleHelper;

	@Getter @Setter
	String typeCode;

	@Getter @Setter
	ComponentProvider <WebResponder> responderProvider;

	@Getter @Setter
	String targetContextTypeName;

	@Getter @Setter
	ComponentProvider <WebResponder> targetResponderProvider;

	@Getter @Setter
	String createPrivDelegate;

	@Getter @Setter
	String createPrivCode;

	@Getter @Setter
	ConsoleFormType <TicketRec> formContextBuilder;

	@Getter @Setter
	String createTimeFieldName;

	@Getter @Setter
	String createUserFieldName;

	@Getter @Setter
	String ticketManagerPath;

	@Getter @Setter
	List <ObjectTicketCreateSetFieldSpec> ticketFieldSpecs;

	// state

	ConsoleHelper <ParentType> parentHelper;
	TicketManagerRec ticketManager;

	ConsoleForm <ObjectType> formContext;

	// details

	@Override
	public
	WebResponder backupResponder (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"backupResponder");

		) {

			return responderProvider.provide (
				taskLogger);

		}

	}

	@Override
	protected
	WebResponder goReal (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTransaction transaction =
				database.beginReadWrite (
					logContext,
					parentTaskLogger,
					"goReal");

		) {

			// find context object

			Record <?> contextObject =
				consoleHelper.findFromContextRequired (
					transaction);

			// determine ticket

			Optional <TicketManagerRec> ticketManagerOptional =
				genericCastUnchecked (
					objectManager.dereference (
						transaction,
						contextObject,
						ticketManagerPath));

			if (
				optionalIsNotPresent (
					ticketManagerOptional)
			) {
				return null;
			}

			ticketManager =
				optionalGetRequired (
					ticketManagerOptional);

			// check permissions

			if (createPrivCode != null) {

				Record <?> createDelegate =
					createPrivDelegate != null
						? (Record <?>) objectManager.dereferenceObsolete (
							transaction,
							ticketManager,
							createPrivDelegate)
						: ticketManager;

				if (
					! privChecker.canRecursive (
						transaction,
						createDelegate,
						createPrivCode)
				) {

					requestContext.addError (
						"Permission denied");

					return null;

				}

			}

			// create new record and set parent

			TicketRec ticket =
				ticketHelper.createInstance ()

				.setTicketManager (
					ticketManager);

			for (
				ObjectTicketCreateSetFieldSpec ticketFieldSpec
					: ticketFieldSpecs
			) {

				TicketFieldTypeRec ticketFieldType =
					ticketFieldTypeHelper.findByCodeRequired (
						transaction,
						ticketManager,
						ticketFieldSpec.fieldTypeCode ());

				TicketFieldValueRec ticketFieldValue =
					ticketFieldValueHelper.createInstance ()

					.setTicket (
						ticket)

					.setTicketFieldType (
						ticketFieldType);

				switch (ticketFieldType.getDataType ()) {

				case string:

					ticketFieldValue.setStringValue (
						(String)
						objectManager.dereferenceObsolete (
							transaction,
							contextObject,
							ticketFieldSpec.valuePath ()));

					break;

				case number:

					ticketFieldValue.setIntegerValue (
						(Long)
						objectManager.dereferenceObsolete (
							transaction,
							contextObject,
							ticketFieldSpec.valuePath ()));

					break;

				case bool:

					ticketFieldValue.setBooleanValue (
						(Boolean)
						objectManager.dereferenceObsolete (
							transaction,
							contextObject,
							ticketFieldSpec.valuePath ()));

					break;

				case object:

					Long objectId = (
						(Record <?>)
						objectManager.dereferenceObsolete (
							transaction,
							contextObject,
							ticketFieldSpec.valuePath ())
					).getId ();

					ticketFieldValue

						.setIntegerValue (
							objectId);

					break;

				default:

					throw new RuntimeException ();

				}

				ticket

					.setNumFields (
						ticket.getNumFields () + 1);

				ticket.getTicketFieldValues ().put (
					ticketFieldType.getId (),
					ticketFieldValue);

			}

			// set type code

			if (consoleHelper.typeCodeExists ()) {

				PropertyUtils.propertySetAuto (
					ticket,
					consoleHelper.typeCodeFieldName (),
					typeCode);

			}

			// perform updates

			/*
			if (formFieldsProvider != null) {

				prepareFieldSet (
					transaction);

			}
			*/

			ConsoleForm <TicketRec> formContext =
				formContextBuilder.buildAction (
					transaction,
					emptyMap (),
					ticket);

			formContext.update (
				transaction);

			if (formContext.errors ()) {

				formContext.reportErrors (
					transaction);

				return null;

			}

			// set create time

			if (createTimeFieldName != null) {

				PropertyUtils.propertySetAuto (
					ticket,
					createTimeFieldName,
					transaction.now ());

			}

			// set create user

			if (createUserFieldName != null) {

				PropertyUtils.propertySetAuto (
					ticket,
					createUserFieldName,
					userConsoleLogic.userRequired (
						transaction));

			}

			// insert

			ticketHelper.insert (
				transaction,
				ticket);

			// create event

			Object objectRef =
				consoleHelper.codeExists ()
					? consoleHelper.getCode (ticket)
					: ticket.getId ();

			if (consoleHelper.ephemeral ()) {

				eventLogic.createEvent (
					transaction,
					"object_created_in",
					userConsoleLogic.userRequired (
						transaction),
					objectRef,
					consoleHelper.shortName (),
					ticketManager);

			} else {

				eventLogic.createEvent (
					transaction,
					"object_created",
					userConsoleLogic.userRequired (
						transaction),
					ticket,
					ticketManager);

			}

			// update events

			if (ticket instanceof PermanentRecord) {

				formContext.runUpdateHooks (
					transaction,
					(PermanentRecord <?>) ticket,
					optionalAbsent (),
					optionalAbsent ());

			} else {

				formContext.runUpdateHooks (
					transaction,
					(PermanentRecord <?>) ticketManager,
					optionalOf (
						objectRef),
					optionalOf (
						consoleHelper.shortName ()));

			}

			// commit transaction

			transaction.commit ();

			// prepare next page

			requestContext.addNotice (
				stringFormat (
					"%s created",
					capitalise (
						consoleHelper.shortName ())));

			requestContext.setEmptyFormData ();

			return targetResponderProvider.provide (
				transaction);

		}

	}

}
