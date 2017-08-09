package wbs.services.ticket.core.hibernate;

import java.util.List;

import lombok.NonNull;

import org.hibernate.criterion.Restrictions;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.hibernate.HibernateDao;
import wbs.framework.logging.LogContext;

import wbs.services.ticket.core.model.TicketDao;
import wbs.services.ticket.core.model.TicketFieldTypeRec;
import wbs.services.ticket.core.model.TicketFieldValueRec;
import wbs.services.ticket.core.model.TicketRec;

@SingletonComponent ("ticketDaoHibernate")
public
class TicketDaoHibernate
	extends HibernateDao
	implements TicketDao {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	// implementation

	@Override
	public
	TicketFieldValueRec findTicketFieldValue (
			@NonNull Transaction parentTransaction,
			@NonNull TicketRec ticket,
			@NonNull TicketFieldTypeRec ticketFieldType) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"findTicketFieldValue");

		) {

			return findOneOrNull (
				transaction,
				TicketFieldValueRec.class,

				createCriteria (
					transaction,
					TicketFieldValueRec.class,
					"_ticketFieldValue")

				.add (
					Restrictions.eq (
						"_ticketFieldValue.ticket",
						ticket))

				.add (
					Restrictions.eq (
						"_ticketFieldValue.ticketFieldType",
						ticketFieldType))

			);

		}

	}

	@Override
	public
	List <TicketRec> findUnqueuedTickets (
			@NonNull Transaction parentTransaction) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"findUnqueuedTickets");

		) {

			return findMany (
				transaction,
				TicketRec.class,

				createCriteria (
					transaction,
					TicketRec.class,
					"_ticket")

				.add (
					Restrictions.eq (
						"_ticket.queued",
						false))

			);

		}

	}

}
