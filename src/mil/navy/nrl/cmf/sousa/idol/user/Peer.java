// File: Peer.java
package mil.navy.nrl.cmf.sousa.idol.user;

import mil.navy.nrl.cmf.sousa.ServerContact;

public class Peer implements Comparable
{
private final ServerContact _contactAddress;
private final String _description;

public Peer(/* non-null */ ServerContact contactAddress, 
			/* non-null */ String description) 
{
	_contactAddress = contactAddress;
	_description = description;
}

public ServerContact getContactAddress() 
{
	return _contactAddress;
}

public String getDescription() 
{
	return _description;
}

// java.util.Comparable

// Only the Address is significant when comparing two Peers.
public int compareTo(Object o) 
{
	int answer = 0;
	ServerContact his = ((Peer)o).getContactAddress();
	
	return _contactAddress.compareTo(his);
}

// java.lang.Object
public String toString() 
{
	String addr = (null != _contactAddress) ? _contactAddress.toString() : "null";
	String descr = (null != _description) ? _description : "null";

	return "Peer(" + _contactAddress + ", " + descr + ")";
}

}
