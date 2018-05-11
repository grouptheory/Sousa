package mil.navy.nrl.cmf.sousa.idol.util;

import java.awt.Dimension;
import java.io.Serializable;

/**
   ImageParams
 */
public final class ImageParams
implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	   _name
	 */
	/*@ non_null */ private final String _name;
	
	/**
	   _bounds
	 */
	private final double[] _bounds = new double[4];

	/**
	   _size
	 */
	private final Dimension _size = new Dimension();

	/**
	   _res
	 */
	private final double[] _res = new double[2];

	/**
	   _elevs
	 */
	private final double[] _elevs = new double[2];

// Constructors

/**
   ImageParams(String, double, double, double, double, Dimension)
   @methodtype ctor
   @param name .
   @param n .
   @param s .
   @param w .
   @param e .
   @param size .
 */
public
ImageParams(String name, double n, double s, double w, double e, Dimension size)
{
	this(name, n, s, w, e, size, 0.0, Double.MAX_VALUE);
}

/**
   ImageParams(String, double, double, double, double, Dimension, double, double)
   @methodtype ctor
   @param name .
   @param n .
   @param s .
   @param w .
   @param e .
   @param size .
   @param minelev .
   @param maxelev .
 */
public
ImageParams(String name, double n, double s, double w, double e, Dimension size, double minelev, double maxelev)
{
	this._name = name;

	_bounds[0] = n;
	_bounds[1] = s;
	_bounds[2] = w;
	_bounds[3] = e;

	_size.setSize(size);

	_res[0] = (n - s) / _size.height;
	_res[1] = (e - w) / _size.width;

	_elevs[0] = minelev;
	_elevs[1] = maxelev;
}

// java.lang.Object

public final int
hashCode()
{
	return name().hashCode();
}

public final boolean
equals(Object o)
{
	if (!(o instanceof ImageParams)) {
		return false;
	}

	return ((ImageParams)o).name().equals(name());
}

public final String
toString()
{
	return name() + "(" + north() + " " + west() + ", " + south() + " " + east() + " [" + rows() + ", " + columns() + "])";
}

// ImageParams

public final String
name()
{
	return _name;
}

public final double
north()
{
	return _bounds[0];
}

public final double
south()
{
	return _bounds[1];
}

public final double
west()
{
	return _bounds[2];
}

public final double
east()
{
	return _bounds[3];
}

public final int
rows()
{
	return _size.height;
}

public final int
columns()
{
	return _size.width;
}

public final double
nsres()
{
	return _res[0];
}

public final double
ewres()
{
	return _res[1];
}

public final double
minelev()
{
	return _elevs[0];
}

public final double
maxelev()
{
	return _elevs[1];
}
}; // ImageParams
