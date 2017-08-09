package wbs.services.ticket.core.model;

import java.util.List;

import wbs.framework.database.Transaction;

public
interface TicketDaoMethods {

	TicketFieldValueRec findTicketFieldValue (
			Transaction parentTransaction,
			TicketRec ticket,
			TicketFieldTypeRec ticketFieldType);

	List <TicketRec> findUnqueuedTickets (
			Transaction parentTransaction);

}