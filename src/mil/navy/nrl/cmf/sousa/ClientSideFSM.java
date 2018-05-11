

package mil.navy.nrl.cmf.sousa;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.lang.reflect.Proxy;

import mil.navy.nrl.cmf.norm4j.NormIOException;
import mil.navy.nrl.cmf.sousa.util.Strings;

import org.apache.log4j.Logger;

/**
 * ClientSideFSM contains all information about a single instance of
 * the Entity's role as a client.  ClientSideFSM extends EntityFSM.
 *
 * A ClientSideFSM object includes the following state information:
 * <ul>
 * <li> Focus
 * <li> Receptor
 * <li> (static) INIT_STATE
 * <li> (static) PENDING_STATE
 * <li> (static) READY_STATE
 * </ul>
 * <p>
 *
 * The STATE data members are static singletons representing logical
 * states within the finite state machine.
 *
 * The ClientSideFSM is delegated to by the Focus in order to build
 * its SelectableSet (via buildSelectableSet).  Later, when some
 * Selectable becomes triggered for I/O, the Focus delegates the
 * responsibility of addressing the event via its handle() method.
 * 
 * @version 	%I%, %G%
 * @see     EntityFSM
 * @author 	Bilal Khan
 * @author 	David Talmage
 */

public final class ClientSideFSM extends EntityFSM {
    private static final Logger _LOG = 
	Logger.getLogger(ClientSideFSM.class);
    
    /** FetchRequest is a request by a client Entity to connect to a 
     * server Entity.
     *
     * A FetchRequest object includes the following state information:
     * <ul>
     * <li> QoS -- quality of service requested from the server
     * <li> ServerContact -- address of the server
     * </ul>
     * <p>
     *
     * @version 	$Id: ClientSideFSM.java,v 1.12 2007/04/12 21:30:04 talmage Exp $
     * @author 	Bilal Khan
     * @author 	David Talmage
     */
    static class FetchRequest extends EntityFSM.Request {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	final QoS _qos;
	final ServerContact _revContact;
	
	FetchRequest(QoS qos, ServerContact revContact) {
	    _qos = qos;
	    _revContact = revContact;
	}
    };
    
    /** 
     * A static factory method which creates new FetchRequest objects.
     *
     * @param qos              quality of service requested from the server
     * @param revContact       address of the client
     * @return                 new FetchRequest message
     */
    final EntityFSM.Request makeFetchRequest(QoS qos, ServerContact revContact) {
		// DAVID: When we use SIP for signaling, consider making an
		// XLM document instead of a FetchRequest.

		return new FetchRequest(qos, revContact);
    }
    
    // Possible states of a ClientSideFSM: [INIT_STATE], PENDING_STATE, READY_STATE.
    static final EntityFSM.State INIT_STATE = new EntityFSM.State() {
	    // handle IO by dispatch
	    public EntityFSM.State handle(Selectable sel, SignalType st, EntityFSM context) {
		return context.dispatch(sel, st, this);
	    }
	    // handle Timers
	    public EntityFSM.State handle(Clock.Alarm m, EntityFSM context) {
		EntityFSM.State next = this;
		ClientSideFSM con = (ClientSideFSM)context;
		return next;
	    }
	    // handle incoming messages
	    public EntityFSM.State handleIndication(Serializable msg, EntityFSM context) {
		EntityFSM.State next = this;
		ClientSideFSM con = (ClientSideFSM)context;
		ClientSideFSM._LOG.debug(new Strings(new Object[] {"ind: ", msg}));
		return next;
	    }
	    // handle requests
	    public EntityFSM.State handleRequest(EntityFSM.Request req, EntityFSM context) {
		EntityFSM.State next = this;
		ClientSideFSM con = (ClientSideFSM)context;
		ClientSideFSM._LOG.debug(new Strings(new Object[] {"req: ",req}));
		if (req instanceof FetchRequest) {

			// DAVID: When we use SIP, if makeFetchRequest() doesn't
			// make XML Documents, then create one here as a container
			// for the FetchRequest.

			// DAVID: Better: Just put the QoS and the ServerContact
			// info into the XML Document.  Forget about
			// FetchRequests.
		    context.scheduleWrite( (ClientSideFSM.FetchRequest)req );
		    next = PENDING_STATE;
		}
		return next;
	    }
	    // print state
	    public String name() { return "ClientSideFSM.INIT_STATE"; }
	};
    
    // Possible states of a ClientSideFSM: INIT_STATE, [PENDING_STATE], READY_STATE.
    static final EntityFSM.State PENDING_STATE = new EntityFSM.State() {
	    // handle IO by dispatch
	    public EntityFSM.State handle(Selectable sel, SignalType st, EntityFSM context) {
		return context.dispatch(sel, st, this);
	    }
	    // handle Timers
	    public EntityFSM.State handle(Clock.Alarm m, EntityFSM context) {
		EntityFSM.State next = this;
		ClientSideFSM con = (ClientSideFSM)context;
		return next;
	    }
	    // handle incoming messages
	    public EntityFSM.State handleIndication(Serializable msg, EntityFSM context) {
		EntityFSM.State next = this;
		ClientSideFSM con = (ClientSideFSM)context;
		ClientSideFSM._LOG.debug(new Strings(new Object[] {"ind: ", msg}));

		//
		// DAVID: When we use SIP, extract the FetchResponse from the
		// SIP XML
		//
		if (msg instanceof ServerSideFSM.FetchResponse) {
		    con._receptor = ((ServerSideFSM.FetchResponse)msg)._receptor;
		    if (null != con._receptor) {
			try {
			    con._receptor.start(con, 
						con._focus._entity._normInstance);
			    next = READY_STATE;
			    con._focus._entity.receptorReadyIndication(con);
			} catch (NormIOException ex) {
			    ClientSideFSM._LOG.error(new Strings(new Object[] 
				{"Receptor.start() threw ", ex}));
			    // DAVID: There should be a DEAD_STATE
			    next = NULL_STATE;
			    con._focus.removeConnectionToServer(con);
			}
		    } else {
			// Server rejected the QoS.  Don't know what
			// it was.
			ClientSideFSM._LOG.error("ERROR: Server rejected fetch");
			next = NULL_STATE;
			con._focus.removeConnectionToServer(con);
			// TODO: cleanup
			// Remove this and the control channel from
			// the Focus' _cfsms.
			//
			// Close the control channel.
		    }
		}
		
		return next;
	    }
	    // handle requests
	    public EntityFSM.State handleRequest(EntityFSM.Request req, EntityFSM context) {
		EntityFSM.State next = this;
		ClientSideFSM con = (ClientSideFSM)context;
		ClientSideFSM._LOG.debug(new Strings(new Object[] {"req: ", req}));
		return next;
	    }
	    // print state
	    public String name() { return "ClientSideFSM.PENDING_STATE"; }
	};
    
    // Possible states of a ClientSideFSM: INIT_STATE, PENDING_STATE, [READY_STATE].
    static final EntityFSM.State READY_STATE = new EntityFSM.State() {
	    // handle IO by dispatch
	    public EntityFSM.State handle(Selectable sel, SignalType st, EntityFSM context) {
		return context.dispatch(sel, st, this);
	    }
	    // handle Timers
	    public EntityFSM.State handle(Clock.Alarm m, EntityFSM context) {
		EntityFSM.State next = this;
		ClientSideFSM con = (ClientSideFSM)context;
		return next;
	    }
	    // handle incoming messages
	    public EntityFSM.State handleIndication(Serializable msg, EntityFSM context) {
		EntityFSM.State next = this;
		ClientSideFSM con = (ClientSideFSM)context;
		ClientSideFSM._LOG.debug(new Strings(new Object[] {"ind: ", msg}));
		
		// DAVID: Ideally, I'd delegate to
		// con._receptor.handle().  There's no Selectable,
		// alas, so these indications have to go through
		// another method, handleMessage().
		Serializable response = con._receptor.handleMessage(msg);
		
		// Sometimes the Receptor has a reply for the client.
		if (null != response) {
		    con.scheduleWrite(response);
		}
		
		return next;
	    }
	    // handle requests
	    public EntityFSM.State handleRequest(EntityFSM.Request req, EntityFSM context) {
		EntityFSM.State next = this;
		ClientSideFSM con = (ClientSideFSM)context;
		ClientSideFSM._LOG.debug(new Strings(new Object[] {"req: ",req}));
		return next;
	    }
	    // print state
	    public String name() { return "ClientSideFSM.READY_STATE"; }
	};
    
    
    // the Focus in which this ClientStateFSM resides.
    final Focus _focus;
    
    // receptor received from the server
    private Receptor _receptor = null;
    
    /** 
     * Constructor
     *
     * @param cc            the P2P control channel
     * @param focus         the Focus managing this client side FSM
     */
    ClientSideFSM(P2PChannel cc, Focus focus) {
	super(cc);
	_focus = focus;
	_currentState = INIT_STATE;
	// DAVID: _cc is initialized by super(cc).  _cc.equals(cc).
	this.handleRequest(makeFetchRequest(_cc.getServerContact().getQoS(),
						_focus._entity.getServerContact()));
    }
    
    /** 
     * Make a proxy to a Receptor which implements a given interface
     *
     * @param c    the interface
     * @return     a Proxy to the Receptor which implements c
     */
    public Object genProxy(Class c) {
	return Proxy.newProxyInstance(c.getClassLoader(),
				      new Class[] {c},
				      this.getReceptor());
    }
    
    /** 
     * A getter method which returns contact information for the
     * remote server.  This method is useful for matching a Projector
     * to the Receptor that provides the parameters to its
     * ViewInterpreters.  When a Projector's
     *
     * ServerSideFSM.getClientContact() == getServerContact()
     *
     * the Projector can assume that the Receptor provides parameters
     * for its ViewInterpreters.
     *
     * @return                 ServerContact of the remote Server
     **/
    public final ServerContact getServerContact() {
	return _cc.getServerContact();
    }
    
    /** 
     * A getter method which returns the Receptor.
     * @return                 Receptor from the remote Server
     **/
    public final Receptor getReceptor() {
	return _receptor;
    }
    
   
    /** 
     * A command which disconnects this FSM from its focus, releases
     * the P2P connection, and deregisters the associate Receptor.
     */
    public final void deregister() {
	_focus.removeConnectionToServer(this);
	
	if ((_cc != null) && ( ! _cc.isClosed())) {
	    try {
		_cc.close();
	    }
	    catch (IOException ex) {
		_LOG.error(ex);
	    }
	}
	
	if (_receptor != null) {
	    Receptor receptor = _receptor;
	    _receptor = null;
	    receptor.deregister();
	}
    }
    
    //// package methods

    /** 
     * Augment a SelectableSet to include communication channels that
     * this FSM wishes to be notified about.
     * @param ss    the SelectableSet which is to be augmented.
     **/
    void buildSelectableSet(SelectableSet ss) {
	ss.addSelectable(_cc, SignalType.READ, this);
	ss.addSelectable(_cc, SignalType.ERROR, this);
	if (_outQ.size() > 0)
	    // there is something to write out
	    ss.addSelectable(_cc, SignalType.WRITE, this);
	
	if (_receptor != null) {
	    // ask the Receptor to add the data channel if needed
	    _receptor.buildSelectableSet(ss);
	}
    }
    
    /** 
     * Wake up the entity thread, thereby aborting any ongoing
     * select()
     **/
    final void wakeup() {
	_focus._entity.wakeup();
    }
    
    /** 
     * Attempt to kill this FSM (typically when a deathWarrant has
     * been issued)
     **/
    final void attemptKill() {
	if (_outQ.size() == 0) {
	    _focus.removeConnectionToServer(this);
	}
    }
}

// File: ClientSideFSM.java
