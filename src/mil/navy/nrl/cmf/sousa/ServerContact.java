package mil.navy.nrl.cmf.sousa;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import mil.navy.nrl.cmf.sousa.util.HashCodeUtil;

/**
 * An address at which a server can be contacted
 */
public class ServerContact implements Comparable, Serializable {
    private InetAddress _addr;
    private final int _port;
    private final QoS _qos;
    private final String _canonicalHostName;
    
    /**
     * ServerContact(InetAddress, int, QoS)
     *
     * Creates a ServerContact for the specified InetAddress and port.
     * The ServerContact contains a request for a QoS.  
     * 
     * The InetAddress may be a wildcard address.  In that case the
     * canonical host name is determined.  Should the ServerContact be
     * serialized, the InetAddress of the canonical host name will be
     * substituted for the wildcard address when the ServerContact is
     * deserialized.
     * 
     * @methodtype ctor
     * @param addr the InetAddress at which the server is listening for new connections
     * @param port the port on which the server listens for new connections
     * @param qos the quality of service desired of the server
     * @throws UnknownHostException if there is no IP address for the local host.  This is a problem only when <CODE>addr</CODE> is a wildcard address.
     */
    public ServerContact(InetAddress addr, int port, QoS qos)
	throws UnknownHostException 
    {
	_addr = addr;
	_port = port;
	_qos = qos;
	
	if (_addr.isAnyLocalAddress()) {
	    // InetAddress.getLocalHost() throws java.net.UnknownHostException,
	    // an IOException
	    _canonicalHostName = InetAddress.getLocalHost().getCanonicalHostName();
	} else {
	    _canonicalHostName = null;
	}
    }
    
    /**
     * ServerContact(int, QoS)
     * 
     * Creates a ServerContact for the local host using
     * InetAddress.getLocalHost().  Beware that on a multihomed host
     * getLocalHost() might not return a routable address.
     * 
     * @methodtype ctor
     * @param port the port on which the server listens for new connections
     * @param qos the quality of service desired of the server
     * @throws UnknownHostException if there is no IP address for the
     * local host.
     */
    public ServerContact(int port, QoS qos) 
	throws UnknownHostException
    {
	// InetAddress.getLocalHost() throws
	// java.net.UnknownHostException, an IOException
	InetAddress addr = InetAddress.getLocalHost();
	
	_addr = addr;
	_port = port;
	_qos = qos;
	_canonicalHostName = null;
    }

    /**
     * get the host address
     * @return the internet address of the host
    */
    public InetAddress getHost() {
	return _addr;
    }
    
    /**
     * get the port of th server
     * @return the port number
    */
    public int getPort() {
	return _port;
    }
    
    /**
     * get the QoS that the server can be contacted for
     * @return the quality of service supported at the server
    */
    public QoS getQoS() {
	return _qos;
    }
    
    /**
     * compare two server contacts
     * @see java.lang.Comparable#compareTo(Object o)
     */
    public int compareTo(Object o) {
	int answer = 0;
	
	// This shortcut check for identity also ensures that o is a
	// ServerContact; it throws ClassCastException if o isn't a
	// ServerContact.
	if (this == (ServerContact)o) answer = 0;
	else {
	    ServerContact a2 = (ServerContact)o;
	    
	    // compare addresses first.  If they're both wildcard
	    // addresses, then compare the canonical host names.
	    // Otherwise, compare the addresses.
	    if (_addr.isAnyLocalAddress() && a2._addr.isAnyLocalAddress()) {
		answer = _canonicalHostName.compareToIgnoreCase(a2._canonicalHostName);
	    } else {
		answer = _addr.getHostAddress().compareTo(a2._addr.getHostAddress());
	    }
	    
	    if (0 == answer) { 
		// Addresses are equal.  Check ports.
		int p1 = this.getPort();
		int p2 = a2.getPort();
		if (p1 < p2) answer = -1;
		if (p1 > p2) answer = +1;
		
		if (0 == answer) {
		    // Ports are equal.  Check QoS size.
		    p1 = _qos.size();
		    p2 = a2._qos.size();
		    
		    if (p1 < p2) answer = -1;
		    if (p1 > p2) answer = +1;
		    
		    if ((0 == answer) && (p1 >0)) {
			// QoS sizes are equal and non-zero.  Compare
			// the names of the corresponding QoS members.
			// Remember, QoS contains Class Objects only.
			Iterator his = a2._qos.iterator();
			for (Iterator mine = _qos.iterator(); mine.hasNext(); ) {
			    // DAVID: Consider refactoring
			    // QoS.ClassComparator so that we can use
			    // it here.
			    
			    Class myClass = (Class)mine.next();
			    Class hisClass = (Class)his.next();
			    
			    answer = myClass.toString().compareTo(hisClass.toString());
			    
			    if (0 != answer) break;
			}
		    } // else QoS sizes are not equal or QoS sizes are
		    // both zero.  In either case, return answer.
		}
	    }
	}
	
	return answer;
    }
    
    // java.lang.Object
    
    /**
     * Two <CODE>ServerContacts</CODE> are equal if their addresses, ports, and
     * <CODE>QoS</CODE>es are equal.
     * 
     * Their addresses are equal if they are both wildcard addresses
     * and their canonical host names are equal and their ports are
     * equal
     * 
     * or
     * 
     * they are both not wildcards and they are equal according to
     * <CODE>InetAddr.equals()</CODE> and their ports are equal.
     * 
     * This implementation of <CODE>equals()</CODE> never declares
     * that an instance of <CODE>ServerContact</CODE> equals an instance of a
     * class derived from <CODE>ServerContact</CODE>.
     * 
     * @param obj a candidate for equality
     * @return <CODE>true</CODE> if this <CODE>ServerContact</CODE> equals <CODE>obj</CODE>.
    */
    public boolean equals(Object obj) {
	boolean answer = (this == obj); // shortcut for identity.
	
	if (! answer) {  // not identical
	    answer = ((obj != null) &&
		      (obj.getClass() == this.getClass()));
	    
	    if (answer) {
		answer = (_port == ((ServerContact)obj)._port);
		
		// Addresses are equal if either of these are true:
		//
		// 1. both are wildcard addresses and the canonical host names match
		// 2. neither are wildcard addresses and _addr.equals(obj._addr)
		//
		if (answer) {
		    boolean addrIsAnyLocalAddress = _addr.isAnyLocalAddress();
		    boolean objAddrIsAnyLocalAddress = 
			((ServerContact)obj)._addr.isAnyLocalAddress();
		    
		    answer = false;
		    
		    if (addrIsAnyLocalAddress && objAddrIsAnyLocalAddress) {
			// Ignore case in canonical host names because
			// DNS rules say that case is not significant.
			answer = _canonicalHostName.equalsIgnoreCase(((ServerContact)obj)._canonicalHostName);
		    } else if (!addrIsAnyLocalAddress && !objAddrIsAnyLocalAddress) {
			answer = _addr.equals(((ServerContact)obj)._addr);
		    }
		}
		
		if (answer) {
		    answer = _qos.equals(((ServerContact)obj)._qos);
		}
	    }
	}
	
	return answer;
    }
    
    /**
     * Computes the hash code using the rules from Effective Java.    
    */
    public int hashCode() {
	int answer = HashCodeUtil.SEED;
	
	answer = HashCodeUtil.hash(answer, _port);
	
	if (_addr.isAnyLocalAddress()) {
	    // Ignore case in the canonical host name as required by
	    // our implementation of equals() by converting it to
	    // lower case before computing the hash.
	    answer = HashCodeUtil.hash(answer, _canonicalHostName.toUpperCase());
	} else {
	    answer = HashCodeUtil.hash(answer, _addr);
	}
	
	answer = HashCodeUtil.hash(answer, _qos);
	
	return answer;
    }
    
    /**
     * fill in a ServerContact from an input stream
     * @param in the input stream
    */
    private void readObject(ObjectInputStream in)
	throws IOException, ClassNotFoundException
    {
	//@ assume in != null;
	in.defaultReadObject();
	
	// Substitute the InetAddress of the canonical host name for
	// the wildcard address originally given to the constructor.
	if (null != _canonicalHostName) {
	    _addr = InetAddress.getByName(_canonicalHostName);
	}
    }
    
    /**
     * Convert a server contact to a String
     * @return the string representation
    */
    public String toString() {
	return _addr.toString()+":"+_port + "{" + _qos + "}";
    }
}
