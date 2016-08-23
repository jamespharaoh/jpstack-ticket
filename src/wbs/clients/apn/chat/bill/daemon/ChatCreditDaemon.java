package wbs.clients.apn.chat.bill.daemon;

import static wbs.framework.utils.etc.StringUtils.stringFormat;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.joda.time.Duration;
import org.joda.time.Instant;

import lombok.Cleanup;
import lombok.NonNull;
import lombok.extern.log4j.Log4j;
import wbs.clients.apn.chat.bill.logic.ChatCreditLogic;
import wbs.clients.apn.chat.user.core.model.ChatUserObjectHelper;
import wbs.clients.apn.chat.user.core.model.ChatUserRec;
import wbs.framework.application.annotations.SingletonComponent;
import wbs.framework.database.Database;
import wbs.framework.database.Transaction;
import wbs.platform.daemon.SleepingDaemonService;

@Log4j
@SingletonComponent ("chatCreditDaemon")
public
class ChatCreditDaemon
	extends SleepingDaemonService {

	// dependencies

	@Inject
	ChatCreditLogic chatCreditLogic;

	@Inject
	ChatUserObjectHelper chatUserHelper;

	@Inject
	Database database;

	// details

	@Override
	protected
	Duration getSleepDuration () {

		return Duration.standardSeconds (
			15);

	}

	@Override
	protected
	String generalErrorSource () {

		return "chat credit daemon";

	}

	@Override
	protected
	String generalErrorSummary () {

		return "error finding users with negative credit";

	}

	@Override
	protected
	String getThreadName () {

		return "ChatCredit";

	}

	// implementation

	private
	void doUserCredit (
			@NonNull Long chatUserId) {

		@Cleanup
		Transaction transaction =
			database.beginReadWrite (
				"ChatCreditDaemon.doUserCredit (chatUserId)",
				this);

		ChatUserRec chatUser =
			chatUserHelper.findRequired (
				chatUserId);

		chatCreditLogic.userBill (
			chatUser,
			false);

		transaction.commit ();

	}

	@Override
	protected
	void runOnce () {

		log.debug (
			"Checking for all users with negative credit");

		@Cleanup
		Transaction transaction =
			database.beginReadOnly (
				"ChatCreditDaemon.runOnce ()",
				this);

		Instant threeMonthsAgo =
			transaction.now ().minus (
				Duration.standardDays (90));

		log.debug (
			stringFormat (
				"Chat billing after %s",
				threeMonthsAgo));

		List <Long> chatUserIds =
			chatUserHelper.findWantingBill (
				threeMonthsAgo)

			.stream ()

			.map (
				ChatUserRec::getId)

			.collect (
				Collectors.toList ());

		transaction.close ();

		log.debug (
			stringFormat (
				"Chat billing after %s",
				chatUserIds.size ()));

		for (
			Long chatUserId
				: chatUserIds
		) {

			doUserCredit (
				chatUserId);

		}

	}

}
