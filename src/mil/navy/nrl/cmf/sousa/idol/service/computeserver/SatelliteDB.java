package mil.navy.nrl.cmf.sousa.idol.service.computeserver;

import java.io.Serializable;
import java.sql.*;
import java.util.HashSet;
import java.util.Set;
import mil.navy.nrl.cmf.stk.*;
import mil.navy.nrl.cmf.sousa.util.StackTrace;
import mil.navy.nrl.cmf.sousa.util.Strings;

public class SatelliteDB
	extends AbstractCalcDB
	implements SxPxConstants, Serializable
{
	private final String _jdbcClass;
	private final String _jdbcURL;
	private final String _querySD;
	private final String _queryTCE;

	private Connection _conn;

// Constructors

public SatelliteDB(String name, String jdbcURL, String jdbcClass, String whereClause)
{
	super(name);
	this._jdbcURL = jdbcURL;
	this._jdbcClass = jdbcClass;
	this._querySD = "SELECT DISTINCT sd.id, sd.sat_name, sd.owner, sd.mission FROM sd where " + whereClause + ";";
	this._queryTCE = "SELECT * FROM tce WHERE (id = ?) ORDER BY epoch_year desc, epoch_day desc, epoch_time desc;";
}

// CalcDB

public boolean initialize()
throws java.util.prefs.BackingStoreException
{
	// perform derived initializations first
	boolean success = true;
	try {
		Class.forName(this._jdbcClass);
	} catch (Exception ex) {
		throw new java.util.prefs.BackingStoreException(ex);
	}

	try {
		this._conn = DriverManager.getConnection(this._jdbcURL, System.getProperty("user.name"), "");
		this._conn.setAutoCommit(false);
	} catch (Exception ex) {
		AbstractCalcDB._LOG.warn(new Strings(new Object[] {
			"Connection to ", this._jdbcURL, " by ",
			System.getProperty("user.name"),
			" failed."}));

		throw new java.util.prefs.BackingStoreException(ex);
	}

	// then call super.initialize, which will downcall into
	// createCalcObjects()
	if (success) {
		success &= super.initialize();
	}

	return success;
}

// AbstractCalcDB

public Set createCalcObjects()
{
	Set sds = new HashSet();
	
	try {
		PreparedStatement querySD = _conn.prepareStatement(_querySD);
		PreparedStatement queryTCE = _conn.prepareStatement(_queryTCE);

		ResultSet rsSD = querySD.executeQuery();
		while (rsSD.next()) {
			queryTCE.setString(1, rsSD.getString("id"));
			ResultSet rsTCE = queryTCE.executeQuery();
			// Expecting only one!
			while (rsTCE.next()) {
				TLE tle = new TLE(rsTCE.getString("id"), 
					rsTCE.getString("sec"),
					rsTCE.getString("id_year"), 
					rsTCE.getString("id_launch"), 
					rsTCE.getString("id_piece"),
					rsTCE.getInt("epoch_year"), 
					rsTCE.getInt("epoch_day"), 
					rsTCE.getDouble("epoch_time"),
					rsTCE.getDouble("mean_mot_1st_der"),
					rsTCE.getInt("mean_mot_2nd_der"), 
					rsTCE.getInt("iexp"),
					rsTCE.getInt("bstar_drag"), 
					rsTCE.getInt("ibexp"),
					rsTCE.getString("ephemeris_type"),
					rsTCE.getString("element_set_number"),
					rsTCE.getDouble("inclination"),
					rsTCE.getDouble("ra_of_asc_node"),
					rsTCE.getLong("eccentricity"),
					rsTCE.getDouble("arg_of_perigree"),
					rsTCE.getDouble("mean_anomaly"),
					rsTCE.getDouble("mean_motion"),
					rsTCE.getInt("rev_at_epoch")
				);
				sds.add(new Satellite(tle, 
					rsSD.getString("sat_name"),
					rsSD.getString("owner"), 
					rsSD.getString("mission")));
			}
			queryTCE.clearParameters();
		}
		querySD.clearParameters();

		AbstractCalcDB._LOG.info(new Strings(new Object[] 
			{new Integer(sds.size()), " TLEs loaded!" }));
	} catch (Exception e) {
		AbstractCalcDB._LOG.warn(new Strings(new Object[] 
			{StackTrace.formatStackTrace(e)}));
	}

	return sds;
}
};
