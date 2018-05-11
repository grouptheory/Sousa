package mil.navy.nrl.cmf.sousa.spatiotemporal;

import mil.navy.nrl.cmf.sousa.util.ObjectFactory;

import java.util.Properties;

/**
   A factory of {@link Vector3d} that uses <CODE>Properties</CODE> as
   inputs.
 */
public final class Vector3dFactory implements ObjectFactory
{
	public Vector3dFactory() {};

	/**
	  Constructs a <CODE>Vector3d</CODE> from <CODE>p</CODE>.  Obtains
	  the <CODE>x</CODE>, <CODE>y</CODE>, and <CODE>z</CODE> values
	  from the properties <CODE>prefix</CODE>+".x",
	  <CODE>prefix</CODE>+".y", and <CODE>prefix</CODE>+".z",
	  respectively.  If any property is missing, its value is
	  <CODE>0.0</CODE>.

	  @throws IllegalArgumentException if a property is present but
	  does not have a value that is convertible into a double.
	*/
	public Object create(String prefix, Properties p)
		throws IllegalArgumentException 
	{
		String xVal = p.getProperty(prefix + ".x", "0.0");
		String yVal = p.getProperty(prefix + ".y", "0.0");
		String zVal = p.getProperty(prefix + ".z", "0.0");
		double x = 0.0;
		double y = 0.0;
		double z = 0.0;

		try {
			x = Double.parseDouble(xVal);
		} catch (NumberFormatException ex) {
			throw new IllegalArgumentException("Expecting a double for " + 
											   prefix + ".x but found " + 
											   xVal + " instead.");
		}

		try {
			y = Double.parseDouble(yVal);
		} catch (NumberFormatException ex) {
			throw new IllegalArgumentException("Expecting a double for " + 
											   prefix + ".y but found " + 
											   yVal + " instead.");
		}
			
		try {
			z = Double.parseDouble(zVal);
		} catch (NumberFormatException ex) {
			throw new IllegalArgumentException("Expecting a double for " + 
											   prefix + ".z but found " + 
											   zVal + " instead.");
		}

		return new Vector3d(x, y, z);
	}
}
