package wbs.sms.gazetteer.console;

import static wbs.utils.etc.OptionalUtils.optionalAbsent;
import static wbs.utils.etc.OptionalUtils.optionalGetRequired;
import static wbs.utils.etc.OptionalUtils.optionalIsNotPresent;
import static wbs.utils.etc.OptionalUtils.optionalIsPresent;
import static wbs.utils.etc.OptionalUtils.optionalOf;
import static wbs.utils.etc.ResultUtils.errorResult;
import static wbs.utils.etc.ResultUtils.errorResultFormat;
import static wbs.utils.etc.ResultUtils.successResult;
import static wbs.utils.etc.TypeUtils.genericCastUnchecked;
import static wbs.utils.string.CodeUtils.simplifyToCodeRelaxed;
import static wbs.utils.string.StringUtils.stringFormat;
import static wbs.utils.string.StringUtils.stringIsEmpty;

import java.util.Map;

import com.google.common.base.Optional;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import wbs.console.forms.types.FormFieldInterfaceMapping;
import wbs.console.helper.manager.ConsoleObjectManager;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.NestedTransaction;
import wbs.framework.database.Transaction;
import wbs.framework.logging.LogContext;

import wbs.sms.gazetteer.model.GazetteerEntryRec;
import wbs.sms.gazetteer.model.GazetteerRec;

import fj.data.Either;

@Accessors (fluent = true)
@PrototypeComponent ("gazetteerFormFieldInterfaceMapping")
public
class GazetteerFormFieldInterfaceMapping <Container>
	implements FormFieldInterfaceMapping <
		Container,
		GazetteerEntryRec,
		String
	> {

	// singleton dependencies

	@SingletonDependency
	GazetteerEntryConsoleHelper gazetteerEntryHelper;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ConsoleObjectManager objectManager;

	// properties

	@Getter @Setter
	String gazetteerFieldName;

	// implementation

	@Override
	public
	Either <Optional <GazetteerEntryRec>, String> interfaceToGeneric (
			@NonNull Transaction parentTransaction,
			@NonNull Container container,
			@NonNull Map <String, Object> hints,
			@NonNull Optional <String> interfaceValue) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"interfaceToGeneric");

		) {

			if (

				optionalIsNotPresent (
					interfaceValue)

				|| stringIsEmpty (
					interfaceValue.get ())

			) {

				return successResult (
					optionalAbsent ());

			}

			Optional <GazetteerRec> gazetteerOptional =
				genericCastUnchecked (
					objectManager.dereference (
						transaction,
						container,
						gazetteerFieldName));

			if (
				isNotPresent (
					gazetteerOptional)
			) {

				return errorResultFormat (
					"You must configure a gazetteer first");

			}

			GazetteerRec gazetteer =
				optionalGetRequired (
					gazetteerOptional);

			String entryCode =
				simplifyToCodeRelaxed (
					interfaceValue.get ());

			Optional <GazetteerEntryRec> entryOptional =
				gazetteerEntryHelper.findByCode (
					transaction,
					gazetteer,
					entryCode);

			if (
				optionalIsNotPresent (
					entryOptional)
			) {

				return errorResult (
					stringFormat (
						"Location not found"));

			}

			GazetteerEntryRec entry =
				entryOptional.get ();

			return successResult (
				optionalOf (
					entry));

		}

	}

	private boolean isNotPresent (
			Optional <GazetteerRec> gazetteerOptional) {

		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public
	Either <Optional <String>, String> genericToInterface (
			@NonNull Transaction parentTransaction,
			@NonNull Container container,
			@NonNull Map <String, Object> hints,
			@NonNull Optional <GazetteerEntryRec> genericValue) {

		try (

			NestedTransaction transaction =
				parentTransaction.nestTransaction (
					logContext,
					"genericToInterface");

		) {

			if (
				optionalIsPresent (
					genericValue)
			) {

				return successResult (
					optionalOf (
						genericValue.get ().getName ()));

			} else {

				return successResult (
					optionalAbsent ());

			}

		}

	}

}
