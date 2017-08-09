package wbs.services.ticket.core.logic;

import static wbs.utils.etc.OptionalUtils.optionalAbsent;
import static wbs.utils.etc.OptionalUtils.optionalOrNull;

import com.google.common.base.Optional;

import lombok.NonNull;

import org.hibernate.TransientObjectException;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.component.annotations.WeakSingletonDependency;
import wbs.framework.database.Database;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.entity.record.Record;
import wbs.framework.logging.LogContext;
import wbs.framework.object.ObjectHelper;
import wbs.framework.object.ObjectHooks;
import wbs.framework.object.ObjectManager;

import wbs.platform.object.core.model.ObjectTypeRec;
import wbs.platform.queue.logic.QueueLogic;
import wbs.platform.queue.model.QueueItemRec;

import wbs.utils.random.RandomLogic;

import wbs.services.ticket.core.model.TicketFieldTypeObjectHelper;
import wbs.services.ticket.core.model.TicketFieldTypeRec;
import wbs.services.ticket.core.model.TicketFieldValueObjectHelper;
import wbs.services.ticket.core.model.TicketFieldValueRec;
import wbs.services.ticket.core.model.TicketObjectHelper;
import wbs.services.ticket.core.model.TicketRec;

public
class TicketHooks
	implements ObjectHooks <TicketRec> {

	// singleton dependencies

	@SingletonDependency
	Database database;

	@ClassSingletonDependency
	LogContext logContext;

	@WeakSingletonDependency
	ObjectManager objectManager;

	@WeakSingletonDependency
	QueueLogic queueLogic;

	@SingletonDependency
	RandomLogic randomLogic;

	@WeakSingletonDependency
	TicketFieldTypeObjectHelper ticketFieldTypeHelper;

	@WeakSingletonDependency
	TicketFieldValueObjectHelper ticketFieldValueHelper;

	@WeakSingletonDependency
	TicketObjectHelper ticketHelper;

	// implementation

	@Override
	public
	void afterInsert (
			@NonNull Transaction parentTransaction,
			@NonNull TicketRec ticket) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"afterInsert");

		) {

			// TODO does not belong here

			if (ticket.getTicketState ().getShowInQueue ()) {

				// create queue item

				QueueItemRec queueItem =
					queueLogic.createQueueItem (
						transaction,
						ticket.getTicketState (),
						"default",
						ticket,
						ticket,
						ticket.getCode (),
						ticket.getTicketState ().toString ());

				// add queue item to ticket

				ticket

					.setQueueItem (
						queueItem);

			}

		}

	}

	@Override
	public
	Object getDynamic (
			@NonNull Transaction parentTransaction,
			@NonNull TicketRec ticket,
			@NonNull String name) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"getDynamic");

		) {

			// find the ticket field type

			TicketFieldTypeRec ticketFieldType =
				ticketFieldTypeHelper.findByCodeRequired (
					transaction,
					ticket.getTicketManager (),
					name);

			// find the ticket field value

			TicketFieldValueRec ticketFieldValue =
				ticket.getTicketFieldValues ().get (
					ticketFieldType.getId ());

			if (ticketFieldValue == null) {
				return null;
			}

			switch (ticketFieldType.getDataType ()) {

			case string:

				return ticketFieldValue.getStringValue ();

			case number:

				return ticketFieldValue.getIntegerValue ();

			case bool:

				return ticketFieldValue.getBooleanValue ();

			case object:

				ObjectTypeRec objectType =
					ticketFieldType.getObjectType ();

				Long objectId =
					ticketFieldValue.getIntegerValue ();

				ObjectHelper <?> objectHelper =
					objectManager.objectHelperForTypeIdRequired (
						objectType.getId ());

				Object object =
					objectHelper.findRequired (
						transaction,
						objectId);

				return object;

			default:

				throw new RuntimeException ();

			}

		} catch (TransientObjectException exception) {

			// object not yet saved so fields will all be null

			return null;

		}

	}

	@Override
	public
	Optional <String> setDynamic (
			@NonNull Transaction parentTransaction,
			@NonNull TicketRec ticket,
			@NonNull String name,
			@NonNull Optional <?> valueOptional) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"setDynamic");

		) {

			// find the ticket field type

			TicketFieldTypeRec ticketFieldType =
				ticketFieldTypeHelper.findByCodeRequired (
					transaction,
					ticket.getTicketManager (),
					name);

			TicketFieldValueRec ticketFieldValue;

			try {

				 ticketFieldValue =
					ticket.getTicketFieldValues ().get (
						ticketFieldType.getId ());

			} catch (Exception exception) {

				ticketFieldValue =
					null;

			}

			// if the value object does not exist, a new one is created

			if (ticketFieldValue == null) {

				ticketFieldValue =
					ticketFieldValueHelper.createInstance ()

					.setTicket (
						ticket)

					.setTicketFieldType (
						ticketFieldType);

			}

			switch (ticketFieldType.getDataType ()) {

			case string:

				ticketFieldValue

					.setStringValue (
						(String)
						optionalOrNull (
							valueOptional));

				break;

			case number:

				ticketFieldValue

					.setIntegerValue (
						(Long)
						optionalOrNull (
							valueOptional));

				break;

			case bool:

				ticketFieldValue

					.setBooleanValue (
						(Boolean)
						optionalOrNull (
							valueOptional));

				break;

			case object:

				Record<?> record =
					(Record<?>)
					optionalOrNull (
						valueOptional);

				ticketFieldValue.setIntegerValue (
					record.getId ());

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

		return optionalAbsent ();

	}

}