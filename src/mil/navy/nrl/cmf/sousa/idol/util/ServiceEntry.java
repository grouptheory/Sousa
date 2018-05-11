package mil.navy.nrl.cmf.sousa.idol.util;

import java.io.Serializable;
import java.io.IOException;

import java.net.SocketAddress;

/**
   ServiceEntry
 */
public final class ServiceEntry
implements Serializable
{
	/**
	   _host
	 */
	/*@ non_null */ private final String _host;

	/**
	   _port
	 */
	private final int _port;

	/**
	   _principal
	 */
	/*@ non_null */ private final String _principal;

// Constructors

/**
   ServiceEntry(String, int, String)
   @methodtype ctor
   @param host .
   @param port .
   @param principal .
 */
public
ServiceEntry(/*@ non_null */ String host, int port, /*@ non_null */ String principal)
{
	this._host = host;
	this._port = port;
	this._principal = principal;
}

// mil.navy.nrl.cmf.idol.util.ServiceEntry

/**
   getHost()
   @methodtype get
   @return String
 */
public final String
getHost()
{
	return _host;
}

/**
   getPort()
   @methodtype get
   @return int
 */
public final int
getPort()
{
	return _port;
}

/**
   getPrincipal()
   @methodtype get
   @return String
 */
public final String
getPrincipal()
{
	return _principal;
}

// java.lang.Object

/**
   @see java.lang.Object#toString()
 */
public final String
toString()
{
	return _host;
}

/**
   @see java.lang.Object#hashCode()
 */
public final int
hashCode()
{
	return _host.hashCode();
}

/**
   @see java.lang.Object#equals(Object)
 */
public final boolean
equals(Object o)
{
	return ((o instanceof ServiceEntry) && (((ServiceEntry)o)._host.equals(_host)));
}
}; // ServiceEntry
