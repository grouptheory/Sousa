// File: QueryViewInterpreter.java

package mil.navy.nrl.cmf.sousa.spatiotemporal;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mil.navy.nrl.cmf.sousa.ViewInterpreter;
import mil.navy.nrl.cmf.sousa.Field;
import mil.navy.nrl.cmf.sousa.State;
import mil.navy.nrl.cmf.sousa.util.StackTrace;
import mil.navy.nrl.cmf.sousa.util.Strings;

import org.apache.log4j.Logger;


/**
   QueryViewInterpreter is interested in the client's position and
   time as supplied by the query parameters.  When the client moves
   signficantly or changes its time significantly,
   QueryViewInterpreter asks the authoritative State's Queryable to
   tell it what is no longer visible and what is newly visible.
   <P>
   Inputs from State: {@link QueryFields#QUERYABLE_FIELDNAME}
   <P>
   Query parameters (Inputs from client):
   {@link QueryClientFields#POSITION_FIELDNAME},
   {@link QueryClientFields#WIDTH_FIELDNAME},
   {@link QueryClientFields#TIMELOWERBOUND_FIELDNAME},
   {@link QueryClientFields#TIMEUPPERBOUND_FIELDNAME},
   {@link QueryClientFields#FIELDS_FIELDNAME}
   <P>
   Outputs to client:
   {@link QueryFields#RESULTS_ADDED}, {@link QueryFields#RESULTS_REMOVED}, 
   {@link QueryFields#RESULTS_CHANGED}
*/
public class QueryViewInterpreter
	extends  ViewInterpreter 
{
    protected static final Logger _LOG = Logger.getLogger(QueryViewInterpreter.class);

	/** The spatial location of the client */
	private static final String POSITION_FIELDNAME = 
		ViewInterpreter.getQualifiedFieldName(SpatiotemporalViewInterpreter.class, 
											  QueryClientFields.POSITION_FIELDNAME);

	/** The width of the spatial search */
	private static final String WIDTH_FIELDNAME = 
		ViewInterpreter.getQualifiedFieldName(SpatiotemporalViewInterpreter.class, 
											  QueryClientFields.WIDTH_FIELDNAME);

	/** Lower bound of the temporal search */
	private static final String TIMELOWERBOUND_FIELDNAME = 
		ViewInterpreter.getQualifiedFieldName(SpatiotemporalViewInterpreter.class, 
											  QueryClientFields.TIMELOWERBOUND_FIELDNAME);

	/** Upper bound of the temporal search */
	private static final String TIMEUPPERBOUND_FIELDNAME = 
		ViewInterpreter.getQualifiedFieldName(SpatiotemporalViewInterpreter.class, 
											  QueryClientFields.TIMEUPPERBOUND_FIELDNAME);

	/** Fields that the client wants in each {@link QueryResultHandle} */
	private static final String FIELDS_FIELDNAME = 
		ViewInterpreter.getQualifiedFieldName(SpatiotemporalViewInterpreter.class, 
											  QueryClientFields.FIELDS_FIELDNAME);
		
	/** Copy of the client's current position */
    private Vector3d _currentPosition = null;

	/** Copy of the search width */
    private Vector3d _currentWidth = null;

	/** Copy of the temporal lower bound */
    private Calendar _currentLBTime = null;

	/** Copy of the temporal upper bound */
    private Calendar _currentUBTime = null;

    /** _factory performs the spatiotemporal query. */
    private Queryable _factory = null;

	/**
	   Answers returned by previous call to
	   <CODE>_factory.query</CODE>.
	*/
    private Set _previousResults = new HashSet();

	/**
	  Query context returned by <CODE>_factory.query</CODE>.  Don't
	  change its contents.
	*/
    private Map _context = new HashMap();

	/**
	   Copy of the names of the fields that the client wants in each
	   {@link QueryResultHandle}.
	*/
    private final Set _fieldNames = new HashSet();

    // ViewInterpreter

    public QueryViewInterpreter(State s) 
    {
		super(s);

		HashSet fields = new HashSet();

		// These are the Fields of s that interest us.
		fields.add(QueryFields.QUERYABLE_FIELDNAME);

		// Tell s to notify us whenver one of the Fields changes.
		s.attachFieldListener(fields, this);
		
		//	_currentLBTime = Calendar.getInstance();
		//	_currentUBTime = (Calendar)_currentLBTime.clone();
    }

	/**
	   This QueryViewInterpreter is dirty if any of its parameters
	   have changed.
	   <P>

	   <EM>TODO: Think of a way to to teach QueryViewInterpreter the
	   significant parameters.  For instance, it might be OK to
	   ignore time changes.</EM>

	 */
	public boolean isDirty(State parameters){
		boolean answer = isDirty();
		Set dirtyParams = getDirtyParameterFields();

		// DAVID: Think of a way to to teach QueryViewInterpreter the
		// significant parameters.  For instance, it might be OK to
		// ignore time changes.
		if (dirtyParams.contains(POSITION_FIELDNAME) ||
			dirtyParams.contains(WIDTH_FIELDNAME) ||
			dirtyParams.contains(TIMELOWERBOUND_FIELDNAME) ||
			dirtyParams.contains(TIMEUPPERBOUND_FIELDNAME) ||
			dirtyParams.contains(FIELDS_FIELDNAME))
			answer |= true;

		return answer;
	}

	/**
	   Ensures that {@link QueryFields#RESULTS_ADDED}, {@link
	   QueryFields#RESULTS_REMOVED}, and {@link
	   QueryFields#RESULTS_CHANGED} are present in the
	   State.ChangeMessage.
	 */
	protected State.ChangeMessage getCurrentFieldValues(State parameters) {
		//
		// super.getCurrentFieldValues() invokes interpret().
		// this.getCurrentFieldValues() must Fill in the Fields that
		// interpret() doesn't provide.
		//
		State.ChangeMessage changes = super.getCurrentFieldValues(parameters);

		// changes could be null
		if (changes == null) return null;

		boolean needAdded = true;
		boolean needRemoved = true;
		boolean needChanged = true;
		List fieldChangeMessages = changes.getMessages();
		
		for (Iterator i = fieldChangeMessages.iterator(); i.hasNext(); ) {
			Field.ChangeMessage fieldChange = (Field.ChangeMessage)i.next();
			if (fieldChange._fname.equals(QueryFields.RESULTS_ADDED)) {
				needAdded = false;
			}
			
			if (fieldChange._fname.equals(QueryFields.RESULTS_REMOVED)) {
				needRemoved = false;
			}

			if (fieldChange._fname.equals(QueryFields.RESULTS_CHANGED)) {
				needChanged = false;
			}
		}

		if (needAdded) {
			Field.ChangeMessage added = new Field.ChangeMessage(QueryFields.RESULTS_ADDED, 
																(Serializable)new HashSet());
			changes.addFieldChangeMessage(added);

		}

		if (needRemoved) {
			Field.ChangeMessage removed = new Field.ChangeMessage(QueryFields.RESULTS_REMOVED, 
																  (Serializable)new HashSet());
			changes.addFieldChangeMessage(removed);
		}

		if (needChanged) {
			Field.ChangeMessage changed = new Field.ChangeMessage(QueryFields.RESULTS_CHANGED, 
																  (Serializable)new HashSet());
			changes.addFieldChangeMessage(changed);
		}

		return changes;
	}

	/**
	  Performs a spatiotemporal query using the authoritative State's
	  {@link Queryable} {@link QueryFields#QUERYABLE_FIELDNAME}.
	  Inputs to the query come from <CODE>parameters</CODE>.

	  @param fieldChanges the changed fields in the Entity's authoritative State
	  @param parameters inputs to <CODE>query</CODE>
	 */
    protected State.ChangeMessage interpret(List fieldChanges,
											State parameters)
    {
		// TODO: Distinguish the Fields that may be members of
		// fieldChanges from Fields that may be members of
		// parameters.

		State.ChangeMessage answer = new State.ChangeMessage();
		boolean mustRestore = false;
		// Watch for changes in the POSITION, WIDTH, and FIELDS fields.
		// If any of them change, rerun the query, add and remove
		// from RESULTS.

		//
		// Collect all of the changes from the authoritative State.
		//
		if (null != fieldChanges) {
			for (Iterator it = fieldChanges.iterator(); it.hasNext(); ) {
				Field.ChangeMessage fcm = (Field.ChangeMessage)it.next();

				if (fcm._fname.equals(QueryFields.QUERYABLE_FIELDNAME)) {
					//
					// Server offers a different StateFactory.
					// 
					_factory = (Queryable)fcm._value;
					mustRestore = true;
				} else {
					_LOG.error(new Strings(new Object[] {
						this, 
						" interpret(): unknown authoritative State Field [",
						fcm._fname, " = " , fcm._value}));
				}
			}
		}

		//
		// Collect all of the changes from the Receptor State.
		//
		Set changedParams = getDirtyParameterFields();
		if (changedParams.size() > 0) {
			for (Iterator it = changedParams.iterator(); it.hasNext(); ) {
				String fieldName = (String)it.next();

				// DAVID: Think carefully about setting the logging level to
				// DEBUG because a ChangeMessage can contain a lot
				// of information.  You might not want to print that much
				// information.

				_LOG.debug(new Strings(new Object[] 
					{this, " interpret() ", fieldName}));

				if (fieldName.equals(POSITION_FIELDNAME)) {
					// Client wants a snapshot from a different place.
					//
					try {
						_currentPosition = (Vector3d)parameters.getField(fieldName);
						mustRestore = true;
					} catch (NoSuchFieldException ex) {
						_LOG.error(new Strings(new Object[]
							{this, " interpret(): Can't find ",
							 fieldName, " in State ", parameters}));
					}
				} else if (fieldName.equals(WIDTH_FIELDNAME)) {
					// Client wants a snapshot of a different width.
					//
					try {
						_currentWidth = (Vector3d)parameters.getField(fieldName);
						mustRestore = true;
					} catch (NoSuchFieldException ex) {
						_LOG.error(new Strings(new Object[]
							{this, " interpret(): Can't find ",
							 fieldName, " in State ", parameters}));
					}
				} else if (fieldName.equals(TIMELOWERBOUND_FIELDNAME)) {
					try {
						_currentLBTime = (Calendar)parameters.getField(fieldName);
						mustRestore = true;
					} catch (NoSuchFieldException ex) {
						_LOG.error(new Strings(new Object[]
							{this, " interpret(): Can't find ",
							 fieldName, " in State ", parameters}));
					}
				} else if (fieldName.equals(TIMEUPPERBOUND_FIELDNAME)) {
					try {
						_currentUBTime = (Calendar)parameters.getField(fieldName);
						mustRestore = true;
					} catch (NoSuchFieldException ex) {
						_LOG.error(new Strings(new Object[]
							{this, " interpret(): Can't find ",
							 fieldName, " in State ", parameters}));
					}
				} else if (fieldName.equals(FIELDS_FIELDNAME)) {
					//
					// Adjust the fields of interest.  fcm must be a
					// CollectionFieldValue.CollectionChangeMessageImpl.
					//
					try {
						Set newFields = (Set)parameters.getField(fieldName);
						_fieldNames.clear();
						_fieldNames.addAll(newFields);

						// DAVID: Must I do this?  Not sure why I wouldn't.
						// See original code, please.
						mustRestore = true;
					} catch (NoSuchFieldException ex) {
						_LOG.error(new Strings(new Object[]
							{this, " interpret(): Can't find ",
							 fieldName, " in State ", parameters}));
					}
				} else {
					_LOG.error(new Strings(new Object[] {
						this, " interpret(): unexpected Receptor State Field: ",
						fieldName}));
				}
			}
		}

		if (!mustRestore) {
			_LOG.debug(new Strings(new Object[] {
									   this, " interpret(): mustRestore was false"} ));
		}

		if (_factory == null) {
			_LOG.debug(new Strings(new Object[] {
									   this, " interpret(): _factory was NULL"} ));
		}

		if (mustRestore) {

			if (_factory != null) {
				/*
				  _LOG.info(new Strings(new Object[] {
				  this, 
				  " Restoring for location= ", _currentPosition,
				  ", width= ", _currentWidth,
				  ", lbTime= ", _currentLBTime.getTime(),
				  ", ubTime= ", _currentUBTime.getTime()
				  }));
				*/

				// QueryResultHandles that are newly in view as the result of the
				// current spatiotemporal query.
				Set added = new HashSet();

				// QueryResultHandles that were in view in the previous spatiotemporal
				// query but are no longer in view as the result of the current query.
				Set removed = new HashSet();

				// QueryResultHandles that were in view in the previous spatiotemporal
				// query but have different values as the result of the current query.
				Set changed = new HashSet();

				added.clear();
				removed.clear();
				changed.clear();

				// _currentPosition is the center of the query region, not
				// the lower left corner.  Queryable.query() expects the
				// lower left corner, so we have to calculate it from
				// _currentPosition and _currentWidth.

				if ((null != _currentPosition) && (null != _currentWidth)) {
					Vector3d lowerLeftCorner = 
						new Vector3d(_currentPosition.x - _currentWidth.x / 2.0,
									 _currentPosition.y - _currentWidth.y / 2.0,
									 _currentPosition.z - _currentWidth.z / 2.0);
			
					Set currentResults = 
						_factory.query(lowerLeftCorner, _currentWidth,
									   _currentLBTime, _currentUBTime,
									   _previousResults,
									   _fieldNames, added, removed, changed,
									   _context);

					if ((added.size() > 0 ) || (removed.size() > 0) ||
						(changed.size() > 0)) {
						Field.ChangeMessage fcm = 
							new Field.ChangeMessage(QueryFields.RESULTS_ADDED, 
													(Serializable)added);
						answer.addFieldChangeMessage(fcm);

						fcm = new Field.ChangeMessage(QueryFields.RESULTS_REMOVED, 
													  (Serializable)removed);
						answer.addFieldChangeMessage(fcm);

						fcm = new Field.ChangeMessage(QueryFields.RESULTS_CHANGED, 
													  (Serializable)changed);
						answer.addFieldChangeMessage(fcm);
					} 

					_previousResults = currentResults;
				} else {
					_LOG.info(new Strings(new Object[] {
						this, 
						" NOT restoring because location (", _currentPosition,
						") or  width(", _currentWidth,
						") is NULL."
					}));
				}
			} else {
				_LOG.error(new Strings(new Object[] {
					this, ": No factory!"}));
			
			}
		}

		return answer;
    }
};
