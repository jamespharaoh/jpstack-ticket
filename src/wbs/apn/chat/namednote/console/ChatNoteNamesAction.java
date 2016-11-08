package wbs.apn.chat.namednote.console;

import static wbs.utils.etc.Misc.lessThan;
import static wbs.utils.etc.NumberUtils.fromJavaInteger;
import static wbs.utils.etc.NumberUtils.moreThanZero;
import static wbs.utils.etc.NumberUtils.toJavaIntegerRequired;
import static wbs.utils.etc.OptionalUtils.optionalOrNull;
import static wbs.utils.string.StringUtils.stringEqualSafe;
import static wbs.utils.string.StringUtils.stringFormat;
import static wbs.utils.string.StringUtils.stringIsNotEmpty;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;

import lombok.Cleanup;
import lombok.NonNull;

import wbs.apn.chat.core.model.ChatObjectHelper;
import wbs.apn.chat.core.model.ChatRec;
import wbs.apn.chat.namednote.model.ChatNoteNameObjectHelper;
import wbs.apn.chat.namednote.model.ChatNoteNameRec;
import wbs.console.action.ConsoleAction;
import wbs.console.request.ConsoleRequestContext;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.Database;
import wbs.framework.database.Transaction;
import wbs.framework.logging.TaskLogger;
import wbs.framework.web.Responder;

@PrototypeComponent ("chatNoteNamesAction")
public
class ChatNoteNamesAction
	extends ConsoleAction {

	// singleton dependencies

	@SingletonDependency
	Database database;

	@SingletonDependency
	ChatObjectHelper chatHelper;

	@SingletonDependency
	ChatNoteNameObjectHelper chatNoteNameHelper;

	@SingletonDependency
	ConsoleRequestContext requestContext;

	// details

	@Override
	public
	Responder backupResponder () {
		return responder ("chatNoteNamesResponder");
	}

	// implementation

	@Override
	protected
	Responder goReal (
			@NonNull TaskLogger taskLogger)
		throws ServletException {

		List<String> notices =
			new ArrayList<String> ();

		// setup transaction etc

		@Cleanup
		Transaction transaction =
			database.beginReadWrite (
				"ChatNoteNamesAction.goReal ()",
				this);

		ChatRec chat =
			chatHelper.findRequired (
				requestContext.stuffInteger (
					"chatId"));

		List <ChatNoteNameRec> chatNoteNames =
			chatNoteNameHelper.findNotDeleted (
				chat);

		if (
			requestContext.formIsPresent (
				"saveChanges")
		) {

			// update note names

			int numUpdated = 0;

			for (
				ChatNoteNameRec noteName
					: chatNoteNames
			) {

				String formKey =
					stringFormat (
						"noteName%d",
						noteName.getId ());

				String newName =
					optionalOrNull (
						requestContext.form (
							formKey));

				if (newName == null)
					continue;

				if (
					stringEqualSafe (
						newName,
						noteName.getName ())
				) {
					continue;
				}

				noteName

					.setName (
						newName);

				numUpdated ++;

			}

			if (numUpdated == 1)
				notices.add ("Note name updated");

			if (numUpdated > 1) {

				notices.add (
					stringFormat (
						"%d note names updated",
						numUpdated));

			}

			// add new note

			if (
				stringIsNotEmpty (
					requestContext.formOrEmptyString (
						"noteNameNew"))
			) {

				chatNoteNameHelper.insert (
					chatNoteNameHelper.createInstance ()

					.setChat (
						chat)

					.setIndex (
						fromJavaInteger (
							chatNoteNames.size ()))

					.setName (
						requestContext.formRequired (
							"noteNameNew"))

					.setDescription (
						"")

					.setDeleted (
						false)

				);

				notices.add ("New name added");

				requestContext.setEmptyFormData ();

			}

		} else for (ChatNoteNameRec chatNoteName
				: chatNoteNames) {

			String noteMoveUpKey =
				stringFormat (
					"noteMoveUp%d",
					chatNoteName.getId ());

			if (

				requestContext.formIsPresent (
					noteMoveUpKey)

				&& moreThanZero (
					chatNoteName.getIndex ())

			) {

				long index =
					chatNoteName.getIndex ();

				ChatNoteNameRec otherNote =
					chatNoteNames.get (
						toJavaIntegerRequired (
							index - 1));

				// first set to non-conflicting values

				chatNoteName.setIndex (
					-1l);

				otherNote.setIndex (
					-2l);

				transaction.flush ();

				// then to new values

				chatNoteName.setIndex (
					index - 1);

				otherNote.setIndex (
					index);

				// done

				notices.add (
					"Note moved up");

				break;

			}

			String noteMoveDownKey =
				stringFormat (
					"noteMoveDown%d",
					chatNoteName.getId ());

			if (

				requestContext.formIsPresent (
					noteMoveDownKey)

				&& lessThan (
					chatNoteName.getIndex (),
					chatNoteNames.size () - 1)

			) {

				long index =
					chatNoteName.getIndex ();

				ChatNoteNameRec otherNote =
					chatNoteNames.get (
						toJavaIntegerRequired (
							index + 1));

				chatNoteName.setIndex (
					-1l);

				otherNote.setIndex (
					-2l);

				transaction.flush ();

				chatNoteName.setIndex (
					index + 1);

				otherNote.setIndex (
					index);

				notices.add (
					"Note moved down");

				break;

			}

			String noteDeleteKey =
				stringFormat (
					"noteDelete%d",
					chatNoteName.getId ());

			if (
				requestContext.formIsPresent (
					noteDeleteKey)
			) {

				long index =
					chatNoteName.getIndex ();

				chatNoteName.setIndex (
					null);

				chatNoteName.setDeleted (
					true);

				transaction.flush ();

				for (
					long otherIndex = index;
					otherIndex < chatNoteNames.size () - 1;
					otherIndex ++
				) {

					ChatNoteNameRec otherChatNoteName =
						chatNoteNames.get (
							toJavaIntegerRequired (
								otherIndex + 1));

					otherChatNoteName.setIndex (
						otherIndex + 1);

				}

				notices.add (
					"Note deleted");

				break;

			}

		}

		// commit and finish up

		transaction.commit ();

		requestContext.addNotices (notices);

		return null;

	}

}