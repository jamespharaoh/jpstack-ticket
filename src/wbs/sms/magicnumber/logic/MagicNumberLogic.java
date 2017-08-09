package wbs.sms.magicnumber.logic;

import java.util.Collection;

import com.google.common.base.Optional;

import wbs.framework.database.Transaction;

import wbs.platform.affiliate.model.AffiliateRec;
import wbs.platform.service.model.ServiceRec;
import wbs.platform.text.model.TextRec;
import wbs.platform.user.model.UserRec;

import wbs.sms.command.model.CommandRec;
import wbs.sms.magicnumber.model.MagicNumberRec;
import wbs.sms.magicnumber.model.MagicNumberSetRec;
import wbs.sms.message.batch.model.BatchRec;
import wbs.sms.message.core.model.MessageRec;
import wbs.sms.number.core.model.NumberRec;
import wbs.sms.route.router.model.RouterRec;

public
interface MagicNumberLogic {

	MagicNumberRec allocateMagicNumber (
			Transaction parentTransaction,
			MagicNumberSetRec magicNumberSet,
			NumberRec number,
			CommandRec command,
			Long ref);

	MessageRec sendMessage (
			Transaction parentTransaction,
			MagicNumberSetRec magicNumberSet,
			NumberRec number,
			CommandRec magicCommand,
			Long magicRef,
			Optional <Long> threadId,
			TextRec messageText,
			RouterRec router,
			ServiceRec service,
			Optional <BatchRec> batch,
			AffiliateRec affiliate,
			Optional <UserRec> user);

	Long sendMessage (
			Transaction parentTransaction,
			MagicNumberSetRec magicNumberSet,
			NumberRec number,
			CommandRec magicCommand,
			Long magicRef,
			Optional <Long> threadId,
			Collection <TextRec> parts,
			RouterRec router,
			ServiceRec service,
			Optional <BatchRec> batch,
			AffiliateRec affiliate,
			Optional <UserRec> user);

}
