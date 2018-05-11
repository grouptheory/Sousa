package mil.navy.nrl.cmf.sousa.idol.util;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
   StatusUpdateMessage
 */
public final class StatusUpdateMessage
implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	   _inprocess
	 */
	private String _inprocess;

	/**
	   _inqueue
	 */
	private List _inqueue = new LinkedList();

	/**
	   _indb
	 */
	private Set _indb = new HashSet();

// Constructors

/**
   StatusUpdateMessage(String, List, Set)
   @methodtype ctor
   @param inprocess .
   @param inqueue .
   @param indb .
 */
public
StatusUpdateMessage(String inprocess, List inqueue, Set indb)
{
	this._inprocess = inprocess;
	_inqueue.addAll(inqueue);
	_indb.addAll(indb);
}

// sousa.idol.util.StatusUpdateMessage

/**
   inprocess()
   @methodtype get
   @return String
 */
public final String
inprocess()
{
	return _inprocess;
}

/**
   inqueue()
   @methodtype get
   @return List
 */
public final List
inqueue()
{
	return _inqueue;
}

/**
   indb()
   @methodtype get
   @return Set
 */
public final Set
indb()
{
	return _indb;
}
}; // StatusUpdateMessage
