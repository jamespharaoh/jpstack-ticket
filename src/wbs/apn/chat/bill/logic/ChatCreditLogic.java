package wbs.apn.chat.bill.logic;

import com.google.common.base.Optional;

import lombok.Data;
import lombok.experimental.Accessors;

import org.joda.time.LocalDate;

import wbs.apn.chat.bill.model.ChatUserSpendRec;
import wbs.apn.chat.user.core.model.ChatUserRec;

public
interface ChatCreditLogic {

	void userReceiveSpend (
			ChatUserRec toUser,
			int receivedMessageCount);

	/**
	 * Bills a user the specified amount. This increases their valueSinceXxx
	 * counters and reduces their credit. If they have free usage it increases
	 * their creditAdded by the same amount and the overall credit is
	 * unaffected.
	 */
	void userSpend (
			ChatUserRec chatUser,
			int userMessageCount,
			int monitorMessageCount,
			int textProfileCount,
			int imageProfileCount,
			int videoProfileCount);

	void chatUserSpendBasic (
		ChatUserRec chatUser,
		int amount);

	/**
	 * @param chatUser
	 *            the chat user to associate with
	 * @param date
	 *            the date to associate with
	 * @return the new/existing chat user spend record
	 */
	ChatUserSpendRec findOrCreateChatUserSpend (
			ChatUserRec chatUser,
			LocalDate date);

	ChatCreditCheckResult userSpendCreditCheck (
			ChatUserRec chatUser,
			Boolean userActed,
			Optional<Long> threadId);

	ChatCreditCheckResult userCreditCheck (
			ChatUserRec chatUser);

	ChatCreditCheckResult userCreditCheckStrict (
			ChatUserRec chatUser);

	Optional <String> userBillCheck (
			ChatUserRec chatUser,
			BillCheckOptions options);

	void userBill (
			ChatUserRec chatUser,
			BillCheckOptions options);

	void userBillReal (
			ChatUserRec chatUser,
			boolean updateRevoked);

	long userBillLimitAmount (
			ChatUserRec chatUser);

	boolean userBillLimitApplies (
			ChatUserRec chatUser);

	/**
	 * Sends a credit hint to a chat user, unless they are barred or blocked or
	 * have had one very recently.
	 *
	 * @param chatUser
	 *            the chat user to send the hint to
	 * @param threadId
	 *            the threadId to associate the message with, or null
	 */
	void userCreditHint (
			ChatUserRec chatUser,
			Optional<Long> threadId);

	void doRebill ();

	/**
	 * Checks if a user has a credit limit less than their successful delivered
	 * count rounded down to the nearest thousand (ten pounds) plus one thousand
	 * (ten pounds), if so it raises their credit limit to that amount and logs
	 * the event.
	 *
	 * @param chatUser
	 *            The user to check
	 */
	void creditLimitUpdate (
			ChatUserRec chatUser);

	String userCreditDebug (
			ChatUserRec chatUser);

	@Accessors (fluent = true)
	@Data
	public static
	class BillCheckOptions {
		Boolean retry = false;
		Boolean includeBlocked = false;
		Boolean includeFailed = false;
	}

}