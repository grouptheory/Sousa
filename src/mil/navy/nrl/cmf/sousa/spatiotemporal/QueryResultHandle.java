// File: QueryResultHandle.java

package mil.navy.nrl.cmf.sousa.spatiotemporal;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
   A container for results returned by {@link
   Queryable#query(Vector3d, Vector3d, Calendar, Calendar, Set, Set,
   Set, Set, Set, Map)}.  Each item in the <CODE>QueryResultHandle</CODE>
   corresponds to a unique <CODE>String</CODE> key.  The values that
   correspond to the keys are <CODE>Serializable</CODE>.
*/
public class QueryResultHandle implements Serializable
{
	// DAVID: These variables want to be final but they cannot be
	// final because Skaringa, the XML derializing package can't
	// restore the value of any final variables.
	private int _id = 0;
	private HashMap _fields = new HashMap();

	// ctors

	// In another package, QueryResultHandles are serialized to and
	// deserialized from XML using the Skaringa library.  Skaringa
	// requires there to be a default constructor to do that.  No code
	// in SOUSA uses this default constructor.
	private QueryResultHandle() {
	}

	/**
	   Class constructor that takes the hash code of the QueryResultHandle
	   and the Map of String-Serializable pairs.

	   @param id the hash code of this QueryResultHandle
	   @param fields a Map with <CODE>String</CODE> as keys and
	   <CODE>Serializable</CODE> as values

	   @throws all of the Runtime exceptions that {@link
	   java.util.Map#putAll(Map)} throws.
	*/
	public QueryResultHandle(int id, Map fields)
	{
		_id = id;
		_fields.putAll(fields);
	}


	/**
	   Returns the <CODE>Serializable</CODE> associated with <CODE>name</CODE>.
 
	   @param name the key of some <CODE>Serializable</CODE> in this
	   QueryResultHandle
	   @return the <CODE>Serializable</CODE> associated with <CODE>name</CODE>
	   @throws all of the Runtime exceptions that {@link
	   java.util.Map#get(Object)} throws
	*/
	public Serializable getFieldValue(String name)
	{
		return (Serializable)_fields.get(name);
	}


	/**
	 * fieldNameIterator()
	 * @return an Iterator over the String keys in the QueryResultHandle.
	 * @methodtype convenience
	 */
	public Iterator fieldNameIterator()
	{
		return _fields.keySet().iterator();
	}

	// java.lang.Object
	/**
	   Two <CODE>QueryResultHandles<CODE> are equal iff their hash
	   codes are equal.

	   @param o another <CODE>QueryResultHandle</CODE>
	   @return <CODE>true</CODE> if the _id fields are equal;
	   <CODE>false</CODE> otherwise.
	 */
	public boolean equals(Object o) 
	{
		return (_id == ((QueryResultHandle)o)._id);
	}

	/**
	   Returns the hash code given to the constructor.

	   @return the hash code
	 */
	public int hashCode()
	{
		return _id;
	}

	public String toString()
	{
		StringBuffer buf = new StringBuffer();
	
		buf.append(super.toString());
		buf.append(" id:");
		buf.append(_id);
		buf.append(" fields: { ");
		for(Iterator i = fieldNameIterator(); i.hasNext();) {
			String name = (String)i.next();
			buf.append(name);
			buf.append(":");
			buf.append(_fields.get(name));
			buf.append(" ");
		}
		buf.append("}");
	
		return buf.toString();
	}

}
