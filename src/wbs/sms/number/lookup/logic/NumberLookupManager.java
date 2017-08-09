package wbs.sms.number.lookup.logic;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import wbs.framework.database.Transaction;

import wbs.sms.number.core.model.NumberRec;
import wbs.sms.number.lookup.model.NumberLookupRec;

public
interface NumberLookupManager {

	boolean lookupNumber (
			Transaction parentTransaction,
			NumberLookupRec numberLookup,
			NumberRec number);

	Pair <List <NumberRec>, List <NumberRec>> splitNumbersPresent (
			Transaction parentTransaction,
			NumberLookupRec numberLookup,
			List <NumberRec> numbers);

}
