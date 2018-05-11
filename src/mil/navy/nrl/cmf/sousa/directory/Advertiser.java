// File: AdvertiserViewInterpreter.java

package mil.navy.nrl.cmf.sousa.directory;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import mil.navy.nrl.cmf.sousa.*;
import mil.navy.nrl.cmf.sousa.spatiotemporal.*;

import org.apache.log4j.Logger;

/**
   A QoS interface through which a client can advertise itself.
*/
public interface Advertiser
{
	/**
	   Client advertises its contact address and the type of
	   information it offers for a period of time.

	  @param contact the contact address of the client
	  @param description text that briefly describes the information
	  offered by the advertiser
	  @param ttl the length of the advertisement in milliseconds

	  @return <CODE>null</CODE>
	*/
	public Object advertise(ServerContact contact, String description, Integer ttl);
};
