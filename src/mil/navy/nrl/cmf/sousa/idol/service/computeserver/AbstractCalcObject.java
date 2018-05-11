package mil.navy.nrl.cmf.sousa.idol.service.computeserver;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import mil.navy.nrl.cmf.sousa.spatiotemporal.*;
import mil.navy.nrl.cmf.stk.*;

public abstract class AbstractCalcObject
extends MathSupport
implements CalcObject
{
	static final double CALCULATION_DELTA_TIME = 1.0; // seconds

	static final double MIN_ANGULAR_VELOCITY = 0.001; // degrees/second

	static final int MIN_RECALC_INTERVAL = 1; // seconds
	static final int MAX_RECALC_INTERVAL = 60; // seconds

	//static final double VISUAL_ANGULAR_GRANULARITY = 0.01;
	static final double VISUAL_ANGULAR_GRANULARITY = 1.0;

	private final String _id;
	private final Vector3d _xyz = new Vector3d();

	private final transient Vector3d _vel = new Vector3d();
	private final transient Vector3d _xyz2 = new Vector3d();
	private final transient Vector3d _viewer = new Vector3d();
	private final transient Vector3d _viewer2 = new Vector3d();
	private       transient int      _recalcInterval = 0;

// Constructors

protected AbstractCalcObject(String id)
{
	this._id = id;
	this._recalcInterval = 0;
}

// CalcObject

public int computeRecalculationInterval(Calendar time, Calendar lasttime, boolean first, Vector3d viewerpos)
{
	if (!time.equals(lasttime) || first) {
		computePositionVelocity(time, _xyz, _vel);
	}

	// _xyz is in meters
	// viewerpos is in meters
	_viewer.sub(_xyz, viewerpos);

	// CALCULATION_DELTA_TIME is in seconds
	// _vel is in km/min
	// _xyz is in meters
	// _xyz2 needs to be in meters
	_xyz2.scaleAdd(CALCULATION_DELTA_TIME * 1000.0 / 60.0, _vel, _xyz);

	// _xyz2 is in meters
	// viewerpos is in meters
	_viewer2.sub(_xyz2, viewerpos);

 	// result is in radians, convert to degrees
	double angle = toDegrees(_viewer.angle(_viewer2));

	if (Double.isNaN(angle)) {
		_recalcInterval = MIN_RECALC_INTERVAL;
	} else {
		// convert to degrees/sec
		double angularVelocity = angle / CALCULATION_DELTA_TIME;

		// moves very slowly
		if (angularVelocity < MIN_ANGULAR_VELOCITY) {
			// recalc infrequently		
			_recalcInterval = MAX_RECALC_INTERVAL;
		} else {
			// compute recalculate
			//_recalcInterval = (int)(1000.0 * VISUAL_ANGULAR_GRANULARITY / angularVelocity);
			_recalcInterval = (int)(100.0 * VISUAL_ANGULAR_GRANULARITY / angularVelocity);
		}

		if (_recalcInterval > MAX_RECALC_INTERVAL) {
			_recalcInterval = MAX_RECALC_INTERVAL;
		}
		if (_recalcInterval < MIN_RECALC_INTERVAL) {
			_recalcInterval = MIN_RECALC_INTERVAL;
		}
	}

	return _recalcInterval;
}

public QueryResultHandle convertToHandle()
{
	Map fields = new HashMap();
	Map additionalFields = additionalQueryResultFields();
	
	fields.put(QueryClientFields.POSITION_FIELDNAME, _xyz);
	fields.put("velocity", _vel);

	// additionalFields may contain a POSITION_NAME key-value pair.
	// If so, that pair replaces the one in fields.
	fields.putAll(additionalFields);
	
	return new QueryResultHandle(_id.hashCode(), fields);
}

// AbstractCalcObject

public abstract void computePositionVelocity(Calendar time, Vector3d xyz, Vector3d vel);

public abstract Map additionalQueryResultFields();

// java.lang.Object

public final int hashCode()
{
	return Integer.parseInt(_id);
}

public final boolean equals(Object o)
{
	return (o instanceof AbstractCalcObject) && _id.equals(((AbstractCalcObject)o)._id);
}
}; // AbstractCalcObject
