package mil.navy.nrl.cmf.sousa.idol.service.computeserver;

import mil.navy.nrl.cmf.sousa.spatiotemporal.Queryable;

public interface CalcDB extends Queryable
{
	public boolean initialize()
		throws java.util.prefs.BackingStoreException;
};
