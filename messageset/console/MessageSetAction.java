package wbs.sms.messageset.console;

import static wbs.utils.etc.LogicUtils.referenceNotEqualWithClass;
import static wbs.utils.etc.NullUtils.isNull;
import static wbs.utils.etc.NumberUtils.integerToDecimalString;
import static wbs.utils.etc.NumberUtils.toJavaIntegerRequired;
import static wbs.utils.etc.OptionalUtils.optionalIsNotPresent;
import static wbs.utils.etc.OptionalUtils.optionalIsPresent;
import static wbs.utils.string.StringUtils.stringIsEmpty;
import static wbs.utils.string.StringUtils.stringNotEqualSafe;

import java.util.regex.Pattern;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import wbs.console.action.ConsoleAction;
import wbs.console.lookup.BooleanLookup;
import wbs.console.module.ConsoleManager;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.component.manager.ComponentProvider;
import wbs.framework.database.Database;
import wbs.framework.database.OwnedTransaction;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.OwnedTaskLogger;
import wbs.framework.logging.TaskLogger;

import wbs.platform.event.logic.EventLogic;
import wbs.platform.user.console.UserConsoleLogic;

import wbs.sms.gsm.GsmUtils;
import wbs.sms.message.core.model.MessageRec;
import wbs.sms.messageset.model.MessageSetMessageObjectHelper;
import wbs.sms.messageset.model.MessageSetMessageRec;
import wbs.sms.messageset.model.MessageSetRec;
import wbs.sms.route.core.model.RouteObjectHelper;
import wbs.sms.route.core.model.RouteRec;

import wbs.web.responder.WebResponder;

@Accessors (fluent = true)
@PrototypeComponent ("messageSetAction")
public
class MessageSetAction
	extends ConsoleAction {

	// singleton dependencies

	@SingletonDependency
	ConsoleManager consoleManager;

	@SingletonDependency
	Database database;

	@SingletonDependency
	EventLogic eventLogic;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	MessageSetMessageObjectHelper messageSetMessageHelper;

	@SingletonDependency
	RouteObjectHelper routeHelper;

	@SingletonDependency
	UserConsoleLogic userConsoleLogic;

	// properties

	@Getter @Setter
	MessageSetFinder messageSetFinder;

	@Getter @Setter
	BooleanLookup privLookup;

	@Getter @Setter
	ComponentProvider <WebResponder> responder;

	// details

	@Override
	public
	WebResponder backupResponder (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTaskLogger taskLogger =
				logContext.nestTaskLogger (
					parentTaskLogger,
					"backupResponder");

		) {

			return responder.provide (
				taskLogger);

		}

	}

	@Override
	public
	WebResponder goReal (
			@NonNull TaskLogger parentTaskLogger) {

		try (

			OwnedTransaction transaction =
				database.beginReadWrite (
					logContext,
					parentTaskLogger,
					"goReal");

		) {

			// check privs

			if (
				! privLookup.lookup (
					requestContext.consoleContextStuffRequired ())
			) {

				requestContext.addError (
					"Access denied");

				return null;

			}

			// lookup the message set

			MessageSetRec messageSet =
				messageSetFinder.findMessageSet (
					transaction,
					requestContext);

			// iterate over the input and check them

			long numMessages =
				requestContext.parameterIntegerRequired (
					"num_messages");

			for (
				long index = 0;
				index < numMessages;
				index ++
			) {

				if (
					optionalIsNotPresent (
						requestContext.parameter (
							"enabled_" + index))
				) {
					continue;
				}

				if (
					! Pattern.matches (
						"\\d+",
						requestContext.parameterRequired (
							"route_" + index))
				) {

					requestContext.addErrorFormat (
						"Message %s has no route",
						integerToDecimalString (
							index + 1));

					return null;

				}

				if (
					stringIsEmpty (
						requestContext.parameterRequired (
							"number_" + index))
				) {

					requestContext.addErrorFormat (
						"Message %s has no number",
						integerToDecimalString (
							index + 1));

					return null;

				}

				String message =
					requestContext.parameterRequired (
						"message_" + index);

				if (! GsmUtils.gsmStringIsValid (message)) {

					requestContext.addErrorFormat (
						"Message %s has invalid characters",
						integerToDecimalString (
							index + 1));

					return null;

				}

				if (GsmUtils.gsmStringLength (message) > 160) {

					requestContext.addError (
						"Message " + (index + 1) + " is too long");

					return null;

				}

			}

			// iterate over the input and do it

			for (
				long index = 0;
				index < numMessages;
				index ++
			) {

				boolean enabled =
					optionalIsPresent (
						requestContext.parameter (
							"enabled_" + index));

				MessageSetMessageRec messageSetMessage =
					index < messageSet.getMessages ().size ()
						? messageSet.getMessages ().get (
							toJavaIntegerRequired (
								index))
						: null;

				if (messageSetMessage != null && ! enabled) {

					// delete existing message

					messageSetMessageHelper.remove (
						transaction,
						messageSetMessage);

	//				messageSet.getMessages ().remove (
	//					new Integer (index));

					eventLogic.createEvent (
						transaction,
						"messageset_message_removed",
						userConsoleLogic.userRequired (
							transaction),
						index,
						messageSet);

				} else if (enabled) {

					// set up some handy variables

					RouteRec newRoute =
						routeHelper.findRequired (
							transaction,
							requestContext.parameterIntegerRequired (
								"route_" + index));

					String newNumber =
						requestContext.parameterRequired (
							"number_" + index);

					String newMessage =
						requestContext.parameterRequired (
							"message_" + index);

					if (
						isNull (
							messageSetMessage)
					) {

						// create new message

						messageSetMessage =
							messageSetMessageHelper.createInstance ()

							.setMessageSet (
								messageSet)

							.setIndex (
								index)

							.setRoute (
								newRoute)

							.setNumber (
								newNumber)

							.setMessage (
								newMessage);

						messageSetMessageHelper.insert (
							transaction,
							messageSetMessage);

						messageSet.getMessages ().add (
							messageSetMessage);

						// and create event

						eventLogic.createEvent (
							transaction,
							"messageset_message_created",
							userConsoleLogic.userRequired (
								transaction),
							index,
							messageSet,
							newRoute,
							newNumber,
							0,
							newMessage);

					} else {

						// update existing message

						if (
							referenceNotEqualWithClass (
								RouteRec.class,
								messageSetMessage.getRoute (),
								newRoute)
						) {

							messageSetMessage

								.setRoute (
									newRoute);

							eventLogic.createEvent (
								transaction,
								"messageset_message_route",
								userConsoleLogic.userRequired (
									transaction),
								index,
								messageSet,
								newRoute);

						}

						if (
							stringNotEqualSafe (
								messageSetMessage.getNumber (),
								newNumber)
						) {

							messageSetMessage

								.setNumber (
									newNumber);

							eventLogic.createEvent (
								transaction,
								"messageset_message_number",
								userConsoleLogic.userRequired (
									transaction),
								index,
								messageSet,
								newNumber);

						}

						if (
							referenceNotEqualWithClass (
								MessageRec.class,
								messageSetMessage.getMessage (),
								newMessage)
						) {

							messageSetMessage

								.setMessage (
									newMessage);

							eventLogic.createEvent (
								transaction,
								"messageset_message_message",
								userConsoleLogic.userRequired (
									transaction),
								index,
								messageSet,
								newMessage);

						}

					}

				}

			}

			transaction.commit ();

			requestContext.addNotice (
				"Messages updated");

			requestContext.setEmptyFormData ();

			return null;

		}

	}

}
