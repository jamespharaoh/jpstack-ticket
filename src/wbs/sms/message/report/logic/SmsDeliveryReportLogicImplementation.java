package wbs.sms.message.report.logic;

import static wbs.utils.etc.EnumUtils.enumEqualSafe;
import static wbs.utils.etc.EnumUtils.enumInSafe;
import static wbs.utils.etc.EnumUtils.enumName;
import static wbs.utils.etc.EnumUtils.enumNameSpaces;
import static wbs.utils.etc.EnumUtils.enumNotInSafe;
import static wbs.utils.etc.NullUtils.ifNull;
import static wbs.utils.etc.NullUtils.isNotNull;
import static wbs.utils.etc.NumberUtils.integerToDecimalString;
import static wbs.utils.etc.OptionalUtils.optionalOrNull;
import static wbs.utils.etc.NullUtils.isNull;
import static wbs.utils.string.StringUtils.stringFormat;

import com.google.common.base.Optional;

import lombok.NonNull;

import org.joda.time.ReadableInstant;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.Database;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.logging.LogContext;
import wbs.framework.object.ObjectManager;

import wbs.platform.text.model.TextObjectHelper;

import wbs.sms.core.logic.NoSuchMessageException;
import wbs.sms.message.core.logic.InvalidMessageStateException;
import wbs.sms.message.core.logic.SmsMessageLogic;
import wbs.sms.message.core.model.MessageDao;
import wbs.sms.message.core.model.MessageDirection;
import wbs.sms.message.core.model.MessageObjectHelper;
import wbs.sms.message.core.model.MessageRec;
import wbs.sms.message.core.model.MessageStatus;
import wbs.sms.message.report.model.MessageReportObjectHelper;
import wbs.sms.route.core.model.RouteObjectHelper;
import wbs.sms.route.core.model.RouteRec;

@SingletonComponent ("smsDeliveryReportLogic")
public
class SmsDeliveryReportLogicImplementation
	implements SmsDeliveryReportLogic {

	// dependencies

	@SingletonDependency
	Database database;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	MessageDao messageDao;

	@SingletonDependency
	MessageObjectHelper messageHelper;

	@SingletonDependency
	SmsMessageLogic messageLogic;

	@SingletonDependency
	MessageReportObjectHelper messageReportHelper;

	@SingletonDependency
	ObjectManager objectManager;

	@SingletonDependency
	RouteObjectHelper routeHelper;

	@SingletonDependency
	TextObjectHelper textHelper;

	// implementation

	@Override
	public
	void deliveryReport (
			@NonNull Transaction parentTransaction,
			@NonNull MessageRec message,
			@NonNull MessageStatus newMessageStatus,
			@NonNull Optional <String> theirCode,
			@NonNull Optional <String> theirDescription,
			@NonNull Optional <String> extraInformation,
			@NonNull Optional <ReadableInstant> theirTimestamp)
		throws
			NoSuchMessageException,
			InvalidMessageStateException {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"deliveryReport");

		) {

			// check arguments

			if (
				enumNotInSafe (
					newMessageStatus,
					MessageStatus.sent,
					MessageStatus.submitted,
					MessageStatus.undelivered,
					MessageStatus.delivered)
			) {

				throw new IllegalArgumentException (
					stringFormat (
						"Invalid new message status \"%s\" for message %s",
						enumNameSpaces (
							newMessageStatus),
						integerToDecimalString (
							message.getId ())));

			}

			// check delivery reports are enabled

			if (! message.getRoute ().getDeliveryReports ()) {

				throw new RuntimeException (
					stringFormat (
						"Not expecting delivery reports on %s",
						objectManager.objectPath (
							transaction,
							message.getRoute ())));

			}

			// create message report thingy

			messageReportHelper.insert (
				transaction,
				messageReportHelper.createInstance ()

				.setMessage (
					message)

				.setReceivedTime (
					transaction.now ())

				.setNewMessageStatus (
					newMessageStatus)

				.setTheirCode (
					optionalOrNull (
						textHelper.findOrCreate (
							transaction,
							theirCode)))

				.setTheirDescription (
					optionalOrNull (
						textHelper.findOrCreate (
							transaction,
							theirDescription)))

				.setTheirTimestamp (
					optionalOrNull (
						theirTimestamp))

			);

			// update received time if appropriate

			if (

				enumEqualSafe (
					newMessageStatus,
					MessageStatus.delivered)

				&& isNull (
					message.getProcessedTime ())

			) {

				message

					.setProcessedTime (
						transaction.now ());

			}

			// depending on the new and old status, update it

			if (
				isNotNull (
					newMessageStatus != null)
			) {

				switch (message.getStatus ()) {

				case sent:

					if (
						enumInSafe (
							newMessageStatus,
							MessageStatus.sent,
							MessageStatus.submitted,
							MessageStatus.undelivered,
							MessageStatus.delivered)
					) {

						messageLogic.messageStatus (
							transaction,
							message,
							newMessageStatus);

					}

					break;

				case submitted:

					if (
						enumInSafe (
							newMessageStatus,
							MessageStatus.submitted,
							MessageStatus.delivered,
							MessageStatus.undelivered)
					) {

						messageLogic.messageStatus (
							transaction,
							message,
							newMessageStatus);

					}

					break;

				case reportTimedOut:

					if (
						enumInSafe (
							newMessageStatus,
							MessageStatus.delivered,
							MessageStatus.undelivered)
					) {

						messageLogic.messageStatus (
							transaction,
							message,
							newMessageStatus);

					}

					break;

				case undelivered:

					if (
						enumInSafe (
							newMessageStatus,
							MessageStatus.delivered)
					) {

						messageLogic.messageStatus (
							transaction,
							message,
							newMessageStatus);

					}

					break;

				case delivered:
				case manuallyUndelivered:

					break;

				default:

					throw new InvalidMessageStateException (
						stringFormat (
							"Message %s has status \"%s\"",
							integerToDecimalString (
								message.getId ()),
							enumNameSpaces (
								message.getStatus ())));

				}

			}

			// write to log file

			transaction.noticeFormat (
				"DLV %s %s %s %s",
				integerToDecimalString (
					message.getId ()),
				message.getRoute ().getCode (),
				ifNull (
					message.getOtherId (),
					"—"),
				enumName (
					message.getStatus ()));

			// update simulated multipart messages

			message.getMultipartCompanionLinks ().stream ()

				.filter (
					link ->
						link.getSimulated ())

				.forEach (
					link ->
						deliveryReport (
							transaction,
							link.getMessage (),
							newMessageStatus,
							theirCode,
							theirDescription,
							extraInformation,
							theirTimestamp));

		}

	}

	@Override
	public
	MessageRec deliveryReport (
			@NonNull Transaction parentTransaction,
			@NonNull RouteRec route,
			@NonNull String otherId,
			@NonNull MessageStatus newMessageStatus,
			@NonNull Optional <String> theirCode,
			@NonNull Optional <String> theirDescription,
			@NonNull Optional <String> extraInformation,
			@NonNull Optional <ReadableInstant> theirTimestamp)
		throws
			NoSuchMessageException,
			InvalidMessageStateException {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"deliveryReport");

		) {

			// lookup the message

			MessageRec message =
				messageHelper.findByOtherId (
					transaction,
					MessageDirection.out,
					route,
					otherId);

			if (message == null) {

				throw new NoSuchMessageException (
					stringFormat (
						"Delivery report for unrecognised message id %s ",
						otherId,
						"on route %s (%s)",
						route.getCode (),
						integerToDecimalString (
							route.getId ())));

			}

			// process the report

			deliveryReport (
				transaction,
				message,
				newMessageStatus,
				theirCode,
				theirDescription,
				extraInformation,
				theirTimestamp);

			return message;

		}

	}

	@Override
	public
	void deliveryReport (
			@NonNull Transaction parentTransaction,
			@NonNull Long messageId,
			@NonNull MessageStatus newMessageStatus,
			@NonNull Optional <String> theirCode,
			@NonNull Optional <String> theirDescription,
			@NonNull Optional <String> extraInformation,
			@NonNull Optional <ReadableInstant> theirTimestamp)
		throws
			NoSuchMessageException,
			InvalidMessageStateException {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"deliveryReport");

		) {

			// lookup the message

			MessageRec message =
				messageHelper.findOrThrow (
					transaction,
					messageId,
					() -> new NoSuchMessageException (
						stringFormat (
							"Message ID: %s")));

			// process the report

			deliveryReport (
				transaction,
				message,
				newMessageStatus,
				theirCode,
				theirDescription,
				extraInformation,
				theirTimestamp);

		}

	}

}
