package wbs.sms.magicnumber.logic;

import static wbs.framework.utils.etc.Misc.stringFormat;

import java.util.Collection;
import java.util.Date;

import javax.inject.Inject;
import javax.inject.Provider;

import lombok.extern.log4j.Log4j;
import wbs.framework.application.annotations.SingletonComponent;
import wbs.platform.affiliate.model.AffiliateRec;
import wbs.platform.lock.logic.LockLogic;
import wbs.platform.service.model.ServiceRec;
import wbs.platform.text.model.TextRec;
import wbs.sms.command.model.CommandRec;
import wbs.sms.magicnumber.model.MagicNumberObjectHelper;
import wbs.sms.magicnumber.model.MagicNumberRec;
import wbs.sms.magicnumber.model.MagicNumberSetRec;
import wbs.sms.magicnumber.model.MagicNumberUseObjectHelper;
import wbs.sms.magicnumber.model.MagicNumberUseRec;
import wbs.sms.message.batch.model.BatchRec;
import wbs.sms.message.core.model.MessageRec;
import wbs.sms.message.outbox.logic.MessageSender;
import wbs.sms.number.core.model.NumberRec;
import wbs.sms.route.core.model.RouteRec;
import wbs.sms.route.router.model.RouterRec;

@Log4j
@SingletonComponent ("magicNumberLogic")
public
class MagicNumberLogic {

	@Inject
	LockLogic coreLogic;

	@Inject
	MagicNumberObjectHelper magicNumberHelper;

	@Inject
	MagicNumberUseObjectHelper magicNumberUseHelper;

	@Inject
	Provider<MessageSender> messageSender;

	/**
	 * Allocate a magic number for a given command from a given magic number set
	 * for the given recipient. Will reuse an existing allocation if possible.
	 *
	 * @param magicNumberSet
	 *            the magic number set to allocate from.
	 * @param number
	 *            the number object representing the recipient.
	 * @param command
	 *            the command to associate with the magic number for this
	 *            recipient.
	 * @param ref
	 *            an integer reference to be passed to the command.
	 * @return the MagicNumber object representing the allocation.
	 */
	public
	MagicNumberRec allocateMagicNumber (
			MagicNumberSetRec magicNumberSet,
			NumberRec number,
			CommandRec command,
			int ref) {

		if (magicNumberSet == null)
			throw new NullPointerException ("magicNumberSet");

		if (number == null)
			throw new NullPointerException ("number");

		if (command == null)
			throw new NullPointerException ("command");

		// create a lock over the magic number set and number

		coreLogic.magicLock (
			magicNumberSet,
			number);

		// lookup an existing use

		MagicNumberUseRec magicNumberUse =
			magicNumberUseHelper.findByRef (
				magicNumberSet,
				number,
				command,
				ref);

		if (magicNumberUse != null) {

			magicNumberUse
				.setLastUseTimestamp (new Date ());

			return magicNumberUse.getMagicNumber ();

		}

		// lookup an unused magic number

		MagicNumberRec magicNumber =
			magicNumberHelper.findUnused (
				magicNumberSet,
				number);

		if (magicNumber != null) {

			magicNumberUse =
				magicNumberUseHelper.insert (
					new MagicNumberUseRec ()
						.setNumber (number)
						.setMagicNumber (magicNumber)
						.setCommand (command)
						.setRefId (ref)
						.setLastUseTimestamp (new Date ()));

			return magicNumber;

		}

		// ok, reallocate the least-recently-used one

		magicNumberUse =
			magicNumberUseHelper.findLeastRecentlyUsed (
				magicNumberSet,
				number);

		if (magicNumberUse == null) {

			log.fatal (
				stringFormat (
					"No magic numbers found for %s",
					magicNumberSet));

			return null;

		}

		magicNumberUse
			.setCommand (command)
			.setRefId (ref)
			.setLastUseTimestamp (new Date ());

		return magicNumberUse.getMagicNumber ();

	}

	/**
	 * Allocates a magic number and sends multiple messages using it.
	 *
	 * @param magicNumberSet
	 *            the magic number set to allocate from.
	 * @param number
	 *            the number object representing the recipient.
	 * @param magicCommand
	 *            the command object to associate with the magic number for this
	 *            recipient.
	 * @param magicRef
	 *            an integer reference to be passed to the command.
	 * @param threadId
	 *            an existing message thread to include the messages sent in.
	 * @param parts
	 *            the message bodies to send.
	 * @param route
	 *            the route to send using.
	 * @param service
	 *            the service to associate messages with.
	 * @param batch
	 *            the batch to include messages in.
	 * @param affiliate
	 *            the affiliate to associate messages with.
	 * @return the threadId of the messages sent.
	 */
	@Deprecated
	public
	Integer sendMessage (
			MagicNumberSetRec magicNumberSet,
			NumberRec number,
			CommandRec magicCommand,
			int magicRef,
			Integer threadId,
			Collection<TextRec> parts,
			RouteRec route,
			ServiceRec service,
			BatchRec batch,
			AffiliateRec affiliate) {

		// allocate a magic number

		MagicNumberRec magicNumber =
			allocateMagicNumber (
				magicNumberSet,
				number,
				magicCommand,
				magicRef);

		// and send parts

		for (TextRec part : parts) {

			MessageRec message =
				messageSender.get ()
					.threadId (threadId)
					.number (number)
					.messageText (part)
					.numFrom (magicNumber.getNumber ())
					.route (route)
					.service (service)
					.batch (batch)
					.affiliate (affiliate)
					.send ();

			if (threadId == null)
				threadId = message.getThreadId ();

		}

		return threadId;

	}

	public
	Integer sendMessage (
			MagicNumberSetRec magicNumberSet,
			NumberRec number,
			CommandRec magicCommand,
			Integer magicRef,
			Integer threadId,
			Collection<TextRec> parts,
			RouterRec router,
			ServiceRec service,
			BatchRec batch,
			AffiliateRec affiliate) {

		// allocate a magic number

		MagicNumberRec magicNumber =
			allocateMagicNumber (
				magicNumberSet,
				number,
				magicCommand,
				magicRef);

		// and send parts

		for (TextRec part : parts) {

			MessageRec message =
				messageSender.get ()

				.threadId (
					threadId)

				.number (
					number)

				.messageText (
					part)

				.numFrom (
					magicNumber.getNumber ())

				.routerResolve (
					router)

				.service (
					service)

				.batch (
					batch)

				.affiliate (
					affiliate)

				.send ();

			if (threadId == null)
				threadId = message.getThreadId ();

		}

		return threadId;

	}

	@Deprecated
	public
	MessageRec sendMessage (
			MagicNumberSetRec magicNumberSet,
			NumberRec number,
			CommandRec magicCommand,
			int magicRef,
			Integer threadId,
			TextRec messageText,
			RouteRec route,
			ServiceRec service,
			BatchRec batch,
			AffiliateRec affiliate) {

		// allocate a magic number

		MagicNumberRec magicNumber =
			allocateMagicNumber (
				magicNumberSet,
				number,
				magicCommand,
				magicRef);

		// and send message

		return messageSender.get ()
			.threadId (threadId)
			.number (number)
			.messageText (messageText)
			.numFrom (magicNumber.getNumber ())
			.route (route)
			.service (service)
			.batch (batch)
			.affiliate (affiliate)
			.send ();

	}

	public
	MessageRec sendMessage (
			MagicNumberSetRec magicNumberSet,
			NumberRec number,
			CommandRec magicCommand,
			int magicRef,
			Integer threadId,
			TextRec messageText,
			RouterRec router,
			ServiceRec service,
			BatchRec batch,
			AffiliateRec affiliate) {

		// allocate a magic number

		MagicNumberRec magicNumber =
			allocateMagicNumber (
				magicNumberSet,
				number,
				magicCommand,
				magicRef);

		// and send message

		return messageSender.get ()
			.threadId (threadId)
			.number (number)
			.messageText (messageText)
			.numFrom (magicNumber.getNumber ())
			.routerResolve (router)
			.service (service)
			.batch (batch)
			.affiliate (affiliate)
			.send ();

	}

}