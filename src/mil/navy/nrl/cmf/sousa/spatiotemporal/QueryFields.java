package mil.navy.nrl.cmf.sousa.spatiotemporal;

public final class QueryFields 
{
	/**
	   A {@link Queryable}; 
	   <@link ViewInterpreter} input from authoritative State.
	 */
    public static final String QUERYABLE_FIELDNAME = "Queryable";

	/**
	   A <CODE>String</CODE> that describes the information the {@link
	   QueryViewInterpreter} expects to return to the client.  It's
	   for a server's entry in a directory.  Each service includes its
	   description in its advertisement.
	   <CODE>QueryViewInterpreter</CODE> output.
	 */
    public static final String DESCRIPTION_FIELDNAME = "Description";

	/**
	   A <CODE>Set of {@link QueryResultHandle} that were added to the
	   visible set as a result of the query.
	   <CODE>QueryViewInterpreter</CODE> output.
	 */
    public static final String RESULTS_ADDED = "ResultsAdded";

	/**
	   A <CODE>Set of {@link QueryResultHandle} that were removed from
	   the visible set as a result of the query.
	   <CODE>QueryViewInterpreter</CODE> output.
	 */
    public static final String RESULTS_REMOVED = "ResultsRemoved";

	/**
	   A <CODE>Set of {@link QueryResultHandle} that are still in
	   the visible set but with different values.
	   <CODE>QueryViewInterpreter</CODE> output.
	 */
    public static final String RESULTS_CHANGED = "ResultsChanged";

	private QueryFields() {}
}
