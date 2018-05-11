package mil.navy.nrl.cmf.sousa.util;

import java.util.Properties;

public interface ObjectFactory
{
	// Use Properties whose names begin with the prefix to construct
	// an Object.  E.g. create("idol.initializer", p) uses the
	// Properties of p whose names begin with "idol.initializer" to
	// construct the Object.
	public Object create(String prefix, Properties p) 
		throws IllegalArgumentException;
}
