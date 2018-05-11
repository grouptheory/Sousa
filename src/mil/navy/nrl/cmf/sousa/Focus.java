// Focus.java
package mil.navy.nrl.cmf.sousa;

import java.util.HashMap;
import java.util.Iterator;
import mil.navy.nrl.cmf.sousa.util.Strings;
import org.apache.log4j.Logger;

/**
 * The Focus of an Entity contains all information about its roles as
 * a client.
 */
public final class Focus {
	protected static final Logger _LOG = Logger.getLogger(Focus.class);

    // the Entity within which this Focus resides.
    final Entity _entity;
    // the client side FSMs, map: P2PChannel->CFSM
    private final HashMap _cfsms = new HashMap();
    
    /*
     * Construct a Focus for an Entity e.
     * @param e the Entity owning this Focus
     */
    Focus(Entity e) {
	_entity = e;
    }
    
    /**
     * Add a clientside FSM to this Focus on behest of the connection to a server.
     *
     * @param cc the P2PChannel
     * @return the ClientSideFSM managing this connection to the server.
     */
    final ClientSideFSM addConnectionToServer(P2PChannel cc) {
	ClientSideFSM fsm = new ClientSideFSM(cc, this);
	_cfsms.put( cc, fsm );
	// fsm.scheduleWrite( new Integer(666) );
	_entity.wakeup();
	return fsm;
    }
    
    /**
     * Remove a client side FSM.  
     *
     * @param fsm the FSM to be removed
     */
    final void removeConnectionToServer(ClientSideFSM fsm) {
		_LOG.debug(new Strings(new Object[]
			{"removeConnectionToServer(): ", fsm}));
	_cfsms.remove(fsm._cc);
	_entity.wakeup();
    }
    
    //// building the SelectableSet
    
    /**
     * Allow FSMs to add their P2PChannels to the SelectableSet
     *
     * @param ss the SelectableSet to which P2PChannels are to be added
     */
    void buildSelectableSet(SelectableSet ss) {
	for (Iterator it=_cfsms.values().iterator(); it.hasNext(); ) {
	    ClientSideFSM cfsm=(ClientSideFSM)it.next();
	    cfsm.buildSelectableSet(ss);
	}
    }
}
// Focus.java

