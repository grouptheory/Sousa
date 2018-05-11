package mil.navy.nrl.cmf.annotation;

import java.awt.Dimension;
import java.io.Serializable;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import mil.navy.nrl.cmf.sousa.spatiotemporal.*;
import mil.navy.nrl.cmf.sousa.util.Strings;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
   <CODE>AnnotationQueryable</CODE> searches a database for annotations
   within a spatial region of interest.
*/
public class AnnotationQueryable implements Queryable, Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected static final Logger _LOG = 
		Logger.getLogger(AnnotationQueryable.class);
 
	/** 
		Searches for annotations inside a spatial region of interest.
	*/
	static final String QUERY_SQL = "SELECT text, lat, lon, elev, mint, maxt "
	    + "FROM annotations "
	    + "WHERE "
	    + "(lat between ? and ?) AND "
	    + "(lon between ? and ?) AND "
	    + "(elev between ? and ?) AND "
	    + "(? >= mint) AND "
	    + "(? <= maxt)";

	/**
	   The connection to the database
	*/
	/*@ non_null */ private final java.sql.Connection _conn;

	/**
	   The spatial query for the database
	*/
	/*@ non_null */ private final PreparedStatement _query;

	/**
	   Class constructor that opens a connection to a database at the
	   specified <CODE>dbURL</CODE>.

	   @param dbURL the URL of the database server
	*/
	public AnnotationQueryable(/*@ non_null */ String dbURL) 
		throws SQLException
	{
		_LOG.warn(new Strings(new Object[] {
		    "AnnotationQueryable(", dbURL, ")"}));
	
		this._conn = DB.getConnection(dbURL);

		// Forward scrolling only
		this._query = _conn.prepareStatement(QUERY_SQL);
	}

	// mil.navy.nrl.cmf.sousa.spatiotemporal.Queryable

	/**
	   Answers the client's spatiotemporal query and computes which
	   answers are new and which previous answers are no longer true.

	   <P>

	   <EM>This implementation computes the <CODE>added</CODE> and
	   <CODE>removed</CODE> sets and delegates the actual query to
	   {@link #doQuery(Vector3d, Vector3d, Calendar, Calendar,
	   Set)}.</EM> <P>

	   <EM>This can probably be refactored.</EM>
	 */

	public Set query(Vector3d llCurrent, Vector3d widthCurrent,
			 Calendar lbTimeCurrent, Calendar ubTimeCurrent,
			 Set previousResults,
			 Set fieldNames,
			 Set added, 
			 Set removed,
			 Set changed,
			 Map context)
	{
		// This version of query() requires no per-client context.
		Set currentResults = new HashSet();

		try {
			currentResults = doQuery(llCurrent, widthCurrent, 
						 lbTimeCurrent, ubTimeCurrent, 
						 fieldNames);

			if (_LOG.isDebugEnabled()) {
				_LOG.debug("Contents of currentResults:");
			
				for (Iterator i = currentResults.iterator(); i.hasNext(); ) {
					_LOG.debug(i.next());
				}
			}

			if (_LOG.isDebugEnabled()) {
				_LOG.debug("Contents of previousResults:");

				for (Iterator i = previousResults.iterator(); i.hasNext(); ) {
					_LOG.debug(i.next());
				}
			}

			Set temp = new HashSet();

			added.clear();
			removed.clear();
		
			// temp = currentResults \ previousResults
			temp.addAll(currentResults);
			temp.removeAll(previousResults);
			added.addAll(temp);
	
			temp.clear();

			// temp = previousResults \ currentResults
			temp.addAll(previousResults);
			temp.removeAll(currentResults);
			removed.addAll(temp);

		} catch (SQLException ex) {
		    _LOG.error(new Strings(new Object[] {
			this, 
			" query(", llCurrent, ", ", widthCurrent,
			", ...) caught ",
			ex, " for query=", _query }));
		}

		return currentResults;
	}


	// Utility

	/**
	   Performs the client's spatiotemporal query.  The answers are
	   annotations within the spatiotemporal region of interest.

	   @param llCorner the lower left corner of the spatial region of interest
	   @param width the width of the spatial region of interest
	   @param lbTime the lower bound of the temporal region of interest
	   @param ubTime the upper bound of the temporal region of interest
	   @param fieldNames the fields to include in each QueryResultHandle

	   @return a set of {@link
	   mil.navy.nrl.cmf.sousa.spatiotemporal.QueryResultHandle} that
	   match the spatiotemporal query.

	   @throws SQLException
	 */
	private Set doQuery(Vector3d llCorner, Vector3d width, 
			    Calendar lbTime, Calendar ubTime, Set fieldNames) 
	    throws SQLException
        {
		Set answer = new HashSet();
		Map fields = new HashMap();
		int i=0;
	
		//
		// Shortcut: Don't query if he's not interested in any fields.
		//
		if ((null != fieldNames) && (fieldNames.size() > 0)) {
		    // "WHERE "
		    // "(lat between ? and ?) AND "+
		    _query.setDouble(++i, llCorner.x);
		    _query.setDouble(++i, width.x + llCorner.x);

		    // "(lon between ? and ?) AND "+
		    _query.setDouble(++i, llCorner.y);
		    _query.setDouble(++i, width.y + llCorner.y);

		    // "(elev between ? and ?) AND "+
		    _query.setDouble(++i, llCorner.z);
		    _query.setDouble(++i, width.z + llCorner.z);

		    // "(? >= mint) AND ";
		    _query.setDouble(++i, (double) ubTime.getTimeInMillis() );

		    // "(? <= maxt)";
		    _query.setDouble(++i, (double) lbTime.getTimeInMillis() );

		    _LOG.debug(new Strings(new Object[]{
			this, " doQuery():",
			" _query=", _query}));

		    ResultSet rs = _query.executeQuery();

		    while (rs.next()) {
				Vector3d position =  null;
				String annotation = rs.getString("text");

				fields.clear();
				if (fieldNames.contains("text"))
					fields.put("text", annotation);

				if (fieldNames.contains("lat"))
					fields.put("lat", new Double(rs.getDouble("lat")));
				if (fieldNames.contains("lon"))
					fields.put("lon", new Double(rs.getDouble("lon")));
				if (fieldNames.contains("elev"))
					fields.put("elev", new Double(rs.getDouble("elev")));
				if (fieldNames.contains("mint"))
					fields.put("mint", new Double(rs.getDouble("mint")));
				if (fieldNames.contains("maxt"))
					fields.put("maxt", new Double(rs.getDouble("maxt")));

				answer.add(new QueryResultHandle(annotation.hashCode(), fields));
		    }

		    _LOG.debug(new Strings(new Object[] {
			this, " doQuery(", llCorner, ", ", width, 
			"...) produced ", new Integer(answer.size()),
			" results"
		    } ));
		}

		return answer;
	}
}
