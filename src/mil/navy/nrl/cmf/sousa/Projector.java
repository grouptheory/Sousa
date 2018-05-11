package mil.navy.nrl.cmf.sousa;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import mil.navy.nrl.cmf.norm4j.NormNodeId;
import mil.navy.nrl.cmf.norm4j.NormSession;
import mil.navy.nrl.cmf.sousa.util.Strings;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * The Projector contains all information about a single role
 * of the Entity as a server to a set of clients with the same QoS.
 */
public final class Projector implements Selectable.Handler {
    private static final Logger _LOG = 
	Logger.getLogger(Projector.class);
    
    // The ServerSideFSM that created this Projector.  It persists in
    // this Projector until all of the clients are gone.  It's used to
    // access the Nimbus and the Entity above this Projector.  In the
    // special case where this Projector isn't shared, it's used to
    // obtain the P2PChannel on which both data and control messages
    // flow to the client.
    private final ServerSideFSM _creator;
    
    // How many clients do we have?  When this number returns to zero,
    // the owning ServerSideFSM can dispose of the Projector.
    private int _numberOfClients = 0;
    
    // The Receptor dual of this Projector.  It is obtained by the
    // Projector's Entity from another Entity in response to the
    // creation of this Projector.  When the Entity obtains more than
    // one Receptor for this Projector, _receptor is the most recent
    // one to become ready.
    //
    // The Projector provides a List of the changes in _receptor's
    // State to ViewInterpreter.makeMessage().
    //
    // See Entity.projectorReadyNotification().
    // See Entity.receptorReadyIndication().
    // See ControlLogic.projectorReadyNotification().
    // See ControlLogic.receptorReadyIndication().
    private Receptor _receptor = null;
    
    // If _receptor is non-null then _receptorStateListener must also
    // be non-null.  It's used to collect the changes to _receptor's
    // State so they can be given to ViewInterpreter.makeMessage() as
    // parameters to the ViewInterpreters.
    private FieldListener _receptorStateListener;
    
    // _dc is a data channel.  If it's not null, then it must be a
    // SelectableNormSession.
    private SelectableNormSession _dc = null;
    
    // Session ID for _dc.  When _dc is first constructed, the session
    // is set to 0 by nextSession().  If we should desire to restart
    // _dc, we can indicate this to the receivers by creating a new
    // SelectableNormSession with a different session ID.  See
    // NormStartSender() in "NORM Developer's Guide", p. 24.
    private short _nextSession = -1;
    
    private final QoS _qos;
    
    // Set of ViewInterpreters.  Each one listens to a subset of the
    // fields of the AuthoritativeState.  _qos dictates the contents
    // of _viewInterpreters.
    private final Set _viewInterpreters = new HashSet();
    
    // buildSelectableSet populates this Set of ViewInterpreters
    // that claim they are interested in writing ("dirty");
    private final Set _dirtyViewInterpreters = new HashSet();
    
    // Command logic
    //
    // _methods maps the String signature of a Method into the Method.
    private final Map _methods = new HashMap(); // String->Method
    
    // _commandLogics are the String signatures of RMI methods
    // provided by the Entity.
    private final HashSet _commandLogics = new HashSet(); // String
    
    private final Executor _executor = new Executor();
    
	// DAVID: There must be a better seed than
	// System.currentTimeMillis().  Expect collisions.
	//
	Random _r = new Random();
	private final int _key = _r.nextInt();
	private final Serializer _serializer = new XORSerializer(_key);

    // the multicast channel
    //private SelectableNormSession _multicast;
    
    /**
     * Construct a new Projector, with a specific ProjectorState
     * within a given Nimbus. After this ctor is called, a Projector
     * exists, with the specified ProjectorState within a given
     * Nimbus.  The Projector knows about all of the interfaces
     * required by the QoS and makes them available for RMI.
     * ViewInterpreters that implement the CommandLogic interface are
     * also available for RMI.
     *
     * @param creator the FSM on the server which initiated the ctor
     * @param qos the Quality of Service required
	 * @throws IllegalArgumentException if any ViewInterpreters or
	 * CommandLogics share method signatures, if there is no
	 * implementation of an interface requested by the QoS, or if the
	 * QoS contains anything that is neither a ViewInterpreter nor a
	 * CommandLogic.
     */
    Projector(ServerSideFSM creator, QoS qos) 
	throws IllegalAccessException, 
	       IllegalArgumentException, 
	       InstantiationException,
	       InvocationTargetException,
	       NoSuchMethodException {
		_creator = creator;
		_qos = qos;
	
		// The next big block of code instantiates all of the
		// ViewInterpreters and CommandLogics whose Class Objects are
		// contained in qos.
		State authoritativeState = _creator._nimbus._entity.getState();
	
		if (_LOG.isEnabledFor(Level.DEBUG)) {
			_LOG.debug("Projector(): Authoritative state: BEGIN");
			authoritativeState.print();
			_LOG.debug("Projector(): Authoritative state: END");
		}
	
		// Construct each of the ViewInterpreters in QoS.  Attach each
		// as a listener to the Entity's State.  Anything that isn't a
		// ViewInterpreter must be a command logic.
		for (Iterator classes = qos.iterator(); classes.hasNext(); ) {
	    
			// If c implements any interfaces, then all of the public
			// methods of those interfaces are available for RMI.
	    
			// c is in one of four categories:
			// 1. Is both a ViewInterpreter and a CommandLogic
			// 2. Is only a ViewInterpreter
			// 3. Is only a CommandLogic
			// 4. Is neither a ViewIntepreter nor a CommandLogic
			//
			Class c = (Class)classes.next();
			ViewInterpreter interp = null;
	    
			if (ViewInterpreter.class.isAssignableFrom(c)) {
				// Use the constructor that has this signature.
				Class[] signature = new Class[] {State.class};
				// throws NoSuchMethodException
				Constructor ctor = c.getConstructor(signature);
		
				// throws the rest of the Exceptions
				interp = (ViewInterpreter)ctor.newInstance(new Object[] 
					{authoritativeState});
		
				// DAVID: The ViewInterpreter ctor calls
				// attachFieldListener() so it's not necessary to do
				// that here.
				//
				// Set fields = interp.getFields();
				// authoritativeState.attachFieldListener(fields, interp);
				_viewInterpreters.add(interp);
		
				_LOG.debug(new Strings(new Object[] 
					{"Adding to Executor: ", interp}));

				// TODO: Revisit all this iteration and see if it can
				// be done more efficiently.
		
				// DAVID: Throws IllegalArgumentException if interp
				// has a method whose signature duplicates one already
				// known to _executor.  We don't like this collision
				// of signatures because it makes it difficult to
				// predict which implementation will be in effect.
				_executor.add(interp);

				// Don't permit collision of signatures between
				// _executor and _commandLogics.
				Class[] interfaces = c.getInterfaces();
				for (int i=0; i < interfaces.length; i++) {
					Method[] methods = interfaces[i].getMethods();
					for (int j=0; j < methods.length; j++) {
						String sig = methods[j].toString();
						if (_commandLogics.contains(sig)) {
							throw new IllegalArgumentException("Duplicate signature: " + sig);
						}
					}
				}
			} else {
				// c is not a ViewInterpreter. If it's an interface,
				// call up to the Entity to see if there is an Object
				// that implements it.  If there is one, update
				// _commandLogics.
				//
				// Don't permit any collision of method signatures
				// within _commandLogics or between _executor and
				// _commandLogics.
				if (c.isInterface()) {
					// DAVID: lookupCommandLogic() returns a Set of
					// String (signatures)
					Set signatures = _creator._nimbus._entity.lookupCommandLogic(c);
					if (null != signatures) {
						for (Iterator i = signatures.iterator(); i.hasNext(); ) {
							String sig = (String)i.next();
							if ((! _executor.canExecute(sig)) &&
								(! _commandLogics.contains(sig))) {
								_commandLogics.add(sig);
							} else {
								throw new IllegalArgumentException("Duplicate signature: " + sig);
							}
						}
					} else {
						_LOG.warn(new Strings(new Object[] {
												  "Projector(): No implementation of interface ", c}));
						throw new IllegalArgumentException("No implementation of interface " + c);
					}
				} else {
					_LOG.warn(new Strings(new Object[] {
											  "Projector(): ", c, " is neither an interface nor a ViewInterpreter"}));
					throw new IllegalArgumentException(c + " is neither an interface nor a ViewInterpreter");
				}
			}
		}
	
		// Do I need a new NormSession for this Projector?  I do if
		// the session in the QoS is greater than or equal to zero.
	
		if (qos.getSession() >= 0) {
			long buff=SelectableNormSession.BUFF_SIZE;
			String buffSize = System.getProperty("norm.buffer.sender");
	    
			if(buffSize != null){
				buff = Long.parseLong(buffSize);
			}
	    
			// Create a new NormSession using the Entity's
			// NormInstance.  Use it to communicate with all of the
			// clients of this Projector.
			_dc = _creator._nimbus._entity.createMcastSession(_serializer);
			NormSession session = _dc.getSession();
			session.setRxPortReuse(true, true);
	    
			session.startSender(nextSessionID(), buff, 
								SelectableNormSession.SEGMENT_SIZE, 
								SelectableNormSession.BLOCK_SIZE, 
								SelectableNormSession.NUM_PARITY);
	    
			_LOG.warn(new Strings(new Object[] {
									  "Projector(): new NormSession ",
									  _dc.getAddress(), ":", new Integer(_dc.getPort())}));
		} else {
			// Use the control channel for communicating with the only
			// client ths Projector will ever have.
			_LOG.warn("Projector(): Using the control channel");
		}
    }
	
    
    /**
     * Pair this Projector with a Receptor.
     *
     * @param r the Receptor
     */
    void setReceptor(Receptor r) {
	// Clean up: detach the current FieldListener from _receptor's
	// State.
	if (null != _receptor) {
	    State s = _receptor.getState();
	    s.detachFieldListener(s.getFieldNames(),
				  _receptorStateListener);
	    _receptorStateListener = null;
	}
	
	_receptor = r;
	
	if (null != _receptor) {
	    State s = _receptor.getState();
	    _receptorStateListener = new FieldListener(s);
	    s.attachFieldListener(s.getFieldNames(),
				  _receptorStateListener);
	}
    }
    
    /**
     * Assign a ServerSideFSM to this Projector
     *
     * @param fsm the ServerSideFSM to be assigned to this Projector
     */
    final void addLeaf(ServerSideFSM fsm) {
	_numberOfClients ++;
    }
    
    /**
     * Remove a ServerSideFSM previously assigned to this Projector
     *
     * @param fsm the ServerSideFSM to be de-associated from this Projector
     */
    final void removeLeaf(ServerSideFSM fsm) {
	_numberOfClients --;
    }
    
    /**
     * Get the QoS of this Projector
     *
     * @return the QoS
     */
    final QoS getQoS() {
	return _qos;
    }
    
    /**
     * Get the number of clients to which this Projector is projecting.
     *
     * @return the number of clients
     */
    final int size() {
	return _numberOfClients;
    }
        
    /**
     * Stop the Projector and clean up in anticipation of garbage
     * collection.  This method requires that there are no leaves
     * (i.e. _serverSideFSMs.size() == 0).  After it is called the
     * ViewInterpreters no longer listen to fields of the
     * authoritative state.  _viewInterpreters, _methods, and
     * _commandLogic are empty.  The data channel, if there is one, is
     * closed.
     */
    final void stop() {
	// Detach all of the ViewInterpreters from _state
	State authoritativeState = _creator._nimbus._entity.getState();
	
	for (Iterator i = _viewInterpreters.iterator(); i.hasNext(); ) {
	    ViewInterpreter v = (ViewInterpreter)i.next();
	    
	    // DAVID: Do we always detach all fields?  Do we ever
	    // detach a proper subset of v.getFields()?
	    authoritativeState.detachFieldListener(v.getFields(), v);
	}
	
	if (null != _dc) {
	    _dc.close();
	    
	    // Return the mcast address and port to the Entity's
	    // pool of available addresses and ports.
	    _creator._nimbus._entity.returnMcastAddress(_dc.getAddress(),
							_dc.getPort());
	}
	
	_viewInterpreters.clear();
	_methods.clear();
	_commandLogics.clear();
    }
    
    /**
	 * Adds the data channel or the control channel to the
	 * SelectableSet.
	 *
     * If there is a data channel, add it to the SelectableSet for for
     * READ and ERROR.  Add it for WRITE if there are ViewInterpreters
     * that have something to write.
	 *
	 * If there is no data channel, add the control channel to the
	 * SelectableSet for WRITE if there are ViewInterpreters that have
	 * something to write. Don't add the control channel for READ or
	 * ERROR because ServerSideFSM's buildSelectableSet() does that
	 * already.
	 *
     * @param ss the SelectableSet
     */
    void buildSelectableSet(SelectableSet ss) {
	
	// Always read from the data channel.
	if (null != _dc) {
	    ss.addSelectable(_dc, SignalType.ERROR, this);
	    ss.addSelectable(_dc, SignalType.READ, this);
	}
	
	// Write iff a ViewInterpreter is dirty or if the Receptor
	// State FieldListener is dirty.  This reduces the CPU load.
	// It's likely that _dc or the control channel will be
	// writable.
	
	
	List receptorStateChanges = null;
	State receptorState = null;
	if (null != _receptorStateListener) {
	    receptorState = _receptor.getState();
	    State.ChangeMessage m = _receptorStateListener.makeMessage();
	    if (null != m) 
		receptorStateChanges = m.getMessages();
	}
	
	_dirtyViewInterpreters.clear();
	
	for (Iterator i = _viewInterpreters.iterator(); i.hasNext(); ) {
	    ViewInterpreter v = (ViewInterpreter)i.next();
	    
	    v.setDirtyParameterFields(receptorStateChanges);
	    
	    if (v.isDirty(receptorState)) {
		_dirtyViewInterpreters.add(v);
	    }
	}
	
	if (_dirtyViewInterpreters.size() > 0) {
	    if (null != _dc) {
		ss.addSelectable(_dc, SignalType.WRITE, this);
	    // System.out.println("Projector wants to write on " + _dc);

	    } else {
		// Fields might have changed.  With no mcast data channel,
		// we have to use the unicast control channel to transmit
		// the State.ChangeMessage.
		ss.addSelectable(_creator._cc, SignalType.WRITE, this);
	    }
	}
    }
       
    /**
     * Create a Receptor. Its State contains the current values of the
     * fields supplied by all ViewInterpreters.  The Receptor contains
     * a Renderer that supports the contentType of the Projector's QoS.
     */
    final Receptor newReceptor(String contentType) {
		State receptorState = new State();
		NormNodeId nodeID = null;	// Receptor mcast node id
		String address = null;		// Projector mcast address
		int port = -1;				// Projector mcast port
		State parameters = null;	// query parameters
	
		if (null != _receptor)
			parameters = _receptor.getState();
	
		// Copy all of the Receptor Fields from all of the
		// ViewInterpreters into the Receptor State.
	
		// DAVID: This might put the existing receptors slightly
		// behind the new Receptor.  This will correct itself the next
		// time handle() is called.
	
		//
		// This code is nearly identical to the code in handle()
		// DAVID: Let's call the pattern the Accumulator Pattern.
		for (Iterator i = _viewInterpreters.iterator(); i.hasNext(); ) {
			ViewInterpreter viewInterpreter = (ViewInterpreter)i.next();
	    
			// DAVID: Do I have to tell viewInterpreter that all of
			// its parameter fields are dirty?
			State.ChangeMessage currentValues = 
			    viewInterpreter.getCurrentFieldValues(parameters);

			if (currentValues==null) {
			    System.out.println("CurrentValues == NULL");
			    continue;
			}
			else if (currentValues.getMessages()==null) {
			    System.out.println("CurrentValues.getMessages() == NULL");
			    continue;
			}

			//
			// This code is nearly identical to the code in accumulate().
			//
			for (Iterator fields = currentValues.getMessages().iterator(); 
				 fields.hasNext(); ) {
				Field.ChangeMessage f = (Field.ChangeMessage)fields.next();
		
				//
				// This is where the code differs from accumulate()
				//
				receptorState.addField(ViewInterpreter.getQualifiedFieldName(viewInterpreter.getClass(), f._fname),
									   f._value);
			}
		}
	
		if (null != _dc) {
			address = _dc.getAddress();
			port = _dc.getPort();
			nodeID = _creator._nimbus._entity.createUniqueNodeId();
		}
	
		// Client interprets (null == _address) as "use the control
		// channel" for reading and writing messages to the server.
		Receptor answer = new Receptor(receptorState, address, port, nodeID, _key);

		// Build the Renderer if there is one.  It's OK to use obtain
		// the Content-Type from the Projector's QoS because
		// Content-Type is significant to the QoS.  That is, for the
		// FetchRequest's QoS to match that of this Projector, the
		// FetchRequest's QoS must contain the same Content-Type(s) as
		// the Projector's QoS.
		//
		// Should Content-Type ever become insignificant to the QoS,
		// newReceptor() will need to know the Content-Type from the
		// FetchRequests's QoS.
		if ((null != contentType) && (contentType.length() > 0)) {
			Class rendererClass = 
				_creator._nimbus._entity.lookupRenderer(contentType);
			Renderer r = null;

			if (null != rendererClass) {
				try {
					r = (Renderer)rendererClass.newInstance();
				} catch (IllegalAccessException ex) {
					_LOG.warn(new Strings(new Object[] 
						{"newReceptor(): error instantiating ", 
						 rendererClass, " : ",
						 ex}));
				} catch (InstantiationException  ex) {
					_LOG.warn(new Strings(new Object[] 
						{"newReceptor(): error instantiating ", 
						 rendererClass, " : ",
						 ex}));
				} catch (ExceptionInInitializerError ex) {
					_LOG.warn(new Strings(new Object[] 
						{"newReceptor(): error instantiating ", 
						 rendererClass, " : ",
						 ex}));
				} catch (SecurityException ex) {
					_LOG.warn(new Strings(new Object[] 
						{"newReceptor(): error instantiating ", 
						 rendererClass, " : ",
						 ex}));
				}

				answer.setRenderer(r);
			}
		}

		return answer;
    }
    
    //// Selectable.Handler
        
    /**
     * Handle IO on the data channel to the client(s)
     *
     * @param sel the Selectable
     * @param st the signal
     */
    public void handle(Selectable sel, SignalType st) {
	// TODO
	if (SignalType.WRITE == st) {
	    State receptorState = null;
	    State.ChangeMessage receptorStateChanges = 
		new State.ChangeMessage();
	    
	    if (null != _receptor) receptorState = _receptor.getState();
	    
	    for (Iterator i = _dirtyViewInterpreters.iterator(); i.hasNext();) {
		ViewInterpreter viewInterpreter = (ViewInterpreter)i.next();
		
		State.ChangeMessage changeMessage = 
		    viewInterpreter.makeMessage(receptorState);
		
		if (null != changeMessage)
		    accumulate(receptorStateChanges, viewInterpreter, 
			       changeMessage);
	    }
	    
	    if (receptorStateChanges.size() > 0) {
			sel.write(receptorStateChanges);
	    }
	}
    }
    
    //// Messages from the control channel
       
    /**
     * Delegate messages that arrived on the control channel to the
     * specialized handlers in the Projector.  Return their various
     * responses to the caller.  It is expected that a ServerSideFSM
     * will call handleMessage from its READY state implementation of
     * handleIndication().  No other callers are expected.
     *
     * @param msg handle the arrival of this message
     * @return the return message
     */
    public Serializable handleMessage(Serializable msg) {
	Serializable answer = null;
	
	if (msg instanceof MethodInvocationMessage) {
	    answer = (Serializable)handleRMI((MethodInvocationMessage)msg);
	} else {
	    _LOG.error(new Strings(new Object[] {
		"Projector.handleMessage(): unexpected ",
		msg}));
	}
	
	return answer;
    }
    
    //// RMI
        
    /**
     * Invoke the method requested by a client using RMI.  We should
     * consider making handleRMI be of type void instead of/
     * MethodResponseMessage.  Use _fsm.scheduleWrite(response) to
     * send the answer back to the client.
     *
     * @param m the invocation message
     * @return the response message
     */
    private MethodResponseMessage handleRMI(MethodInvocationMessage m) {
	MethodResponseMessage response = null;
	try {
	    Object val = null;
	    if (_executor.canExecute(m.getSignature())) {
		_LOG.debug(new Strings(new Object[] 
		    {"Executing ", m.getSignature()}));
		
		val =_executor.execute(m.getSignature(), m.getArgs());
	    } else if (_commandLogics.contains(m.getSignature())) {
		_LOG.debug(new Strings(new Object[] 
		    {"Entity Executing ", m.getSignature()}));
		
		val = _creator._nimbus._entity.execute(m.getSignature(), m.getArgs());
	    } else {
		_LOG.debug(new Strings(new Object[] 
		    {"No such method! ", m.getSignature()}));
		
		val = new NoSuchMethodException(m.getSignature());
	    }
	    
	    response = new MethodResponseMessage(m.getToken(), val);
	} catch (Exception ex) {
	    response = new MethodResponseMessage(m.getToken(), ex);
	}
	
	return response;
    }
    
    //// Utility
        
    /**
     * Utility method to instantiate an instance of derivedClass that
     * is assignable to an Object of type baseClass using the
     * derivedClass' constructor.  Choose the constructor according to
     * the signature[] using the formal args[].
     *
     * @param baseClass
     * @param derivedClass
     * @param signature
     * @param args
     * @return the Object
     */
    private final Object createObject(Class baseClass, Class derivedClass,
				      Class[] signature,
				      Object[] args) 
	throws IllegalAccessException, 
	       IllegalArgumentException, 
	       InstantiationException,
	       InvocationTargetException,
	       NoSuchMethodException {
	Object answer = null;
	
	if (baseClass.isAssignableFrom(derivedClass)) {
	    Constructor ctor = derivedClass.getConstructor(signature);
	    answer = ctor.newInstance(args);
	} else {
	    throw new InstantiationException(derivedClass.toString() + 
					     " is not assignable to " +
					     baseClass.toString());
	}
	
	return answer;
    }
    
    /**
     * Accumulate State.ChangeMessages
     *
     * @param accumulator the State.ChangeMessage which will contain the accumulated answer
     * @param viewInterpreter the ViewInterpreter generating the messages
     * @param changes the State.ChangeMessage to be accumulated into accumulator
     */
    private final void accumulate(State.ChangeMessage accumulator,
				  ViewInterpreter viewInterpreter,
				  State.ChangeMessage changes) {
	
	List fieldChangeMessages = changes.getMessages();
	
	for (Iterator i = fieldChangeMessages.iterator(); i.hasNext(); ) {
	    Field.ChangeMessage f = (Field.ChangeMessage)i.next();
	    Field.ChangeMessage g = 
		new Field.ChangeMessage(ViewInterpreter.getQualifiedFieldName(viewInterpreter.getClass(), f._fname),
					f._value);
	    
	    accumulator.addFieldChangeMessage(g);
	}
    }
    
    /**
     * Compute the next session ID.  Wrap around from max short to min
     * short if need be.
     *
     * @return the next ID
     */
    private final short nextSessionID() {
	short answer;
	if (_nextSession < Short.MAX_VALUE)
	    _nextSession ++;
	else _nextSession = Short.MIN_VALUE;
	
	answer = _nextSession;
	return answer;
    }
}
