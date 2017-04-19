package wbs.platform.priv.console;

import static wbs.utils.collection.CollectionUtils.listSorted;
import static wbs.utils.collection.IterableUtils.iterableFilter;
import static wbs.utils.collection.IterableUtils.iterableMapToList;
import static wbs.utils.collection.IterableUtils.iterableMapToSet;
import static wbs.utils.etc.OptionalUtils.optionalGetRequired;
import static wbs.utils.etc.OptionalUtils.optionalIsPresent;
import static wbs.utils.etc.OptionalUtils.optionalOf;
import static wbs.web.utils.HtmlTableUtils.htmlTableCellWrite;
import static wbs.web.utils.HtmlTableUtils.htmlTableCellWriteHtml;
import static wbs.web.utils.HtmlTableUtils.htmlTableClose;
import static wbs.web.utils.HtmlTableUtils.htmlTableHeaderRowWrite;
import static wbs.web.utils.HtmlTableUtils.htmlTableOpenList;
import static wbs.web.utils.HtmlTableUtils.htmlTableRowClose;
import static wbs.web.utils.HtmlTableUtils.htmlTableRowOpen;

import java.util.List;
import java.util.Set;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import wbs.console.helper.core.ConsoleHelper;
import wbs.console.helper.manager.ConsoleObjectManager;
import wbs.console.part.AbstractPagePart;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.entity.record.Record;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.TaskLogger;

import wbs.platform.group.model.GroupRec;
import wbs.platform.object.core.console.ObjectTypeConsoleHelper;
import wbs.platform.object.core.model.ObjectTypeRec;
import wbs.platform.priv.model.PrivRec;
import wbs.platform.user.console.UserPrivConsoleHelper;
import wbs.platform.user.model.UserPrivRec;
import wbs.platform.user.model.UserRec;

@Accessors (fluent = true)
@PrototypeComponent ("objectPrivsPart")
public
class ObjectPrivsPart <
	ObjectType extends Record <ObjectType>
>
	extends AbstractPagePart {

	// singleton dependencies

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ConsoleObjectManager objectManager;

	@SingletonDependency
	ObjectTypeConsoleHelper objectTypeHelper;

	@SingletonDependency
	PrivConsoleHelper privHelper;

	@SingletonDependency
	UserPrivConsoleHelper userPrivHelper;

	// properties

	@Getter @Setter
	ConsoleHelper <ObjectType> consoleHelper;

	// state

	ObjectType object;

	Set <String> privCodes;

	List <PrivData> privDatas;

	// implementation

	@Override
	public
	void prepare (
			@NonNull TaskLogger parentTaskLogger) {

		TaskLogger taskLogger =
			logContext.nestTaskLogger (
				parentTaskLogger,
				"prepare");

		object =
			consoleHelper.findFromContextRequired ();

		privCodes =
			iterableMapToSet (
				PrivRec::getCode,
				privHelper.findByParent (
					object));

		privDatas =
			preparePrivDatas (
				taskLogger,
				object);

	}

	private
	List <PrivData> preparePrivDatas (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull Record <?> object) {

		TaskLogger taskLogger =
			logContext.nestTaskLogger (
				parentTaskLogger,
				"preparePrivDatas");

		ImmutableList.Builder <PrivData> objectPrivDatasBuilder =
			ImmutableList.builder ();

		for (

			Optional <Record <?>> currentObjectLoop =
				optionalOf (
					object);

			optionalIsPresent (
				currentObjectLoop);

			currentObjectLoop =
				objectManager.getParent (
					optionalGetRequired (
						currentObjectLoop))

		) {

			Record <?> currentObject =
				optionalGetRequired (
					currentObjectLoop);

			ConsoleHelper <?> currentObjectHelper =
				objectManager.findConsoleHelperRequired (
					currentObject);

			ObjectTypeRec currentObjectType =
				objectTypeHelper.findRequired (
					currentObjectHelper.objectTypeId ());

			objectPrivDatasBuilder.addAll (
				iterableMapToList (
					priv ->
						preparePrivData (
							taskLogger,
							currentObjectType,
							currentObject,
							priv),
					listSorted (
						iterableFilter (
							priv ->
								privCodes.contains (
									priv.getCode ()),
							privHelper.findByParent (
								currentObject)))));

		}

		return objectPrivDatasBuilder.build ();

	}

	private
	PrivData preparePrivData (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull ObjectTypeRec objectType,
			@NonNull Record <?> object,
			@NonNull PrivRec priv) {

		return new PrivData ()

			.objectType (
				objectType)

			.object (
				object)

			.priv (
				priv)

			.users (
				iterableMapToList (
					UserPrivRec::getUser,
					iterableFilter (
						UserPrivRec::getCan,
						userPrivHelper.find (
							priv))))

			.groups (
				listSorted (
					priv.getGroups ()))

		;

	}

	@Override
	public
	void renderHtmlBodyContent (
			@NonNull TaskLogger parentTaskLogger) {

		htmlTableOpenList ();

		htmlTableHeaderRowWrite (
			"Type",
			"Object",
			"Priv",
			"Description",
			"Users",
			"Groups");

		for (
			PrivData privData
				: privDatas
		) {

			htmlTableRowOpen ();

			htmlTableCellWrite (
				privData.objectType.getCode ());

			htmlTableCellWrite (
				objectManager.objectPathMini (
					privData.object ()));

			htmlTableCellWrite (
				privData.priv ().getCode ());

			htmlTableCellWrite (
				privData.priv ().getPrivType ().getDescription ());

			htmlTableCellWriteHtml (
				() -> privData.users ().forEach (
					user -> formatWriter.writeLineFormat (
						"%h.%h<br>",
						user.getSlice ().getCode (),
						user.getUsername ())));

			htmlTableCellWrite (
				() -> privData.groups ().forEach (
					group -> formatWriter.writeLineFormat (
						"%h.%h<br>",
						group.getSlice ().getCode (),
						group.getCode ())));

			htmlTableRowClose ();

		}

		htmlTableClose ();

	}

	// data

	@Accessors (fluent = true)
	@Data
	private static
	class PrivData {

		ObjectTypeRec objectType;
		Record <?> object;

		PrivRec priv;

		List <UserRec> users;
		List <GroupRec> groups;

	}

}
