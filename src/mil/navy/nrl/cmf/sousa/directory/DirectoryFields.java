package mil.navy.nrl.cmf.sousa.directory;

public class DirectoryFields
{

	/**
	 * This {@link ConsumerViewInterpreter} input from authoritative
	 * State is a Map with {@link
	 * mil.navy.nrl.cmf.sousa.ServerContact} as keys and String as
	 * values.
	 */
	public static final String DIRECTORY_FIELDNAME = "Directory";

	/**
	 * This part of {@link ConsumerViewInterpreter} output indicates
	 * the entries added to the directory since the last update.  It's
	 * a Map with {@link mil.navy.nrl.cmf.sousa.ServerContact} as keys
	 * and String descriptions as values.
	 */
	public static final String ADDED = "Added";

	/**
	 * This part of {@link ConsumerViewInterpreter} output indicates
	 * the entries deleted from the directory since the last update.  It's a
	 * Map with {@link mil.navy.nrl.cmf.sousa.ServerContact} as keys
	 * and String descriptions as values.
	 */
	public static final String REMOVED = "Removed";

	private DirectoryFields() {}
};
