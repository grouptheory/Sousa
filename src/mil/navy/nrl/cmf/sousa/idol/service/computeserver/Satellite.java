package mil.navy.nrl.cmf.sousa.idol.service.computeserver;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import mil.navy.nrl.cmf.sousa.spatiotemporal.QueryClientFields;
import mil.navy.nrl.cmf.sousa.spatiotemporal.Vector3d;
import mil.navy.nrl.cmf.stk.*;

class Satellite
extends AbstractCalcObject
implements SxPxConstants 
{
	// Do not change the order of these values!
	private static final int SGP = 0;
	private static final int SGP4 = 1;
	private static final int SGP8 = 2;
	private static final int SDP4 = 3;
	private static final int SDP8 = 4;

	private final transient String _name;
	private final transient String _owner;
	private final transient String _mission;
	private final transient SxPx _sxpx;

	// mutable Vector3ds
	private final transient Vector3d _eci = new Vector3d();
	private final transient Vector3d _rpt = new Vector3d();
	private final transient Vector3d _lle = new Vector3d();
	private final transient Vector3d _vlle = new Vector3d();
	private final transient Calendar _t = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

// Constructors

Satellite(TLE tle, String name, String owner, String mission)
{
	super(tle.id);
	this._name = name;
	this._owner = owner;
	this._mission = mission;
	this._sxpx = Satellite.createSxPx(Satellite.SGP4, tle);
}

// AbstractCalcObject

public void computePositionVelocity(Calendar time, Vector3d xyz, Vector3d vel) 
{
	double tsince = // Offset from TLE epoch, in minutes
		(DateUtils.julian_date(time) - _sxpx.getTLE().epoch) * XMNPDA;

	// Do the calculation.  It's safe to cast _sxpx.compute() to
	// Vector3d because it returns _eci, which is a Vector3d.
	//
	Satellite.eci2rpt(time, (Vector3d)_sxpx.compute(tsince, _eci, vel), _rpt);
		// _eci is Earth-centered inertial, a cartesian system, in units of kilometers
		//	see http://www.celestrak.com/columns/v02n01/
		// vel is in km/min
		// _rpt is elevation in kilometers, latitude and longitude in degrees

	// convert to a Cartesian system common to both satellite and viewer
	AbstractCalcDB.rpt2xyz(_rpt, xyz);
	_t.setTime(time.getTime());

	// Determine velocity in deg/sec & meters/sec by
	// adding eci velocity to eci position and converting to
	// rpt with time 1 minute ahead
	Vector3d rpt2 = new Vector3d();
	Vector3d eci2 = new Vector3d();
	eci2.x = _eci.x + vel.x;
	eci2.y = _eci.y + vel.y;
	eci2.z = _eci.z + vel.z;

	Calendar time2 = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
	time2.setTime(time.getTime());
	time2.add(Calendar.MINUTE, 1);

	Satellite.eci2rpt(time2, eci2, rpt2);

	// vlle is lat deg per sec/lon deg per sec/elev meters per sec
	_vlle.x = (rpt2.y - _rpt.y) / 60.0;
	_vlle.y = (rpt2.z - _rpt.z) / 60.0;
	_vlle.z = ((rpt2.x - _rpt.x) * 1000.0) / 60.0;
}

public Map additionalQueryResultFields() 
{
	HashMap answer = new HashMap();

	// Client expects a Vector3d with x = lat, y = lon, z = elev

	// rpt is rho/phi/theta
	// lle is lat/lon/elev
	_lle.x = _rpt.y;
	_lle.y = _rpt.z;
	_lle.z = _rpt.x * 1000.0;

	// This replaces the Cartesian position that the superclass put in
	// the POSITION_NAME field with a spherical position.
	answer.put(QueryClientFields.POSITION_FIELDNAME, _lle);
	answer.put("velocity", _vlle);
	answer.put("time", new Long(_t.getTimeInMillis()));
	answer.put("mapname", _owner + ":" + _name);	
	return answer;
}

// Utility

/**
   Convert Earth-centered inertial cartesian coordinates into rho-phi-theta spherical coordinates.
   See http://www.celestrak.com/columns/v02n01
 */
//@ ensures \result != null;
static Vector3d eci2rpt(/*@ non_null */ Calendar time, /*@ non_null */ Vector3d eci, /*@ non_null */ Vector3d rpt)
{
	double lon = mod(actan(eci.y, eci.x) - DateUtils.gmst(time), TWOPI);
	double r = sqrt(sqr(eci.x) + sqr(eci.y));
	double lat = actan(eci.z, r);
	double phi;
	double c;
	do {
		phi = lat;
		c = 1.0 / sqrt(1.0 - E2 * sqr(sin(phi)));
		lat = actan(eci.z + XKMPER * c * E2 * sin(phi), r);
	} while (abs(lat - phi) > E10A);
	double elev = r / cos(lat) - XKMPER * c;

	rpt.set(elev,
		toDegrees(lat),
		toDegrees(lon));

	return rpt;
}

//@ requires ((ephem >= 0) && (ephem <= 4));
//@ ensures result != null;
static SxPx createSxPx(int ephem, /*@ non_null */ TLE tle)
{
	if (tle.is_deep && (ephem == SGP4 || ephem == SGP8))
		ephem += 2; /* switch to an SDPx */
	if (!tle.is_deep && (ephem == SDP4 || ephem == SDP8))
		ephem -= 2; /* switch to an SGPx */
	SxPx sxpx = null;
	switch (ephem) {
		case SGP:
			sxpx = new SGP(tle);
			break;
		case SGP4:
			sxpx = new SGP4(tle);
			break;
		case SGP8:
			sxpx = new SGP8(tle);
			break;
		case SDP4:
			sxpx = new SDP4(tle);
			break;
		case SDP8:
			sxpx = new SDP8(tle);
			break;
		default:
			throw new IllegalArgumentException("Invalid ephemeris selected");
	}
	return sxpx;
}
}; // Satellite
