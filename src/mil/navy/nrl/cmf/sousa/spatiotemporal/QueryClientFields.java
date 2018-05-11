package mil.navy.nrl.cmf.sousa.spatiotemporal;

public final class QueryClientFields 
{
	/**
	 * A {@link Vector3d}.  It is the client's spatial location
	 * presented as &lt;degrees latitude, degrees longitude, elevation
	 * in meters&gt;.
	 */
    public static final String POSITION_FIELDNAME = "Position";

	/**
	 * A {@link Vector3d}.  It is the width of the spatial search
	 * space presented as <degrees, degrees, meters>.  The width is
	 * always centered around the Position field.
	 */
    public static final String WIDTH_FIELDNAME = "Width";

	/**
	 * A <CODE>Calendar</CODE>.  It is the lower bound of the temporal
	 * search space.
	 */
    public static final String TIMELOWERBOUND_FIELDNAME = "TimeLowerBound";

	/**
	 * A <CODE>Calendar</CODE>.  It is the upper bound of the temporal
	 * search space.
	 */
    public static final String TIMEUPPERBOUND_FIELDNAME = "TimeUpperBound";

	/**
	 * A <CODE>Set</CODE> of <CODE>String</CODE>.  Each
	 * <CODE>String</CODE> names a field that the client wants in the
	 * answers to each spatiotemporal query.
	 */
    public static final String FIELDS_FIELDNAME = "Fields";

	private QueryClientFields() {}
};
