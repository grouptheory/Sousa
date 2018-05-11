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

public class DB {

    protected static final Logger _LOG = 
	    Logger.getLogger(DB.class);

    /**
       The connections to the databases
       String (URL) to java.sql.Connection 
    */
    /*@ non_null */ private static HashMap _connections = new HashMap();

    static public java.sql.Connection getConnection(/*@ non_null */ String dbURL) 
		throws SQLException
	{
	    _LOG.warn(new Strings(new Object[] {"DBConn(", dbURL, ")"}));

	    java.sql.Connection conn = (java.sql.Connection)_connections.get(dbURL);
	    if (conn==null) {
		conn = DriverManager.getConnection(dbURL, System.getProperty("user.name"), "");
		conn.setAutoCommit(true);
		_connections.put(dbURL, conn);
	    }
	    return conn;
	}
}

