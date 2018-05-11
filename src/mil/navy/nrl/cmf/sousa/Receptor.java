package mil.navy.nrl.cmf.sousa;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.rmi.server.UID;
import java.util.HashMap;
import mil.navy.nrl.cmf.norm4j.NormIOException;
import mil.navy.nrl.cmf.norm4j.NormNodeId;
import mil.navy.nrl.cmf.sousa.util.Strings;
import org.apache.log4j.Logger;

/**
 * A Receptor is a dynamic cached copy of server data, which
 * is maintained by multicast updates from the server.
 * 
 * @version 	$Id: Receptor.java,v 1.15 2006/11/16 14:29:12 bilal Exp $
 * @author 	Bilal Khan
 * @author 	David Talmage
 */
public final class Receptor 
    implements InvocationHandler, Selectable.Handler, Serializable {
    
    private static final Logger _LOG = 
	Logger.getLogger(Receptor.class);
    
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    // the data which is a cache copy of the server state.
    private final State _state;
    
    // the data channel
    private Selectable _dc = null;
    
    // 
    // If _projectorAddr is a multicast address, then the data channel
    // will be a NormSession.
    //
    // If _projectorAddr is a unicast address, then the data channel
    // will be the point-to-point control channel.
    private final String _projectorAddress;
    
    private final int _projectorPort;
    
    // Ignored if _projectorAddr isn't a multicast address.
    private final NormNodeId _nodeID;
    
    // the data channel
    // private DataChannel _dc;
    // the ClientSideFSM which owns this Receptor
    private ClientSideFSM _fsm = null;
    
    
	private final int _key;

	// Renders State.ChangeMessage contents into a Renderable.
	// _renderer may be null.
	private Renderer _renderer = null;

    /**
     * Construct a Receptor wrapping the data in the cache copy of the
     * server.
     *
     * @param state the a cached storage for a copy of the authoritative State of the server
     * @param projectorAddress a string representation of the projector address
     * @param projectorPort the port number
     * @param nodeID the multicast ID of the leaf
     */
    
    // PRECONDITION: state!=null.
    // POSTCONDITION: A new Receptor exists.
    Receptor(State state, String projectorAddress, int projectorPort, 
			 NormNodeId nodeID, int key) {
		_state = state;
		_projectorAddress = projectorAddress;
		_projectorPort = projectorPort;
		_nodeID = nodeID;
		_key = key;
    }
    
    /**
       get the State in this State()
       @return the cache copy of the server's state
    */
    public State getState() {
	return _state;
    }
    
    /**
       Get the QoS associated with this Receptor
       @return the QoS that this Receptor supports
    */
    public QoS getQoS() {
	return _fsm.getServerContact().getQoS();
    }
    
    /**
       get the ServerContact for the server that provides this Receptor
       @return the ServerContact for the server that provided this Receptor
    */
    public ServerContact getServerContact() {
	return _fsm.getServerContact();
    }
    
    
    /**
     * Start the Receptor at the client, to enable receipt of updates
     * from the server.
     *
     * @param fsm the FSM managing the connection at the client side
     * @param instance the Norm instance at the client
     */
    final void start(ClientSideFSM fsm, RunnableNormInstance instance)
		throws NormIOException {
		_fsm = fsm;
	
		// Use NORM for the data channel if the Receptor has a
		// NormNodeId.  Otherwise, delegate reading to the control
		// channel in the ClientSideFSM.
		if (null != _nodeID) {
			long buff = SelectableNormSession.BUFF_SIZE;
			String buffSize = System.getProperty("norm.buffer.receiver");
	    
			if(buffSize != null){
				buff = Long.parseLong(buffSize);
			}
	    
			_dc = new SelectableNormSession(instance,
											_projectorAddress,
											_projectorPort,
											_nodeID,
											new XORSerializer(_key));
			((SelectableNormSession)_dc).setRxPortReuse(true, true);
			((SelectableNormSession)_dc).getSession().startReceiver(buff);

			_LOG.debug(new Strings(new Object[] 
				{"start(): new NormSession ",
				 _projectorAddress, ":", new Integer(_projectorPort)}));
		}

		print();
    }

    /**
     * Add data channel to the SelectableSet as needed
     *
     * @param ss the SelectableSet
     */
    void buildSelectableSet(SelectableSet ss) {
	if (null != _dc) {
	    ss.addSelectable(_dc, SignalType.READ, this);
	}
	
    }
        
    /**
     * Disconnect this Receptor because the underlying data channel is
     * dead.
     */
    void deregister() {
	if ((_dc != null) && ( ! _dc.isClosed())) {
	    try {
		_dc.close();
	    }
	    catch (IOException ex) {
		_LOG.error(ex);
	    }
	}
	
	if (_fsm != null) {
	    ClientSideFSM fsm = _fsm;
	    _fsm = null;
	    fsm.deregister();
	}
    }
    
	public final void setRenderer(Renderer r) {
		_renderer = r;
	}

	public final Renderer getRenderer() {
		return _renderer;
	}

    //// Selectable.Handler
        
    /**
     * Handle reading of State.ChangeMessage on the data channel from
     * the server and MethodResponseMessage on the control channel
     * from the server.  Handle writing of MethodInvocationMessages on
     * the control channel to the server.
     *
     * @param sel the Selectable
     * @param st the signal
     */
    public void handle(Selectable sel, SignalType st) {
		if (SignalType.READ == st) {
			// sel is either the data channel or the control channel
			Object obj = sel.read();
	    
			if (obj instanceof State.ChangeMessage) {
	
				_state.applyMessage((State.ChangeMessage)obj);
		
				if (null != _renderer) {
					_renderer.render((State.ChangeMessage)obj);
				}

				// This informs the Entity of the State changes.
				_fsm._focus._entity.receptorStateChangeIndication(this);
		
			} else {
				_LOG.error(new Strings(new Object[] 
					{this, ": handle(", sel, ", ", st, 
					 "): read unexpected Object ", obj}));
			}
		} else {
			_LOG.error(new Strings(new Object[] 
				{this, ": handle(", sel, ", ", st, 
				 "): unexpected SignalType",}));
		}
	}
    
    //// RMI
    
    //// java.lang.reflect.InvocationHandler
    /**
     * Called by the Proxy to invoke a method on this Receptor,
     * triggering the ARMI
     *
     * @param proxy the calling Proxy
     * @param method the method called
     * @param args the arguments passed in
     * @return the return value, as a SelectableFutureResult
     */
    public final Object invoke(Object proxy, Method method, Object[] args)
	throws Throwable 
    {
	SelectableFutureResult result = new SelectableFutureResult();
	String signature = method.toString();
	
	MethodInvocationMessage m = 
	    new MethodInvocationMessage(signature, result.getUID(), args);
	
	// Put this in the control channel's outbound queue.
	_fsm.scheduleWrite(m);
	
	_fsm._focus._entity.register(result);
	
	return result;
    }
    
    /**
     * Delegate messages that arrived as Indications on the control
     * channel to the specialized handlers in the Receptor.  Return
     * their various responses to the caller.  It is expected that a
     * ClientSideFSM will call handleMessage from its READY state
     * implementation of handleIndication().  No other callers are
     * expected.
     *
     * @param msg the Serializable message to be handled
     * @return the response
     */
    public Serializable handleMessage(Serializable msg) {
	Serializable answer = null;
	
	if (msg instanceof MethodResponseMessage) {
	    setMethodResponse((MethodResponseMessage)msg);
	} else if (msg instanceof State.ChangeMessage) {
	    // System.out.println("Receptor.handleMessage() " + msg);
	    _state.applyMessage((State.ChangeMessage)msg);
	    
		// TODO: One renderer per RFC2045 MIME type
		if (null != _renderer) {
 			_renderer.render((State.ChangeMessage)msg);
		}

	    // This informs the Entity of the State changes.
	    _fsm._focus._entity.receptorStateChangeIndication(this);
	    
	} else {
		_LOG.warn(new Strings(new Object[] 
			{"Receptor.handleMessage(): unexpected ",
			 msg}));
	}
	
	return answer;
    }
    
    /**
     * Place the response or the Exception of a MethodResponseMessage
     * into the corresponding SelectableFutureResult, notifying
     * anything that is waiting on it.
     *
     * @param response the value to be returned
     */
    private void setMethodResponse(MethodResponseMessage response)
    {
	// DAVID: Consider requiring getToken() to return UID.
	Object token = response.getToken();
	
	_LOG.debug(new Strings(new Object[]
	    {"Looking for SFR with UID ", token}));
	
	SelectableFutureResult f  = 
	    _fsm._focus._entity.getSelectableFutureResult((UID)token);
	
	_LOG.debug(new Strings(new Object[]
	    {"Found ", f}));
	
	if (null != f) {
	    Exception error = response.getException();
	    
	    if (null != error) {
		f.setError(error);
	    } else {
		// If no error then whatever getResponse() returns is
		// the answer from the server.
		f.set(response.getResponse());
	    }
	}
    }
    
    /**
     * Print out information about this Receptor
     */
    void print() {
	System.out.println("Receptor(): State: BEGIN");
	_state.print();
	System.out.println("Receptor(): State: END");
    }
}
