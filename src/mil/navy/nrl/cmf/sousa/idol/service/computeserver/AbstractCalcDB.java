package mil.navy.nrl.cmf.sousa.idol.service.computeserver;

import java.util.*;
import mil.navy.nrl.cmf.sousa.spatiotemporal.QueryResultHandle;
import mil.navy.nrl.cmf.sousa.spatiotemporal.Vector3d;
import mil.navy.nrl.cmf.sousa.util.*;
import org.apache.log4j.Logger;

public abstract class AbstractCalcDB
implements CalcDB
{
	protected static final Logger _LOG = Logger.getLogger(AbstractCalcDB.class);

	protected final String _name;

	private static Comparator _CalendarComparator = new Comparator()
	{
		public int compare(Object o1, Object o2)
		{
			Calendar c1 = (Calendar)o1;
			Calendar c2 = (Calendar)o2;
			if (c1.before(c2)) {
				return -1;
			} else if (c2.before(c1)) {
				return +1;
			} else {
				return 0;
			}
		}
			
		public boolean equals(Object obj)
		{
			return (obj == this);
		}
	};

	private final Set _cos = new HashSet(); // CalcObject

    // Keys to the context Map provided by clients
	private static final String LASTQUERYT="lastqueryt";
    private static final String LASTQUERYXYZ="lastqueryxyz";
    private static final String FWDRECALC2SD="fwdrecalc2sd";
    private static final String BWDRECALC2SD="bwdrecalc2sd";
    private static final String TFWD="tfwd";
    private static final String COUNT="count";

// Constructors

protected AbstractCalcDB(String name)
{
	this._name = name;
}

// Queryable

public Set query(Vector3d llCurrent, Vector3d widthCurrent, 
				 Calendar lbTimeCurrent, Calendar ubTimeCurrent,
				 Set previousResults, Set fieldNames, 
				 Set added, Set removed, Set changed,
				 Map context)
{
	Set answer = new HashSet(previousResults);
	
	if ((lbTimeCurrent==null) || (ubTimeCurrent==null)) {
		return answer;
	}

	// rho phi theta
	Vector3d rpt = new Vector3d();
	Vector3d vxyz = new Vector3d();

	// Context variables
	Calendar lastqueryt = (Calendar)context.get(LASTQUERYT);
	Vector3d lastqueryxyz = (Vector3d)context.get(LASTQUERYXYZ);
	TreeMap fwdrecalc2sd = (TreeMap)context.get(FWDRECALC2SD); // Calendar -> List (CalcObject)
	TreeMap bwdrecalc2sd = (TreeMap)context.get(BWDRECALC2SD); // Calendar -> List (CalcObject)
	Boolean tfwd = (Boolean)context.get(TFWD);
	Long count = (Long)context.get(COUNT);

	// Restore context variables	   
	if (null == lastqueryt) {
		lastqueryt = (Calendar)lbTimeCurrent.clone();
		context.put(LASTQUERYT, lastqueryt);
	}

	if (null == lastqueryxyz) {
		lastqueryxyz = new Vector3d();
		context.put(LASTQUERYXYZ, lastqueryxyz);
	}

	if (null == fwdrecalc2sd) {
		fwdrecalc2sd = new TreeMap(_CalendarComparator);
		context.put(FWDRECALC2SD, fwdrecalc2sd);
	}

	if (null == bwdrecalc2sd) {
		bwdrecalc2sd = new TreeMap(_CalendarComparator);
		context.put(BWDRECALC2SD, bwdrecalc2sd);
	}

	if (null == tfwd) {
		tfwd = Boolean.TRUE;
		context.put(TFWD, tfwd);
	}

	if (null == count) {
		count = new Long(0);
		context.put(COUNT, count);
	}

	//
	// In rpt, x is rho, y is phi, z is theta.
	//
	// In llCurrent, x is latitude, y is longitude, z is elevation.
	//
	// rho corresponds to elevation (in meters)
	// phi corresponds to latitude (in degrees)
	// theta corresponds to longitude (in degrees)
	//
	rpt.x = llCurrent.z;
	rpt.y = llCurrent.x;
	rpt.z = llCurrent.y;

	// convert to a Cartesian system common to both viewer and objects
	AbstractCalcDB.rpt2xyz(rpt, vxyz);

	AbstractCalcDB._LOG.info(new Strings(new Object[] {
		"query at ", llCurrent, " width ", widthCurrent}));


	TreeMap recalc2sd;
	int moved = 0;

	added.clear();
	removed.clear();
	changed.clear();

	// we only rebuild the calculation schedule if the temporal
	// arrow has changed direction, or the viewer has moved,
	// otherwise check for expired recalc intervals and update
	// only those objects
	if (computeTemporalArrow(context, lastqueryt, lbTimeCurrent) || !vxyz.equals(lastqueryxyz)) {
		tfwd = (Boolean)context.get(TFWD);

		// Clear both recalc schedules
		fwdrecalc2sd.clear();
		bwdrecalc2sd.clear();

		// Select schedule based on temporal arrow
		recalc2sd = (tfwd.booleanValue() ? fwdrecalc2sd : bwdrecalc2sd);

		// For every CalcObject in this CalcDB...
		for (Iterator i = _cos.iterator(); i.hasNext(); ) {
			CalcObject co = (CalcObject)i.next();

			// Calculate, then schedule recalculation at some point in the future (or past)
			Calendar recalct = (Calendar)lbTimeCurrent.clone();
			recalct.add(Calendar.SECOND, (tfwd.booleanValue() ? 1 : -1) *
				co.computeRecalculationInterval(lbTimeCurrent, lastqueryt, (count.longValue() == 0), vxyz));

			// Add to selected schedule
			List recalcs = (List)recalc2sd.get(recalct);
			if (null == recalcs) {
				recalc2sd.put(recalct, recalcs = new LinkedList());
			}
			recalcs.add(co);

			if (0 == count.longValue()) {
				QueryResultHandle qrh = co.convertToHandle();
				added.add(qrh);
			} else if (!lbTimeCurrent.equals(lastqueryt)) {
				QueryResultHandle qrh = co.convertToHandle();

				// DAVID: Remember that only the _id part of the
				// QueryResultHandle is significant to contains().
				// That's the way that QueryResultHandle.equals() is
				// implemented.
				if (previousResults.contains(qrh)) {
					changed.add(qrh);
				} else {
					added.add(qrh);
				}
			}

			/*
			if (!lbTimeCurrent.equals(lastqueryt) || (count.longValue() == 0)) {
				QueryResultHandle qrh = co.convertToHandle();
				if (!added.contains(qrh)) {
					moved++;
				}

				removed.add(qrh);
				added.add(qrh);
			}
			*/
		}

		// removed = previousResults \ changed
		removed.addAll(previousResults);
		//removed.removeAll(added);
		removed.removeAll(changed);

	} else {
		// Select schedule based on temporal arrow
		recalc2sd = (tfwd.booleanValue() ? fwdrecalc2sd : bwdrecalc2sd);

		Calendar queryt = null;
		while (null != (queryt = loopCond(tfwd, recalc2sd, lbTimeCurrent))) {
			List list = (List)recalc2sd.remove(queryt);

			for (Iterator j = list.iterator(); j.hasNext(); ) {
				CalcObject co = (CalcObject)j.next();

				// Calculate, then schedule recalculation at some point in the future (or past)
				Calendar nextt = (Calendar)lbTimeCurrent.clone();
				nextt.add(Calendar.SECOND, (tfwd.booleanValue() ? 1 : -1) *
					co.computeRecalculationInterval(lbTimeCurrent, lastqueryt, (count.longValue() == 0), vxyz));
				
				// Add to selected schedule
				List nextlist = (List)recalc2sd.get(nextt);
				if (null == nextlist) {
					recalc2sd.put(nextt, nextlist = new LinkedList());
				}
				nextlist.add(co);

				QueryResultHandle qrh = co.convertToHandle();

				// DAVID: Remember that only the _id part of the
				// QueryResultHandle is significant to contains().
				// That's the way that QueryResultHandle.equals() is
				// implemented.
				if (previousResults.contains(qrh)) {
					changed.add(qrh);
				} else {
					added.add(qrh);
				}
			}

			/*
			  if (!lbTimeCurrent.equals(lastqueryt) || (count.longValue() == 0)) {
			  QueryResultHandle qrh = co.convertToHandle();
			  if (!added.contains(qrh)) {
			  moved++;
			  }

			  removed.add(qrh);
			  added.add(qrh);
			  }
			*/

			// removed = previousResults \ changed
			removed.addAll(previousResults);
			//removed.removeAll(added);
			removed.removeAll(changed);
		}
	}

	AbstractCalcDB._LOG.debug(new Strings(new Object[] {
		"Number of CalcObjects that moved = ", 
		""+changed.size(), " context ", context}));


	if (recalc2sd.size() > 0) {
		AbstractCalcDB._LOG.info(new Strings(new Object[] {
			"time is now ", ""+lbTimeCurrent.getTimeInMillis(),
			" next computation is at ", ""+((Calendar)(recalc2sd.firstKey())).getTimeInMillis()}));
	}

	lastqueryxyz.set(vxyz);
	lastqueryt = lbTimeCurrent;

	count = new Long(count.longValue() + 1L);

	// Save context variables
	context.put(LASTQUERYT, lastqueryt);
	context.put(LASTQUERYXYZ, lastqueryxyz);
	context.put(FWDRECALC2SD, fwdrecalc2sd);
	context.put(BWDRECALC2SD, bwdrecalc2sd);
	context.put(COUNT, count);

	// No need to write TFWD into context because
	// computeTemporalArrow() does that.

	return answer;
}

// CalcDB

public boolean initialize()
throws java.util.prefs.BackingStoreException
{
	Set cos = createCalcObjects();
	for (Iterator i = cos.iterator(); i.hasNext(); ) {
		CalcObject co = (CalcObject)i.next();
		if (!_cos.contains(co)) {
			_cos.add(co);
		}
	}

	AbstractCalcDB._LOG.info(new Strings(new Object[] {
		"Added ", ""+_cos.size(), "/"+cos.size(), " CalcObjects."}));

	return true;
}

// AbstractCalcDB

public abstract Set createCalcObjects();

// Utility

private final boolean computeTemporalArrow(Map context, Calendar oldt, Calendar newt)
{
	// Expect TFWD and COUNT to be initialized already
	Boolean tfwd = (Boolean)context.get(TFWD);
	Long count = (Long)context.get(COUNT);
	boolean newtfwd;

	if (oldt.after(newt)) {
		newtfwd = false;
	} else {
		newtfwd = true;
	}

	boolean changed = false;
	if ((newtfwd != tfwd.booleanValue()) || (count.longValue() == 0)) {
		changed = true;
	}

	context.put(TFWD, Boolean.valueOf(newtfwd));
	
	return changed;
}

private final Calendar loopCond(Boolean tfwd, TreeMap recalc2sd, Calendar time)
{
	if (recalc2sd.size() == 0) {
		return null;
	}

	Calendar firsttime = (Calendar) recalc2sd.firstKey();
	return (tfwd.booleanValue() ?
		(firsttime.before(time) ? firsttime : null) :
		(firsttime.after(time) ? firsttime : null));
}

//@ ensures \result != null;
static Vector3d rpt2xyz(/*@ non_null */ Vector3d rpt, /*@ non_null */ Vector3d xyz)
{
	double r = rpt.x; // elev
	double phi = Math.toRadians(rpt.y); // lat
	double theta = Math.toRadians(rpt.z); // lon
	double cos_theta = Math.cos(theta);
	double sin_theta = Math.sin(theta);
	double cos_phi = Math.cos(phi);
	double sin_phi = Math.sin(phi);
	double r_cos_phi = r * cos_phi;

	xyz.set(r_cos_phi * sin_theta,
		r * sin_phi,
		r_cos_phi * cos_theta);

	return xyz;
}
};
