package mil.navy.nrl.cmf.sousa.idol.service.city;

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
   <CODE>CityQueryable</CODE> searches a database for cities with
   population corresponding to the client's elevation.  As elevation
   increases, so does the minimum population required for a city to
   match the query.  In other words, the higher the elevation, the
   bigger and fewer the cities.

   <P>
   <EM>IMPLEMENTATION QUESTION: Can CityQueryable, ModelQueryable, and
   RasterQueryable be refactored into SQLQueryable?</EM>
*/
public class CityQueryable implements Queryable, Serializable
{
	private static final long serialVersionUID = 1L;

	protected static final Logger _LOG =
		Logger.getLogger(CityQueryable.class);
	
    // slide this factor as desired to map populous cities to elev
    private static final int SCALE_OFFSET_FACTOR = 2;
	
	/** 
		Searches for cities with population greater than some
		threshold.  Returns the name of the city
		(<CODE>mapname</CODE>), the population (<CODE>pop</CODE>), and
		the location (<CODE>latitude</CODE>, <CODE>longitude</CODE>).
	 */
	private static final String QUERY_SQL = "SELECT feature_name AS mapname, " 
		// DAVID: As of April, 2007, population ("pop") isn't part of this
		// table.  USGS no longer includes population in the gnis table.
		//		+ "pop, "
		+ "X(lonlat) AS longitude, "
		+ "Y(lonlat) AS latitude, "
		+ "elev AS elevation "
		+ "FROM geonames_lonlat "
		// DAVID: Also, the feature types have changed.  'ppl' is now 'Populated Place'.
		//+ "WHERE (feature_type = 'ppl') AND (pop > ?) AND (lonlat && GeomFromText(?, 4326)) ";
		+ "WHERE (feature_type = 'Populated Place') AND (lonlat && GeomFromText(?, 4326));";

									
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
	public CityQueryable(/* @ non_null */String dbURL) throws SQLException {
		_LOG.warn(new Strings(new Object[] {
			"CityQueryable(", dbURL, ")"}));

		this._conn = DriverManager.getConnection(dbURL, System
				.getProperty("user.name"), "");

		//DAVID: addDataType(String, String) is a deprecated method.
		((org.postgresql.PGConnection) _conn).addDataType("geometry",
				"org.postgis.PGgeometry");
		((org.postgresql.PGConnection) _conn).addDataType("box3d",
				"org.postgis.PGbox3d");
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

			_LOG.info(new Strings(new Object[] 
				{this, " previous doQuery() produced ", 
				 new Integer(previousResults.size()),
				 " results"
				} ));

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
	   cities with population greater than 10^magnitude of the
	   client's elevation. For example, if the client's elevation is
	   1500 meters, then the cities returned are those with population
	   greater than 1000 (10^3) meters.

	   This implementation knows about the following Strings in
	   <CODE>fieldNames</CODE>: mapname, pop, north, east, type.

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
		throws SQLException {
		
		Set answer = new HashSet();
		Map fields = new HashMap();
		int i=0;
		
		//
		// Shortcut: Don't query if he's not interested in any fields.
		//
		if ((null != fieldNames) && (fieldNames.size() > 0)) {
			// For now 0 (all cities)... later base on the Vector3d width
			// Create the population query value based on log(elevation)
			double elevation = llCorner.z > 0? llCorner.z : 1;
			//System.out.println("Elevation is: " + elevation);
			long magnitude = Math.round(Math.log(elevation) * 1/Math.log(10));
			//System.out.println("Magnitude is: " + magnitude);
			long popMagnitude = magnitude - SCALE_OFFSET_FACTOR;
			//System.out.println("Pop Magnitude is: " + popMagnitude);
			
			if (popMagnitude < 0)
				popMagnitude = 0;
			else if (popMagnitude > 7) // 9 is close to maxint
				popMagnitude = 7;
			// DAVID: remove this if there isn't population in your gnis
			//int population = new Double(Math.pow(10,popMagnitude)).intValue();
			//_query.setInt(++i,population);
			// DAVID: end of changes for missing population

			// Corners: lower left, upper right
			Point llC = new Point(llCorner.y, llCorner.x);
			Point urC = new Point(llCorner.y + width.y, llCorner.x + width.x);
			
			Geometry points = new MultiPoint(new Point[] {llC, urC});
			_query.setString(++i, points.toString());
			
			_LOG.debug(new Strings(new Object[] {
					this, " doQuery(): time=",
					new Long(lbTime.getTimeInMillis()),
					" _query=",_query}));
			ResultSet rs = _query.executeQuery();
			
			while (rs.next()) {
				Vector3d position = null;
				String mapname = rs.getString("mapname");
				
				fields.clear();
				if (fieldNames.contains("mapname"))
					fields.put("mapname",mapname);
				
				// DAVID: No "pop" field any more.  It's not part of USGS's gnis info.
				//if (fieldNames.contains("pop"))
				//	fields.put("pop",new Integer(rs.getInt("pop")));

				if (fieldNames.contains("north"))
					fields.put("north", new Double(rs.getDouble("latitude")));
				
				if (fieldNames.contains("east"))
					fields.put("east", new Double(rs.getDouble("longitude")));
				
				if (fieldNames.contains("minelev"))
					fields.put("minelev", new Integer(rs.getInt("elevation")));
				
				answer.add(new QueryResultHandle(mapname.hashCode(),fields));
			}

			_LOG.info(new Strings(new Object[] 
				{this, " doQuery(", llCorner, ", ", width, 
				 "...) produced ", new Integer(answer.size()),
				 " results"
				} ));
		}
		
		return answer;
	}
	
	

}
