package mil.navy.nrl.cmf.sousa;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;

import mil.navy.nrl.cmf.sousa.util.Strings;

import org.apache.log4j.Logger;

/**
 * The ServerSideFSM contains all information about a single
 * role of the Entity as a server (to a single client).
 */
public final class ServerSideFSM extends EntityFSM {
    private static final Logger _LOG = 
	Logger.getLogger(ServerSideFSM.class);
    
    /**
     * The response to a fetch by a client.
     */
    static class FetchResponse extends EntityFSM.Response {
	private static final long serialVersionUID = 1L;
	final Receptor _receptor;
	FetchResponse(Receptor rec) {
	    _receptor = rec;
	}
    }
    
    
    // Possible states of a ServerSideFSM: [INIT_STATE], READY_STATE
    static final EntityFSM.State INIT_STATE = new EntityFSM.State() {
	    // handle IO by dispatch
	    public EntityFSM.State handle(Selectable sel, SignalType st, 
					  EntityFSM context) {
		return context.dispatch(sel, st, this);
	    }
	    // handle Timers
	    public EntityFSM.State handle(Clock.Alarm m, EntityFSM context) {
		EntityFSM.State next = this;
		ServerSideFSM con = (ServerSideFSM)context;
		return next;
	    }
	    // handle incoming messages
	    public EntityFSM.State handleIndication(Serializable msg, 
						    EntityFSM context) {
		EntityFSM.State next = this;
		ServerSideFSM con = (ServerSideFSM)context;
		ServerSideFSM._LOG.debug(new Strings(new Object[]{"ind: ", msg}));
		if (msg instanceof ClientSideFSM.FetchRequest) {
		    FetchResponse response = null;
		    ClientSideFSM.FetchRequest fmsg = (ClientSideFSM.FetchRequest)msg;
		    QoS qos = fmsg._qos;
		    
		    if (con._nimbus._entity.admitClient(fmsg)) {
			
				con._clientContact = fmsg._revContact;
			
				if (null != qos) {
					// Local UID is not relevant if the QoS permits
					// the sharing of Projectors.  The indication is
					// qos.getSession() >= 0.
					qos.setLocalUID(con._nimbus.nextLocalUID());
			    
					// TODO: Send FetchResponse(null) when the Nimbus
					// can't create a Projector for the QoS.
					con._proj = con._nimbus.getProjector(qos);
			    
					if (null == con._proj) {
						con._proj = con.createProjector(con, qos);
					}
			    
					if (null != con._proj) {
						// DAVID: Remember, ContentType isn't
						// significant to QoS.
						con._nimbus.addProjector(qos, con._proj);
						con._proj.addLeaf(con);

						// DAVID: That's why we have to tell the
						// Receptor the ContentType of the Renderer.
						Receptor rec = con._proj.newReceptor(qos.getContentType());
						response = new FetchResponse(rec);
						next = READY_STATE;
				
						// report READY_STATE to Entity
						con._nimbus._entity.projectorReadyIndication(con);
				
					} else {
						ServerSideFSM._LOG.error("Can't create Projector for QoS " + qos);
						response = new FetchResponse(null);
						next = NULL_STATE;
					}
				} else {
					// Shouldn't happen.  There must always be a QoS.
					ServerSideFSM._LOG.error("ERROR: No QoS in FetchRequest!");
					response = new FetchResponse(null);
					next = NULL_STATE;
				}
		    }
		    else {
			// Server ControlLogic rejects the Client
			ServerSideFSM._LOG.warn("Entity did not admit Client!");
			next = NULL_STATE;
			response = new FetchResponse(null);
		    }
		    
		    context.scheduleWrite( response );
		    if (next == NULL_STATE) {
			con.flushThenKill();
		    }
		}
		
		return next;
	    }
	    // handle requests
	    public EntityFSM.State handleRequest(EntityFSM.Request req, 
						 EntityFSM context) {
		EntityFSM.State next = this;
		ServerSideFSM con = (ServerSideFSM)context;
		ServerSideFSM._LOG.debug(new Strings(new Object[]{"req: ", req}));
		return next;
	    }
	    // print state
	    public String name() { return "ServerSideFSM.INIT_STATE"; }
	};
    
    // Possible states of a ServerSideFSM: INIT_STATE, [READY_STATE]
    static final EntityFSM.State READY_STATE = new EntityFSM.State() {
	    // handle IO by dispatch
	    public EntityFSM.State handle(Selectable sel, SignalType st, 
					  EntityFSM context) {
		return context.dispatch(sel, st, this);
	    }
	    // handle Timers
	    public EntityFSM.State handle(Clock.Alarm m, EntityFSM context) {
		EntityFSM.State next = this;
		ServerSideFSM con = (ServerSideFSM)context;
		return next;
	    }
	    // handle incoming messages
	    public EntityFSM.State handleIndication(Serializable msg, 
						    EntityFSM context) {
		EntityFSM.State next = this;
		ServerSideFSM con = (ServerSideFSM)context;
		ServerSideFSM._LOG.debug(new Strings(new Object[] {"ind: ", msg}));
		// Delegate msg to the Projector.
		Serializable reply = con._proj.handleMessage(msg);
		
		// Sometimes the Projector has a reply for the client.
		if (null != reply) {
		    con.scheduleWrite(reply);
		}
		
		return next;
	    }
	    
	    // handle requests
	    public EntityFSM.State handleRequest(EntityFSM.Request req, 
						 EntityFSM context) {
		EntityFSM.State next = this;
		ServerSideFSM con = (ServerSideFSM)context;
		ServerSideFSM._LOG.debug(new Strings(new Object[]{"req: ", req}));
		return next;
	    }
	    // print state
	    public String name() { return "ServerSideFSM.READY_STATE"; }
	};
    
    // the Nimbus in which this FSM resides
    final Nimbus _nimbus;
    // the Projector to which this FSM is (eventually) assigned
    private Projector _proj = null;
    
    // DAVID: _clientContact's QoS might be null.
    //
    // How to contact the client.  Set by handleIndication() of
    // INIT_STATE when a FetchRequest arrives.
    private ServerContact _clientContact = null;
    
    /**
     * Construct a ServerSideFSM to maintain state of a connection to
     * a given client (which connected to this Entity on a specified
     * Channel
     *
     * @param cc the control channel
     * @param nimbus the owning Nimbus
    */
    ServerSideFSM(P2PChannel cc, Nimbus nimbus) {
	super(cc);
	_nimbus = nimbus;
	_currentState = INIT_STATE;
    }
    
    /**
     * Request to disconnect the client.  Asynchronous, merely initiation.
    */
    public void disconnect_Request() {
	// this._currentState.handleRequest(SHUTDOWN_REQUEST, this);
	_LOG.error("disconnect_Request() hasn't been implemented!");
    }
        
    /**
     * Get the client contact address
     *
     * @return the client's contact address
    */
    public final ServerContact getClientContact() {
	// return _cc.getServerContact();
	return _clientContact;
    }
    
    //// package methods
    
    /**
     * Disconnect this SFSM because the underlying p2pchannel is dead.
     * Notify the Entity if this is the SFSM for the last/ client of
     * the Projector. PRECONDITION: The SFSM is in the READY state.
    */
    void deregister() {
		// DAVID: Must remove the leaf from the Projector before
		// removing the connection to client from the Nimbus in order
		// to maintain an accurate count of the Projector's leaves.
		// Doing so ensures that the Nimbus removes the Projector if
		// necessary.  The Nimbus won't remove the Projector unless
		// the Projector has no leaves.
		
		// _proj will be null if it wasn't given a value by one of the
		// FSM states.
		if (null != _proj) _proj.removeLeaf(this);

		_nimbus.removeConnectionToClient(this);
		_nimbus._entity.projectorNotReadyIndication(this);
	
		if ((null != _proj) && (0 == _proj.size())) {
			_LOG.debug("ServerSideFSM:deregister(): No more clients!");
			_proj.stop();
		}
    }
        
    /**
     * Add P2PChannels to the SelectableSet as needed
     *
     * @param ss the SelectableSet
    */
    void buildSelectableSet(SelectableSet ss) {
	ss.addSelectable(_cc, SignalType.READ, this);
	ss.addSelectable(_cc, SignalType.ERROR, this);
	if (_outQ.size() > 0)
	    ss.addSelectable(_cc, SignalType.WRITE, this);
	
	if (_proj != null) {
	    _proj.buildSelectableSet(ss);
	}
    }
        
    /**
     * wake up the Entity thread
    */
    final void wakeup() {
	_nimbus._entity.wakeup();
    }
        
    /**
     * Get the Projector
     *
     * @return the Projector associated with this FSM
    */
    final Projector getProjector() {
	return _proj;
    }
    
    /**
     * Create a Projector for this FSM, since we now know the desired QoS
     *
     * @param creator the FSM requesting the creation of a Projector
     * @param qos the desired QoS
     * @return the new Projector
    */
    final Projector createProjector(ServerSideFSM creator, QoS qos) {
	Projector proj = null;
	
	try {
	    proj = new Projector(creator, qos);
	} catch (IllegalAccessException ex) {
	    _LOG.error(ex);
	    proj = null;
	} catch (IllegalArgumentException ex) {
	    _LOG.error(ex);
	    proj = null;
	} catch (InstantiationException ex) {
	    _LOG.error(ex);
	    proj = null;
	} catch (InvocationTargetException ex) {
	    _LOG.error(ex);
	    proj = null;
	} catch (NoSuchMethodException ex) {
	    _LOG.error(ex);
	    proj = null;
	}
	
	return proj;
    }
    
    /**
     * Attempt to kill this FSM
    */
    final void attemptKill() {
	if (_outQ.size() == 0) {
	    _nimbus.removeConnectionToClient(this);
	}
    }
}
