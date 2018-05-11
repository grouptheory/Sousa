// QoS.java
package mil.navy.nrl.cmf.sousa;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Specification of server quality of service to a client. The
 * contents are Classes that are assignable to either ViewInterpreter
 * and or CommandLogic.  The ViewInterpreter Classes will provide
 * values to the Receptor that the server sends back to the client.
 * The CommandLogic classes will implement the method interfaces at
 * the server for processing RMI calls from the client.
 */
public class QoS extends TreeSet {
    // A negative number for session indicates that the client is not
    // willing to share a Projector.  The client may be unwilling to
    // share because NORM is reliable mcast and a client in an mcast
    // group will be subject to the performance of the lowest leaf.
    // To avoid this, a client may find it advantageous to declime
    // multicast and to remain effectively unicast by refusing to
    // share its projector with other clients.
    private final int _session;
    private transient int _localUID = 0;

	// RFC2045 MIME Content-Type type/subtype string.  Describes the
	// Renderer needed to display the info at this QoS.
	private String _contentType = null;

    /**
     * A Comparator for QoS objects 
     */
    private static class ClassComparator implements Comparator, Serializable {
	
	// Throws ClassCastException
	public int compare(Object o1, Object o2) {
	    String s1 = ((Class)o1).toString();
	    String s2 = ((Class)o2).toString();
	    return s1.compareTo(s2);
	}
	
    }
    
    // TODO
    // A list or set of Classes of interfaces.
    
    /**
     * Make a new empty QoS object for session 0.
    */
    public QoS() {
	super(new ClassComparator());
	_session = 0;
    }
    
    /**
     * Make a QoS for a specific session.
    */
    public QoS(int session) {
	super(new ClassComparator());
	_session = session;
    }
    
    /**
     * Make a QoS object for a given session using a collection of
     * interface classes.  Request a Renderer for the contentType.
	 */
    // Throws ClassCastException
    public QoS(int session, Collection c, String contentType) {
	super(new ClassComparator());
	addAll(c);
	_session = session;
	_contentType = contentType;
    }
    
    /**
     * Get the session number
     * @return the session number as integer
    */
    public int getSession() {
	return _session;
    }
    
    /**
     * Get the local UID
     * @return an integer local UID
    */
    int getLocalUID() {
	return _localUID;
    }
    
    /**
     * Set the local UID
     * @param localUID the new local UID
    */
    void setLocalUID(int localUID) {
	_localUID = localUID;
    }
    
	public final void setContentType(String contentType) {
		_contentType = contentType;
	}

	public final String getContentType() {
		return _contentType;
	}

    /**
     * get the QoS key:
     *
     * 1. Concatenate their String names of all Classes together in
     * order, separated by "|".
     * 2. Append the session.
     * 3. If the session is negative, append a "|" followed by the local UID.
     *
     * @return the key, or null if the QoS contains anything other than Class objects.
    */
    final Comparable getKey() {
	String answer = null;
	// DAVID: See toString() for a note on the use of
	// StringBuffer.
	StringBuffer buf = new StringBuffer();
	
	for (Iterator i = iterator(); i.hasNext(); ) {
	    // There shouldn't be anything but Class objects in
	    // this set.  The ClassComparator, above, should have
	    // taken care of that.
	    buf.append((Class)i.next());
	    if (i.hasNext())
		buf.append("|");
	}

	buf.append("|");
	buf.append(_session);
	
	if (_session < 0) {
	    buf.append("|");
	    buf.append(_localUID);
	}
	
	answer = buf.toString();
	
	return answer;
    }
    
    // java.lang.Object
    
    /**
     * read a QoS from a stream
     *
     * @param in the input stream
    */
    private void readObject(ObjectInputStream in)
	throws IOException, ClassNotFoundException
    {
	//@ assume in != null;
	in.defaultReadObject();
	
	_localUID = 0;
    }
    
    /**
     * convert a QoS to a String
     *
     * @return the string representation
    */
    public String toString() {
	// DAVID: This is the right way to use StringBuffer for
	// concatentation.  You have to build the String in pieces.
	// There is no way to build it all at once using the '+'
	// operator.
	StringBuffer buf = new StringBuffer();
	
	buf.append("{");
	
	for (Iterator i = iterator(); i.hasNext(); ) {
	    // There shouldn't be anything but Class objects in this
	    // set.  The ClassComparator, above, should have taken
	    // care of that.
	    buf.append((Class)i.next());
	    if (i.hasNext())
		buf.append(", ");
	}
	
	buf.append("}");
	buf.append(" Content-Type:");
	buf.append(_contentType);

	return buf.toString();
    }
}
// QoS.java

