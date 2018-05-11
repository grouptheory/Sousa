package mil.navy.nrl.cmf.sousa.idol.service.computeserver;

import java.util.Calendar;
import mil.navy.nrl.cmf.sousa.spatiotemporal.QueryResultHandle;
import mil.navy.nrl.cmf.sousa.spatiotemporal.Vector3d;

public interface CalcObject 
{
	public int computeRecalculationInterval(Calendar time, Calendar lasttime, boolean first, Vector3d viewerpos);

	public QueryResultHandle convertToHandle();
}; // CalcObject
