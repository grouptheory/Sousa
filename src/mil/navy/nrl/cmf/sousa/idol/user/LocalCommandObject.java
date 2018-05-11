package mil.navy.nrl.cmf.sousa.idol.user;

import java.util.Calendar;
import mil.navy.nrl.cmf.sousa.ServerContact;
import mil.navy.nrl.cmf.sousa.spatiotemporal.Vector3d;

// DAVID: This is broken.  It doesn't make sense for everyone to
// implement scheduleFetch() and setUpdateInterval() Find another way!
public interface LocalCommandObject extends SetPosition, SetTime {
	public void scheduleFetch(ServerContact s, int session);
	public void setUpdateInterval(int newInterval);
	public void shutdown();
}
