package mil.navy.nrl.cmf.sousa.idol.service.trail;

import java.io.Serializable;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
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
   <CODE>TrailQueryable</CODE> returns a series of points that
   identify the client's travel history.  The trail grows to a fixed
   length.  Once it is at maximum length, adding a new point to the
   head of the trail removes one from the tail.  Think of it as a the
   client dropping a breadcrumb every step or so and a bird some
   number of steps behind the client eating one every time the client
   advances.
 */
public class TrailQueryable implements Queryable, Serializable
{
	private static final long serialVersionUID = 1L;

	protected static final Logger _LOG =
		Logger.getLogger(TrailQueryable.class);
	
	/**
	   Number of entries in the trail List.
	   <P>
	   <EM>TODO: Consider making this either a Property for the server or
	    a parameter set by the client.</EM>
	*/
	private static final int MAX_TRAIL_SIZE = 1000;

	public TrailQueryable() {
		_LOG.warn("TrailQueryable()");
	}
	
	// mil.navy.nrl.cmf.sousa.spatiotemporal.Queryable

	/**
	   Adds another point to the head of the trail.  If the trail is
	   at maximum length, removes a point from the tail of the trail.

	   <P>

	   This implementation knows about the following Strings in
	   <CODE>fieldNames</CODE>: {@link
	   mil.navy.nrl.cmf.sousa.spatiotemporal.QueryClientFields#POSITION_FIELDNAME},
	   mapname, type.

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
		Set currentResults = new HashSet();
		Vector3d currentLocation = 
			new Vector3d(llCurrent.x + widthCurrent.x / 2.0,
						 llCurrent.y + widthCurrent.y / 2.0,
						 llCurrent.z + widthCurrent.z / 2.0);

		// Trail of locations.  Each is a QueryResultHandle.
		LinkedList trail = (LinkedList)context.get("trail");
		if (null == trail) {
			trail = new LinkedList();
			context.put("trail", trail);
		}

		Vector3d previousLocation = (Vector3d)context.get("location");
		Long point = (Long)context.get("point");

		if ((null == previousLocation) || 
			(! previousLocation.equals(currentLocation))) {
			Map fields = new HashMap();
			QueryResultHandle result = null;
			QueryResultHandle previousResult = null;

			// Last location marker
			if (null != point) {
				point = new Long(point.longValue() + 1L);
			} else {
				point = new Long(1);
			}

			// DAVID: Consider making the trail size a parameter set
			// by the client.
			if (trail.size() > MAX_TRAIL_SIZE)
				previousResult = (QueryResultHandle)trail.removeFirst();

			String mapname = point.toString();
			fields.put(QueryClientFields.POSITION_FIELDNAME, currentLocation);
			fields.put("mapname", mapname);

			result = new QueryResultHandle(mapname.hashCode(), fields);

			// Update the context
			trail.add(result);
			context.put("point", point);
			context.put("location", currentLocation);

			// Answer!
			currentResults.add(result);

			// Changes!
			added.clear();
			removed.clear();
			
			added.add(result);
			if (null != previousResult)
				removed.add(previousResult);
		}

		return currentResults;
	}
}
