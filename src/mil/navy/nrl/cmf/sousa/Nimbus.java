// Nimbus.java
package mil.navy.nrl.cmf.sousa;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;
import mil.navy.nrl.cmf.sousa.util.Strings;
import org.apache.log4j.Logger;

/**
 * The Nimbus of an Entity contains all information about its roles as
 * a server.
 */
public final class Nimbus {
	protected static final Logger _LOG = Logger.getLogger(Nimbus.class);

    // the server side FSMs
    // P2PChannel->ServerSideFSM
    private final HashMap _sfsms = new HashMap();
    // the projectors, stored by QoS
    // Comparable->Projector
    // The Comparable is returned by the Projector's QoS.getKey()
    private final HashMap _projectors = new HashMap();
    // the Entity in which this Nimbus resides
    final Entity _entity;
    
    // local unique identifier for QoS.  As a QoS arrives, assign it
    // the value of _localUID then increment _localUID.
    private int _localUID = 0;
    
    // PURPOSE: Construct a Focus for an Entity e.
    // PRECONDITION: e!=null and e._nimbus==null.
    // POSTCONDITION: A Focus exists for use by Entity e.
    /**
     * Construct a Focus for Entity e
     *
     * @param e the owning Entity
     */
    Nimbus(Entity e) {
	_entity = e;
    }
    
    
    /**
     * Add a serverside FSM to this Nimbus on behest of the arrival of
     * a new client.  The P2PChannel cc has been obtained by the
     * P2PChannelFactory.  After this method call, a new serverside
     * FSM has been installed in the Nimbus, which manages all data on
     * the P2PChannel cc.
     *
     * @param cc the P2PChannel
     * @return the ServerSideFSM managing the connection to the new client
     */
    final ServerSideFSM addConnectionToClient(P2PChannel cc) {
	ServerSideFSM fsm = new ServerSideFSM(cc, this);
	_sfsms.put( cc, fsm );
	_entity.wakeup();
	return fsm;
    }
    
    
    /**
     * Remove a serverside FSM from this Nimbus because of the
     * departure of a client.  Before this method is called, the
     * ServerSideFSM fsm was obtained from this Nimbus by calling
     * addClient.  After this method is called, the ServerSideFSM fsm
     * is removed from this/ Nimbus.  If theProjector associated with
     * fsm has no more clients then it is removed from this Nimbus.
     *
     * @param fsm the FSM managing the connection to the Client
     */
    final void removeConnectionToClient(ServerSideFSM fsm) {
	Projector proj = fsm.getProjector();
	
	_sfsms.remove(fsm._cc);
	
	_LOG.debug(new Strings(new Object[]
		{"removeConnectionToClient(): proj=", proj}));
	
	if (null != proj) {
	    if (0 == proj.size()) {
		// There won't be a Projector without a QoS, so it's
		// not necessary to check that proj.getQoS() != null.
		_projectors.remove(proj.getQoS().getKey());
	    }
	}
	
	_entity.wakeup();
    }
    
    //// building the SelectableSet
        
    /**
     * Allow the FSMs to add their P2PChannels to the SelectableSet
     *
     * @param ss the SelectableSet
     */
    void buildSelectableSet(SelectableSet ss) {
	for (Iterator it=_sfsms.values().iterator(); it.hasNext(); ) {
	    ServerSideFSM sfsm=(ServerSideFSM)it.next();
	    sfsm.buildSelectableSet(ss);
	}
    }
        
    /**
     * Get a Projector for a specified QoS, making a new/ Projector if
     * needed.  A Projector is returned (newly made if necessary)
     * which can project fields as specified in qos.  Null is returned
     * if the QoS contains anything other than Class objects.
     *
     * @param qos the desired QoS for the Projector
     * @return the Projector
     */
    final Projector getProjector(QoS qos) {
	Projector proj = null;
	Comparable key = qos.getKey();
	_LOG.debug(new Strings(new Object[]
		{"Nimbus.getProjector(): key= ", key}));
	
	if (null != key) {
	    proj = (Projector) _projectors.get(key);
	}
	
	_LOG.debug(new Strings(new Object[]
		{"Nimbus.getProjector() returning ", proj}));
	return proj;
    }
    
    /**
     * Add a Projector to the Nimbus for a specified QoS
     *
     * @param key the QoS
     * @param p the Projector
    */
    final void addProjector(QoS key, Projector p) {
	_projectors.put(key.getKey(), p);
    }
    
    /**
     * Get another local UID for use in the Nimbus
     * @return the next available UID
    */
    final int nextLocalUID() {
	int answer = _localUID;
	_localUID++;
	return answer;
    }
}
