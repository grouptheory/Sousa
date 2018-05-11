// Entity.java

package mil.navy.nrl.cmf.sousa;

import java.lang.reflect.InvocationTargetException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.rmi.server.UID;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import mil.navy.nrl.cmf.norm4j.NormNodeId;
import mil.navy.nrl.cmf.sousa.util.StackTrace;
import mil.navy.nrl.cmf.sousa.util.Strings;

import org.apache.log4j.Logger;

/**
 * An Entity is a sousa clerver (<EM>c</EM>lient and s<EM>erver</EM>).
 */
public final class Entity implements Selectable.Handler, Runnable {
    private static final Logger _LOG = 
	Logger.getLogger(Entity.class);
    
    // state of the Entity
    private final State _state;
    // data about client roles.
    private final Focus _focus = new Focus(this);
    // data about server roles.
    private final Nimbus _nimbus = new Nimbus(this);
    // autonomous logic.
    final ControlLogic _controlLogic;
    
    // QoS offerings that aren't available for command logic.  All
    // members are Class Objects.  Put Classes of ViewInterpreters
    // here.
    private final QoS _nonCommandLogicQoSClasses = new QoS();
    
    // timer management
    private final Clock _clock = new Clock(this);
    
    // contact point for incoming and outgoing connections from/to
    // clients [contains its own running thread]
    private final P2PChannelFactory _contactPort;
    
    // List of ServerContact, pending high-level connections
    final LinkedList _pendingConnections = new LinkedList();
    
    // _pendingRMI contains all of the SelectableFutureResults that have
    // neither value nor Exception.  They're all awaiting an answer
    // from a server.  buildSelectableSet() adds each one into a
    // SelectableSet, specifying the Entity ControlLogic as the
    // Selectable.Handler.
    // UID->SelectableFutureResult
    // DAVID: Tell why it's a Map
    private final HashMap _pendingRMI = new HashMap();
    
    // Use this RunnableNormInstance to create SelectableNormSessions for
    // Receptors and Projectors [contains its own running thread]
    final RunnableNormInstance _normInstance = new RunnableNormInstance(false);
    
    // Multicast address
    private final String _mcastAddress;
    
    // _mcastPort is the number of the next available port at
    // _mcastAddress.  It's incremented by nextPort() when _freePorts
    // is empty.
    private int _mcastPort;
    
    // _freePorts is a pool of unused port numbers.  It's populated by
    // returnPort(), which is called by returnMcastAddress(). It's
    // drained one at a time by nextPort(), which is called by
    // createMcastSession().
    private final LinkedList _freePorts = new LinkedList();
    
    // is the Entity running?
    private boolean _running = false;
    
    // the high-level Entity thread
    private final Thread _entity_mcastThread = new Thread(this, "Entity Multicast Thread");
    
    // the SelectableSet for high-level asynchronous event handling
    private final SelectableSet _ss = new SelectableSet();
    
    // _executor handles all of the RMI that isn't handled by a
    // ViewInterpreter in a Projector.
    private final Executor _executor = new Executor();

	// maps String->Class.
	// The String is an RFC2045 MIME Content-Type type/subtype string.
	// The Class is the Class of a Renderer for the Content-Type. 
	private final HashMap _contentTypeToRenderer = new HashMap();

    /** 
     * Constructor creates an Entity that will be listening for
     * contact on a given port.
     *
     * @param contactPort the number of the new connection listening port
     * @param mcastAddress the multicast address for this Entity
     * @param baseMcastPort the first port to use on the multicast address
     * @param state the authoritative State of this Entity
     * @param logic the ControlLogic behaviors of this Entity
     * @param qosClasses the Class objects to include in the quality
     * of service offered by this Entity
     * @param commandLogics the interfaces this Entity offers for
     * asynchronous RMI
	 * @param contentTypeToRendererClass a Map between the RFC2045
	 * MIME Content-Types this Entity offers and the Classes of
	 * Renderer applets it can create to draw them.
	 *
	 * @throws IllegalArgumentException if two Classes in
	 * commandLogics have methods with identical signatures
	 * @throws IOException if there is a problem in creating the
	 * contact port
     */
    public Entity(int contactPort, String mcastAddress, int baseMcastPort,
				  State state, ControlLogic logic,
				  List qosClasses, List commandLogics,
				  Map contentTypeToRendererClass) 
		throws IOException, IllegalArgumentException {

		if (null != commandLogics) {
			_LOG.debug("Adding to Executor");
			// Throws IllegalArgumentException if there are duplicate
			// signatures among the methods of the Classes in
			// commandLogics
			for (Iterator i = commandLogics.iterator(); i.hasNext();) {
				_executor.add(i.next());
			}
		}

		if (null != contentTypeToRendererClass)
			_contentTypeToRenderer.putAll(contentTypeToRendererClass);

		// TODO: Wrap the P2PChannelFactory instance in a
		// CryptoChanelFactory.  Negotiate crypto stuff over the
		// P2PChannels that come out of the P2PChannelFactory
		// instance.
		//
		// TODO: Carry the crypto params around with the P2PChannel so
		// that the SelectableNormSession can have crypto, too.
		//
		// TODO: The ServerSideFSM or the Projector must know when a
		// client leaves or joins so that it can cause the the shared
		// key to be changed.
		_contactPort = P2PChannelFactory.newInstance(contactPort, this);
		_entity_mcastThread.start();
		_mcastAddress = ((null == mcastAddress) ? 
						 mcastAddress : new String(mcastAddress));
		_mcastPort = baseMcastPort;
	
		if (null != state) {
			_state = state;
		} else {
			_state = new State();
		}
	
		if (null != logic) {
			_controlLogic = logic;
		} else {
			_controlLogic = new ControlLogic();
		}
	
		_controlLogic.setEntity(this);
		if (null != qosClasses) {
			this.addNonCommandLogicQoSClasses(qosClasses);
		}
    }
    
    
    /** 
     * Expose the Entity's contact address.  The QoS is the aggregate
     * of all qualities of service the Entity offers.  It can contain
     * a both ViewInterpreter Classes and the Classes of the
     * interfaces offered by the command logics.

	 BUG: includes only one Content-Type in the QoS.  If there is more
	 than one Content-Type, then you can't predict which one it will
	 be.
     */
    public final ServerContact getServerContact(/*TODO: String contentType*/) {
	ServerContact answer = null;
	try {
		String contentType = null;
		Iterator i = _contentTypeToRenderer.keySet().iterator();
		if (i.hasNext()) {
			contentType = (String)i.next();
		}

		// TODO: QoS has a Set of ContentType
	    QoS qos = new QoS(0, _nonCommandLogicQoSClasses, contentType);
	    qos.addAll(_executor.getInterfaces());
	    answer = new ServerContact(_contactPort.getInetAddress(),
				       _contactPort.getPort(), 
				       qos);
	}
	catch (UnknownHostException ex) {
	    _LOG.error(new Strings(new Object[] {
		"No ServerContact! ",
		StackTrace.formatStackTrace(ex)}));
	}
	
	return answer;
    }
    
    /** 
     * Expose the authoriative State of the Entity.
     * @return the authoritative State
     */
    public final State getState() {
	return _state;
    }
    
    /** 
     * Expose the Clock of the Entity.
     * @return the Clock
     */
    public final Clock getClock() {
	return _clock;
    }
    
    /** 
     * Return my contact port
     * @return the contact port number
     */
    int getContactPort() {
	return _contactPort.getPort();
    }
    

	/**
	 * Find the Class of the Renderer that corresponds to contentType.
	 *
	 * @param contentType an RFC2045 MIME Content-Type string of the
	 * form type/subtype.  It may be null.
	 *
	 * @return the Class of Renderer that corresponds to the
	 * Content-Type.  It must have a no-arg constructor.
	 */
	Class lookupRenderer(String contentType) {
		return (Class)_contentTypeToRenderer.get(contentType);
	}

	public Set getContentTypes() {
		return Collections.unmodifiableSet(_contentTypeToRenderer.keySet());
	}

    /** 
     * The main execution loop of an Entity
     */
    public final void run() {
	// start running
	_running = true;
	
	while (_running) {
	    
	    _ss.clearAllSelectables(SignalType.READ);
	    _ss.clearAllSelectables(SignalType.WRITE);
	    _ss.clearAllSelectables(SignalType.ERROR);
	    
	    // we are always interested in reading about incoming/outgoing
	    // connections established by the ContactPort
	    _ss.addSelectable(_contactPort, SignalType.READ, this);
	    
	    // we are interested in writing to the ContactPort when
	    // there are pending outgoing requests.
	    synchronized (_pendingConnections) {
		if (_pendingConnections.size() > 0) {
		    _ss.addSelectable(_contactPort, SignalType.WRITE, this);
		}
	    }
	    
	    // allow the Focus/Nimbus to add P2PChannels (from FSMs)
	    // as necessary
	    _focus.buildSelectableSet(_ss);
	    _nimbus.buildSelectableSet(_ss);
	    _controlLogic.buildSelectableSet(_ss);

	    // I deal with the result of RMI.  I delegate to
	    // ControlLogic.handle()
	    synchronized(_pendingRMI) {
		// DAVID: Do I have to check values() against null?
		for (Iterator i = _pendingRMI.values().iterator(); i.hasNext(); ) {
		    SelectableFutureResult r = (SelectableFutureResult)i.next();
		    _ss.addSelectable(r, SignalType.READ, this);
		    _ss.addSelectable(r, SignalType.ERROR, this);
		}
	    }
	    
	    // how long until the next timer
	    long selectTime = _clock.timeToNextAlarm();
	    
	    // wait for asynchronous events on one of the Selectable
	    int changed = _ss.select(selectTime);
	    
	    // handle the Selectables which have events
	    if (changed > 0) {
		// for each SignalType
		for (Iterator itst = SignalType.iterator(); itst.hasNext();) {
		    SignalType st = (SignalType)itst.next();
		    // for each Selectable
		    for (Iterator it = _ss.iterator(st); it.hasNext();) {
			Selectable sel = (Selectable)it.next();
			// get the Handler
			Selectable.Handler h = _ss.getHandler(sel, st);
			// delegate
			// System.out.println("Processing:"+sel+" sig:"+st+ " at: "+h);
			h.handle(sel, st);
		    }			
		}
	    }
	    
	    // handle timer expiration callbacks
	    _clock.processAlarms();
	}
	
	// TODO: Each Projector and each Receptor must call session.close().
	_normInstance.destroyInstance();
    }
    
    /** 
     * force a synchronous break out of select()
     */
    
    final void wakeup() {
	_ss.interrupt();
    }
    
    /** 
     * end the life cycle of an Entity.
     */
    public final void stop() {
	_running = false;
    }
    
    /** 
     * Register a result to an ARMI call
     * @param r the result
     */
    void register(SelectableFutureResult r) {
	synchronized (_pendingRMI) {
	    _pendingRMI.put(r.getUID(), r);
	}
    }
    
    /** 
     * Deregister an ARMI result
     * @param r the result
     */
    void deregister(SelectableFutureResult r) {
	synchronized (_pendingRMI) {
	    _pendingRMI.remove(r.getUID());
	}
    }
    
    /** 
     * Retrieve a FutureResult for a pending ARMI call
     * @param uid the UID of the ARMI call
     * @return the FutureResult
     */
    SelectableFutureResult getSelectableFutureResult(UID uid) {
	return (SelectableFutureResult)_pendingRMI.get(uid);
    }
    
    /** 
     * Receive notifications of client arrivals
     * @param fsm the server side FSM that is now ready
     */
    public void projectorReadyIndication(ServerSideFSM fsm) {
	_controlLogic.projectorReadyIndication(fsm);
    }
    
    /** 
     * Receive notifications of client departures
     * @param fsm the server side FSM that is now dead
     */
    public void projectorNotReadyIndication(ServerSideFSM fsm) {
	_controlLogic.projectorNotReadyIndication(fsm);
    }
    
    /** 
     * Receive notifications of server arrivals
     * @param fsm the client side FSM that is now ready
     */
    public void receptorReadyIndication(ClientSideFSM fsm) {
	_controlLogic.receptorReadyIndication(fsm);
    }
    
    /** 
     * Receive notifications of server departures
     * @param fsm the client side FSM that is now dead
     */
    // DAVID: Nothing calls receptorNotReadyIndication()
    public void receptorNotReadyIndication(ClientSideFSM fsm) {
	_controlLogic.receptorNotReadyIndication(fsm);
    }
    
    /** 
     * Determine if a fetch from a client should be accepted
     * @param msg   the fetch message
     * @return true iff the ControlLogic admits the client
     */
    public boolean admitClient(ClientSideFSM.FetchRequest msg) {
	return _controlLogic.admitClient(msg);
    }
    
    /** 
     * Receive notifications of changes in server state
     * @param r the Receptor whose state has changed
     */
    public void receptorStateChangeIndication(Receptor r) {
	_controlLogic.receptorStateChangeIndication(r);
    }
    
    
    //// NormSession actions
    
    /** 
     * Create a new SelectableNormSession for some InetAddress:port
     * using Serializer <CODE>s</CODE>.
	 *
	 * @param s the Serializer
     */
    public SelectableNormSession createMcastSession(Serializer s) {
		_LOG.warn(new Strings(new Object[] 
			{"Entity.createMcastSession(", _mcastAddress, ", ",
			 new Integer(_mcastPort), ")"}));
	
		int port = nextPort();
	
		SelectableNormSession answer =
			new SelectableNormSession(_normInstance, 
									  _mcastAddress, port, 
									  createUniqueNodeId(),
									  s);
	
		return answer;
    }
    

    /** 
     * get the multicast address of this Entity
     */
    void returnMcastAddress(String address, int port) {
		_LOG.debug(new Strings(new Object[]
			{"Entity.returnMcastAddress(", address, ":", new Integer(port), ")"}));
		returnPort(port);
    }
    
    /** 
     * Create a NormNodeId that is unique to this host and JVM. Uses
     * the hash code of a UID to create a unique NormNodeId.
     * @return a NormNodeId
     */
    public NormNodeId createUniqueNodeId() {
	UID unique = new UID();
	return new NormNodeId(unique.hashCode());
    }
    
    /** 
     * Return the number of the next unused mcast port.
     * @return                 the next unused multicast port
     */
    private int nextPort() {
	int answer = -1;
	if (_freePorts.size() > 0) {
	    Integer i = (Integer)_freePorts.removeFirst();
	    answer = i.intValue();
	} else {
	    answer = _mcastPort;
	    _mcastPort ++;
	}
	
	return answer;
    }
    
    /** 
     * return port to the pool of unused mcast ports.
     * @param port  the port to be returned
     */
    private void returnPort(int port) {
	_freePorts.addLast(new Integer(port));
    }
    
    //// schedule actions
    
    /** 
     * A static factory method which creates new FetchRequest objects.
     *
     * @param addr the address of the server to contact
     */
    // PURPOSE: Enqueue an outgoing connection request which will be
    // executed once the ContactPort becomes writable.
    public void scheduleConnectTo(ServerContact addr) {
	synchronized (_pendingConnections) {
	    _pendingConnections.addLast(addr);
	}
    }
    
    /** 
     * Tell the Entity which qualities of service it supports that are
     * not provided by one of the Command Logic Objects.  Use
     * addCommandLogicQoSClasses to tell the Entity about the
     * ViewInterpreters, for example.  
     * @see #addNonCommandLogicQoSClasses(Collection)
     * @see #getServerContact()
     * @param c a Collection of Class Objects
     */
    public void addNonCommandLogicQoSClasses(Collection c) {
	_nonCommandLogicQoSClasses.addAll(c);
    }
    
    //// CommandLogic
    
    
    /** 
     * Return the Set of String method signatures in Class c. The Executor must know about c.
     * @param c   the Class
     * @return    a set of Strings representing signatures
     */

    Set lookupCommandLogic(Class c)
    {
	return _executor.getSignatures(c);
    }
    
    /** 
     * Execute a method with a given signature using an array of
     * arguments, by delegating to some CommandLogic
     *
     * @param signature  the method signature
     * @param args the arguments
     * @return the return value
     */
    Object execute(String signature, Object[] args) 
	throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
	return _executor.execute(signature, args);
    }
    
    //// Selectable.Handler
    
    /* 
     * Handle a signal to a Selectable of interest
     * @param sel the Selectable
     * @param st the signal
     */
    public void handle(Selectable sel, SignalType st) {
	// the ContactPort is readable
	if ((sel == _contactPort) && (st == SignalType.READ)) {
	    P2PChannel ch = (P2PChannel)_contactPort.read();
	    // ch is non-null since handle is only called when
	    // select() indicates that the ContactPort is readable.
		_LOG.debug(new Strings(new Object[]
			{"Connection established: ", ch}));
	    
	    // is this a connection to a client?
	    if (ch.getServerContact() == null) {
		_nimbus.addConnectionToClient(ch);
	    }
	    // or is this a connection to a server?
	    else {
		_focus.addConnectionToServer(ch);
	    }
	}
	// the ContactPort is writable
	else if ((sel == _contactPort) && (st == SignalType.WRITE)) {
	    synchronized (_pendingConnections) {
		ServerContact addr = (ServerContact)_pendingConnections.removeFirst();
		// addr is non-null, since we are only interested in
		// writing to the ContactPort if pendingConnections is
		// non-empty
		_LOG.debug(new Strings(new Object[]
			{"Attempting connection: ", addr}));
		// contactPort is writable since handle is called
		// after a select() indicates that the ContactPort is
		// writable.
		_contactPort.write(addr);
	    }
	} else if (sel instanceof SelectableFutureResult) {
	    deregister((SelectableFutureResult)sel);
	    _controlLogic.handle(sel, st);
	} else {
	    _LOG.error(new Strings(new Object[]
			{sel, " for ", st, " WHY AM I INTERESTED IN THIS?"}));
	}
    }
}

// Entity.java
