package wbs.platform.group.console;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import lombok.Cleanup;
import wbs.framework.application.annotations.PrototypeComponent;
import wbs.framework.database.Database;
import wbs.framework.database.Transaction;
import wbs.framework.web.Responder;
import wbs.platform.console.action.ConsoleAction;
import wbs.platform.console.request.ConsoleRequestContext;
import wbs.platform.event.logic.EventLogic;
import wbs.platform.group.model.GroupRec;
import wbs.platform.priv.console.PrivChecker;
import wbs.platform.priv.console.PrivConsoleHelper;
import wbs.platform.priv.model.PrivRec;
import wbs.platform.updatelog.logic.UpdateManager;
import wbs.platform.user.model.UserObjectHelper;
import wbs.platform.user.model.UserRec;

@PrototypeComponent ("groupPrivsAction")
public
class GroupPrivsAction
	extends ConsoleAction {

	@Inject
	ConsoleRequestContext requestContext;

	@Inject
	Database database;

	@Inject
	EventLogic eventLogic;

	@Inject
	GroupConsoleHelper groupHelper;

	@Inject
	PrivChecker privChecker;

	@Inject
	PrivConsoleHelper privHelper;

	@Inject
	UpdateManager updateManager;

	@Inject
	UserObjectHelper userHelper;

	@Override
	public
	Responder backupResponder () {
		return responder ("groupPrivsResponder");
	}

	@Override
	public
	Responder goReal () {

		int numRevoked = 0;
		int numGranted = 0;

		@Cleanup
		Transaction transaction =
			database.beginReadWrite ();

		UserRec myUser =
			userHelper.find (
				requestContext.userId ());

		GroupRec group =
			groupHelper.find (
				requestContext.stuffInt ("groupId"));

		Matcher matcher =
			privDataPattern.matcher (
				requestContext.parameter ("privdata"));

		while (matcher.find ()) {

			int privId =
				Integer.parseInt (matcher.group (1));

			boolean newCan =
				matcher.group (2).equals ("1");

			PrivRec priv =
				privHelper.find (privId);

			boolean oldCan =
				group.getPrivs ().contains (priv);

			// check we have permission to update this priv

			if (! privChecker.canGrant (priv.getId ()))
				continue;

			if (!oldCan && newCan) {

				group.getPrivs().add(priv);
				eventLogic.createEvent("group_grant", myUser, priv,
						group);
				numGranted++;

			} else if (oldCan && !newCan) {

				group.getPrivs().remove(priv);
				eventLogic.createEvent("group_revoke", myUser, priv,
						group);
				numRevoked++;
			}
		}

		for (UserRec user
				: group.getUsers ()) {

			updateManager.signalUpdate (
				"user_privs",
				user.getId ());

		}

		transaction.commit();

		if (numGranted > 0)
			requestContext.addNotice ("" + numGranted + " privileges granted");

		if (numRevoked > 0)
			requestContext.addNotice ("" + numRevoked + " privileges revoked");

		return null;

	}

	private final static
	Pattern privDataPattern =
		Pattern.compile ("(\\d+)-can=(0|1)");

}