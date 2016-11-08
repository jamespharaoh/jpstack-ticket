package wbs.console.helper.provider;

import wbs.console.context.ConsoleContextStuff;
import wbs.framework.entity.record.Record;

public
interface ConsoleHelperProvider <
	RecordType extends Record <RecordType>
> {

	String objectName ();

	String getPathId (
			Long objectId);

	String getDefaultContextPath (
			RecordType object);

	String localPath (
			RecordType object);

	boolean canView (
			RecordType object);

	RecordType lookupObject (
			ConsoleContextStuff contextStuff);

	Class <RecordType> objectClass ();

	String idKey ();

	void postProcess (
			ConsoleContextStuff contextStuff);

}