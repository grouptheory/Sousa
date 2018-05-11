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
   <CODE>AnnotationMutable</CODE> inserts an annotation into a
   database at a given location, for a given temporal extent.
*/
public class AnnotationInsertable implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected static final Logger _LOG = 
		Logger.getLogger(AnnotationInsertable.class);
 
	/** 
		Searches for annotations inside a spatial region of interest.
	*/
	static final String ACTION_SQL = "INSERT INTO annotations (text, lat, lon, elev, mint, maxt) "
	    + "VALUES "
	    + "(?, ?, ?, ?, ?, ?)";

	/**
	   The connection to the database
	*/
	/*@ non_null */ private final java.sql.Connection _conn;

	/**
	   The spatial query for the database
	*/
	/*@ non_null */ private final PreparedStatement _action;

	/**
	   Class constructor that opens a connection to a database at the
	   specified <CODE>dbURL</CODE>.

	   @param dbURL the URL of the database server
	*/
	public AnnotationInsertable(/*@ non_null */ String dbURL) 
		throws SQLException
	{
		_LOG.warn(new Strings(new Object[] {
		    "AnnotationInsertable(", dbURL, ")"}));
	
		this._conn = DB.getConnection(dbURL);

		// Forward scrolling only
		this._action = _conn.prepareStatement(ACTION_SQL);
	}

	// mil.navy.nrl.cmf.sousa.spatiotemporal.Insertable

	/**
	   Adds the client's annotation at the specified position,
	   with the specified spatiotemporal extents.
	 */

	public void insert(String text, 
			   Vector3d llCurrent, 
			   Calendar lbTimeCurrent, Calendar ubTimeCurrent)
	{
		try {
		    doInsert(text, llCurrent, lbTimeCurrent, ubTimeCurrent);
		} catch (SQLException ex) {
		    _LOG.error(new Strings(new Object[] {
			this, 
			" insert(", text, ", ", llCurrent, ", ...) caught ",
			ex, " for action=", _action }));
		}
	}


	// Utility

	/**
	   Performs the insert.
	   @throws SQLException
	 */
        void doInsert(String text, 
		      Vector3d llCurrent, 
		      Calendar lbTime, Calendar ubTime)
	    throws SQLException
        {
		Set answer = new HashSet();
		int i=0;

		// INSERT INTO annotation (text, lat, lon, elev, mint, maxt) VALUES (?, ?, ?, ?, ?, ?)

		_action.setString(++i, text);
		_action.setDouble(++i, llCurrent.x);
		_action.setDouble(++i, llCurrent.y);
		_action.setDouble(++i, llCurrent.z);
		_action.setDouble(++i, (double) lbTime.getTimeInMillis() );
		_action.setDouble(++i, (double) ubTime.getTimeInMillis() );

		_LOG.debug(new Strings(new Object[]{
		    this, " doQuery():",
		    " _action=", _action}));
		
		int rows = _action.executeUpdate();
	
		_LOG.debug(new Strings(new Object[] 
			{this, " doInsert(", text, ", ", llCurrent, "...) produced ", 
			 new Integer(rows), " results"} ));
	}
}
