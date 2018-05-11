// File: Strings.java

package mil.navy.nrl.cmf.sousa.util;

import java.rmi.dgc.VMID;
import java.rmi.server.UID;

/**
   Strings
*/
public final class Strings
{
	/**
	   EMPTY
	*/
	/*@ non_null */ private static final Object[] EMPTY = { "" };
	
	/**
	   BLANK
	*/
	/*@ non_null */ private static final String BLANK = "".intern();

	/**
	   _vals
	*/
	/*@ non_null */ private Object[] _values;

	// Constructors

	/**
	   Strings(Object[])
	   @methodtype ctor
	*/
	public
		Strings(/*@ non_null */ Object[] values)
	{
		_values = values;
	}

	/**
	   Strings()
	   @methodtype ctor
	*/
	public
		Strings()
	{
		_values = EMPTY;
	}

	// mil.navy.nrl.cmf.util.Strings

	/**
	   set(Object[])
	   @methodtype set
	*/
	public final void
		set(/*@ non_null */ Object[] values)
	{
		_values = values;
	}

	// java.lang.Object

	/**
	   @see Object#toString()
	*/
	public final String
		toString()
	{
		if (EMPTY == _values) {
			return BLANK;
		} else {
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < _values.length; i++) {
				sb.append(_values[i]);
			}
			return sb.toString();
		}
	}

	// static

	/**
	   decolonify(VMID)
	   @methodtype factory
	   @return String
	*/
	//@ ensures \result != null;
	public static final String
		decolonify(/*@ non_null */ VMID vmid)
	{
		return decolonify(String.valueOf(vmid));
		//return String.valueOf(vmid).replace(':', '_');
	}

	/**
	   decolonify(UID)
	   @methodtype factory
	   @return String
	*/
	//@ ensures \result != null;
	public static final String
		decolonify(/*@ non_null */ UID uid)
	{
		return decolonify(String.valueOf(uid));
		//return String.valueOf(uid).replace(':', '_');
	}

	// static utility

	/**
	   decolonify(String)
	   @methodtype factory
	   @return String
	*/
	//@ ensures \result != null;
	private static final String
		decolonify(/*@ non_null */ String s)
	{
		char[] c = new char[s.length()];
		int j = 0;
		for (int i = 0; i < c.length; i++) {
			char c2 = s.charAt(i);
			if (Character.isLetterOrDigit(c2)) {
				c[j++] = c2;
			}
		}

		return new String(c, 0, j).toUpperCase();
	}
}; // Strings
