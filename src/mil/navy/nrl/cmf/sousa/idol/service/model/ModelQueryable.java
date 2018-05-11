package mil.navy.nrl.cmf.sousa.idol.service.model;

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
import org.postgis.Geometry;
import org.postgis.MultiPoint;
import org.postgis.Point;

/**
   <CODE>ModelQueryable</CODE> searches a database for VRML models
   within a spatial region of interest.

   <P>
   <EM>IMPLEMENTATION QUESTION: Can CityQueryable, ModelQueryable, and
   RasterQueryable be refactored into SQLQueryable?</EM>
*/
public class ModelQueryable implements Queryable, Serializable
{
	private static final long serialVersionUID = 1L;

	protected static final Logger _LOG = 
		Logger.getLogger(ModelQueryable.class);

	/** 
		Searches for models inside a spatial region of interest.
		Returns the name of the model (<CODE>mapname</CODE>) and its
		the location (<CODE>latitude</CODE>, <CODE>longitude</CODE>,
		<CODE>elevation</CODE>).
	*/
	static final String QUERY_SQL =	"SELECT filename AS mapname, " +
		"X(wkb_geometry) AS longitude, " +
		"Y(wkb_geometry) AS latitude, " +
		"Z(wkb_geometry) AS elevation " + 
		//
		// DAVID: Include elevation in the calculations one day
		//
		"FROM bldg_latlon " +
		// a box with lower left corner == location
		// and upper right corner == location + width
		"WHERE GeomFromText(?, 4326) && " +
		// overlaps the_geometry in the database
		"wkb_geometry;";

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
	public ModelQueryable(/*@ non_null */ String dbURL) 
		throws SQLException
	{
		_LOG.warn(new Strings(new Object[] {
								  "ModelQueryable(", dbURL, ")"}));
	
		this._conn = DriverManager.getConnection(dbURL, System.getProperty("user.name"), "");

		//DAVID: addDataType(String, String) is a deprecated method.
		((org.postgresql.PGConnection)_conn).addDataType("geometry", "org.postgis.PGgeometry");
		((org.postgresql.PGConnection)_conn).addDataType("box3d", "org.postgis.PGbox3d");
		_conn.setAutoCommit(true);

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

	// DAVID: When does doQuery throw SQLException? 
	/**
	   Performs the client's spatiotemporal query.  The answers are
	   models within the spatiotemporal region of interest.

	   <P> 

	   This implementation knows about the following Strings in
	   <CODE>fieldNames</CODE>: mapname, minelev, maxelev, north,
	   east, type.

	   <P>
	   <EM>BUGS:
	   <UL>
	   <LI>Ignores time.
	   <LI>Assumes that <CODE>llCorner's</CODE> elevation is enough.
	   <LI>All geometries are really 2D.  We want them to be 3D.
	   </UL>
	   </EM>

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
			//
			// the_geom intersects the bounding box
			//

			// llCorner is the point you are interested in
			Point llC = new Point(llCorner.y, llCorner.x, llCorner.z);
		
			// A point on the perimeter of the search bubble
			Point urC = new Point(llCorner.y + width.y, llCorner.x + width.x, llCorner.z + width.z);

			Geometry points = new MultiPoint(new Point[] { llC, urC });

			//  a box, centered at the point of intererest,
			_query.setString(++i, points.toString());

			_LOG.debug(new Strings(new Object[] {
									   this, " doQuery(): time=",
									   new Long(lbTime.getTimeInMillis()),
									   " _query=", _query}));

			ResultSet rs = _query.executeQuery();

			while (rs.next()) {
				Vector3d position =  null;
				String mapname = rs.getString("mapname");

				fields.clear();
				if (fieldNames.contains("mapname"))
					fields.put("mapname", mapname);

				if (fieldNames.contains("minelev"))
					fields.put("minelev", new Double(rs.getDouble("elevation")));

				if (fieldNames.contains("maxelev"))
					fields.put("maxelev", new Double(rs.getDouble("elevation")));

				if (fieldNames.contains("north"))
					fields.put("north", new Double(rs.getDouble("latitude")));

				if (fieldNames.contains("east"))
					fields.put("east", new Double(rs.getDouble("longitude")));

				answer.add(new QueryResultHandle(mapname.hashCode(), fields));
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
