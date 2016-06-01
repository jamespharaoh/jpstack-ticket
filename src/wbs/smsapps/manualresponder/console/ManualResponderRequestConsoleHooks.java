package wbs.smsapps.manualresponder.console;

import javax.inject.Inject;

import lombok.NonNull;

import com.google.common.collect.ImmutableList;

import wbs.console.helper.ConsoleHooks;
import wbs.console.priv.UserPrivChecker;
import wbs.framework.application.annotations.SingletonComponent;
import wbs.framework.object.ObjectManager;
import wbs.platform.user.console.UserConsoleHelper;
import wbs.platform.user.model.UserRec;
import wbs.smsapps.manualresponder.model.ManualResponderRec;
import wbs.smsapps.manualresponder.model.ManualResponderRequestRec;
import wbs.smsapps.manualresponder.model.ManualResponderRequestSearch;

@SingletonComponent ("manualResponderRequestConsoleHooks")
public
class ManualResponderRequestConsoleHooks
	implements ConsoleHooks<ManualResponderRequestRec> {

	// dependencies

	@Inject
	ManualResponderConsoleHelper manualResponderHelper;

	@Inject
	ObjectManager objectManager;

	@Inject
	UserPrivChecker privChecker;

	@Inject
	UserConsoleHelper userHelper;

	// implementation

	@Override
	public
	void applySearchFilter (
			@NonNull Object searchObject) {

		ManualResponderRequestSearch search =
			(ManualResponderRequestSearch)
			searchObject;

		search

			.filter (
				true);

		// manual responders

		ImmutableList.Builder<Integer> manualRespondersBuilder =
			ImmutableList.<Integer>builder ();

		for (
			ManualResponderRec manualResponder
				: manualResponderHelper.findAll ()
		) {

			 if (
			 	! privChecker.canRecursive (
			 		manualResponder,
			 		"supervisor")
			 ) {
			 	continue;
			 }

			manualRespondersBuilder.add (
				manualResponder.getId ());

		}

		// users

		ImmutableList.Builder<Integer> usersBuilder =
			ImmutableList.<Integer>builder ();

		for (
			UserRec user
				: userHelper.findAll ()
		) {

			 if (
			 	! privChecker.canRecursive (
			 		user,
			 		"supervisor")
			 ) {
			 	continue;
			 }

			usersBuilder.add (
				user.getId ());

		}

		search

			.filterManualResponderIds (
				manualRespondersBuilder.build ())

			.filterProcessedByUserIds (
				usersBuilder.build ());

	}

}