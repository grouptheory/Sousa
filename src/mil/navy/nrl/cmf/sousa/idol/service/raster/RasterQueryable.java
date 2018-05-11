package mil.navy.nrl.cmf.sousa.idol.service.raster;

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
import mil.navy.nrl.cmf.sousa.util.StackTrace;
import mil.navy.nrl.cmf.sousa.util.Strings;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.postgis.Geometry;
import org.postgis.MultiPoint;
import org.postgis.Point;

/**
   <CODE>RasterQueryable</CODE> searches a database for rasters
   within a spatial region of interest.

   <P>
   <EM>IMPLEMENTATION QUESTION: Can CityQueryable, ModelQueryable, and
   RasterQueryable be refactored into SQLQueryable?</EM>
*/
public class RasterQueryable implements Queryable, Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected static final Logger _LOG = 
		Logger.getLogger(RasterQueryable.class);

	/** 
		Searches for rasters inside a spatial region of interest.
		Returns the name of the raster (<CODE>mapname</CODE>), the
		location of its lower left corner (latitude in
		<CODE>south</CODE>, longitude in <CODE>west</CODE>), and the
		location of its upper right corner (latitude in
		<CODE>north</CODE>, longitude in <CODE>east</CODE>).

		The only variable in the query must be a geometry that
		surrounds the users's location.  It will be constructed by
		this object.
		
		+ "WHERE (bounds && GeomFromText(?, 4326)) ";
	*/
/*
	static final String QUERY_SQL = "SELECT mapname, "
		+ "X(GeometryN(bounds, 1)) AS west, " // longitude
		+ "Y(GeometryN(bounds, 1)) AS south, "// latitude
		+ "X(GeometryN(bounds, 2)) AS east, " // longitude
		+ "Y(GeometryN(bounds, 2)) AS north " // latitude
		+ "FROM raster_lonlat "

		// DAVID: change the query to include time
		//
		// 1st? is a geometry (e.g. point, multipoint, polygon)
		//
		+ "WHERE (bounds && GeomFromText(?, 4326)) ";
*/
	final String QUERY_SQL;

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
	public RasterQueryable(/*@ non_null */ String dbURL, /*@ non_null */ String query) 
		throws SQLException
	{
		_LOG.warn(new Strings(new Object[] {
			"RasterQueryable(", dbURL, ") username: "+System.getProperty("user.name")}));
	
		QUERY_SQL = query;

		// DAVID: was
		//		this._conn = DriverManager.getConnection(dbURL, System.getProperty("user.name"), "");
		// I removed it in favor of userid and password as part of the dbURL.
		// This lets me run RasterQueryables as any user, not just myself.

		this._conn = DriverManager.getConnection(dbURL);


		//DAVID: addDataType(String, String) is a deprecated method.
		((org.postgresql.PGConnection)_conn).addDataType("geometry", "org.postgis.PGgeometry");
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
				ex, " for query=", _query,
				StackTrace.formatStackTrace(ex) }));
		}

		return currentResults;
	}


	// Utility

	// DAVID: When does doQuery throw SQLException? 
	/**
	   Performs the client's spatiotemporal query.  The answers are
	   rasters within the spatiotemporal region of interest.

	   <P> 

	   This implementation knows about the following Strings in
	   <CODE>fieldNames</CODE>: mapname, north, south, west, east,
	   nrows, minelev, maxelev, type.
		
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
	
		//
		// Shortcut: Don't query if he's not interested in any fields.
		//
		if ((null != fieldNames) && (fieldNames.size() > 0)) {
			// DAVID: See CityQueryable and ModelQueryable for another
			// way to construct the bounding box.
			_query.setString(1, makeBBox(llCorner, width).toString());

			_LOG.debug(new Strings(new Object[]{
				this, " doQuery():",
				" _query=", _query}));

			ResultSet rs = _query.executeQuery();
	
			while (rs.next()) {
				Vector3d position =  null;
				String mapname = rs.getString("mapname");

				fields.clear();
				if (fieldNames.contains("mapname"))
					fields.put("mapname", mapname);

				if (fieldNames.contains("north"))
					fields.put("north", new Double(rs.getDouble("north")));

				if (fieldNames.contains("south"))
					fields.put("south", new Double(rs.getDouble("south")));

				if (fieldNames.contains("west"))
					fields.put("west", new Double(rs.getDouble("west")));

				if (fieldNames.contains("east"))
					fields.put("east", new Double(rs.getDouble("east")));

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

	/**
	   Creates a bounding box given the lower left corner and the
	   width of the box.

	   <P>
	   <EM>TODO: Change this to use Points in 3-space instead of Points in
	   2-space.</EM>

	   @param llCorner the lower left corner of the box
	   @param width the width of the box.
	*/
	private Geometry makeBBox(Vector3d llCorner, Vector3d width) 
	{
		Point lowerLeft = new Point(llCorner.x, llCorner.y);
		Point upperRight = new Point(llCorner.x + width.x, llCorner.y + width.y);

		Geometry answer = new MultiPoint(new Point[] { lowerLeft, upperRight });

		_LOG.debug(new Strings(new Object[] {
			this, " makeBBox(", llCorner, ", ", width, ") returns ", answer
		} ));
	
		return answer;
	}

}
