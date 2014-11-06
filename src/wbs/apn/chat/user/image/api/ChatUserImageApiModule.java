package wbs.apn.chat.user.image.api;

import java.util.Map;
import java.util.regex.Matcher;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Provider;

import wbs.framework.application.annotations.SingletonComponent;
import wbs.framework.web.PathHandler;
import wbs.framework.web.RegexpPathHandler;
import wbs.framework.web.RequestContext;
import wbs.framework.web.ServletModule;
import wbs.framework.web.WebFile;
import wbs.platform.api.ApiFile;

import com.google.common.collect.ImmutableMap;

@SingletonComponent ("chatUserImageApiModule")
public
class ChatUserImageApiModule
	implements ServletModule {

	// dependencies

	@Inject
	RequestContext requestContext;

	@Inject
	Provider<ApiFile> apiFile;

	// state

	WebFile imageUploadFile;
	RegexpPathHandler.Entry imageUploadEntry;

	// implementation

	@PostConstruct
	public
	void init () {

		imageUploadFile =
			apiFile.get ()

			.getActionName (
				"chatUserImageUploadViewAction")

			.postActionName (
				"chatUserImageUploadPostAction");

		imageUploadEntry =
			new RegexpPathHandler.Entry (
				"/([a-z]+)") {

			@Override
			protected
			WebFile handle (
					Matcher matcher) {

				requestContext.request (
					"chatUserImageUploadToken",
					matcher.group (1));

				return imageUploadFile;

			}

		};

	}

	@Override
	public
	Map<String,PathHandler> paths () {

		return ImmutableMap.<String,PathHandler>builder ()

			.put (
				"/chat/imageUpload",
				new RegexpPathHandler (
					imageUploadEntry))

			.build ();

	}

	@Override
	public
	Map<String,WebFile> files () {

		return ImmutableMap.<String,WebFile>builder ()

			.build ();

	}

}