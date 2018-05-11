// ControlLogic.java

package mil.navy.nrl.cmf.sousa;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;
import mil.navy.nrl.cmf.sousa.util.Strings;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Specification the logic of an Entity
 */

// DAVID: Can we write a universal handle() for ControlLogic?  If not,
// make ControlLogic an abstract class.
public class ControlLogic implements Selectable.Handler {
    private static final Logger _LOG = 
	Logger.getLogger(ControlLogic.class);

    private Entity _entity;
    private boolean _running = true;

    // Facilitates the reverse fetch
    // ServerContact->ServerSideFSM
    private final HashMap _serverContactToServerSideFSM = new HashMap();

	// Projector->LinkedList of Receptors
	//
	// Used by addReceptor() and removeReceptor() to ensure that the
	// Projector always has a Receptor to supply it with query
	// parameters.
	private final HashMap _projector2Receptor = new HashMap();

    /*
     * Constructor
     */
    public ControlLogic() {
    }

    /** 
     * Introduce the ControlLogic to its enclosing Entity.
     * @param e the Entity in which this ControlLogic resides
     */
    protected void setEntity(Entity e) {
		_entity = e;
    }

    /* 
     * Expose the enclosing Entity to other ControlLogics
     * @return the Entity in which this ControlLogic resides
     */
    protected final Entity getEntity() {
		return _entity;

    }

    //// Selectable.Handler

    // What kinds of Selectables will ControlLogic want to read?
    //
    // SelectableFutureResult.  That's about it, I think.
    //
    // TODO: universal handle()

    /* 
     * Handle a signal to a Selectable of interest
     * @param sel the Selectable
     * @param st the signal
     */
    public void handle(Selectable sel, SignalType st) {
		if (SignalType.READ == st) {
			Object obj = sel.read();
			_LOG.debug(new Strings(new Object[] {"Read ", obj}));
		} else if (SignalType.ERROR == st) {
			_LOG.error(new Strings(new Object[] {
				sel, " threw exception ", sel.getError()}));
		} else if (SignalType.WRITE == st) {
			_LOG.error(new Strings(new Object[] {
				sel, " is writable. WHY AM I INTERESTED IN THIS?"}));
		}
    }

    /* 
     * Inform the Entity that it has a new client.  
     * @param fsm the FSM handling the connection to the new client 
     */
    
    public void projectorReadyIndication(ServerSideFSM fsm) {
		_LOG.debug(new Strings(new Object[] {"ServerState ready ", fsm, " on ", fsm.getClientContact()}));
		_serverContactToServerSideFSM.put(fsm.getClientContact(), fsm);
    }

    /* 
     * Inform the Entity that it has one fewer clients.
     * @param fsm the FSM handling the connection to the client 
     */
    
    public void projectorNotReadyIndication(ServerSideFSM fsm) {
		_LOG.debug(new Strings(new Object[] {"ServerState not ready ", fsm}));
		_serverContactToServerSideFSM.remove(fsm.getClientContact());
    }

    /**
     * Inform the Entity it has a connection to server.  Introduce the
     * Receptor to its Projector dual, if there is one.
	 *
     * @param fsm the FSM handling the connection to the server
     */
    public void receptorReadyIndication(ClientSideFSM fsm) {
		ServerSideFSM serverFSM =
			(ServerSideFSM)_serverContactToServerSideFSM.get(fsm.getServerContact());
		Receptor r = fsm.getReceptor();

		_LOG.debug(new Strings(new Object[] {"ClientState ready ", fsm, " scanning for ", fsm.getServerContact()}));
		_LOG.debug(new Strings(new Object[] {
			"Corresponding ServerSideFSM: ", serverFSM}));

		if (null != serverFSM) {
			Projector p = serverFSM.getProjector();
			addReceptor(p, r);
		}

		// This is the only way to give the Receptor's
		// Renderer a chance to render the initial values of
		// the Receptor's State.  It can't be done until after
		// receptorReadyIndication() because the ControlLogic
		// might change the Renderer.
		Set receptorFields = r.getState().getFieldNames();
		State.ChangeMessage changes = 
			r.getState().makeMessage(receptorFields);
		r.handleMessage(changes);

		// DAVID: This might be temporary.  It gives the ControlLogic
		// a chance to deal with the initial values in the Receptor's
		// State.  We don't have to do it this way.  We can make
		// ClientSideFSM.getReceptor() public instead and let the
		// ControlLogic developer deal with it.
		receptorStateChangeIndication(r);
    }

    /**
     * Inform Entity: connection to server disconnected. If the the
     * connection is the one supplying query parameters to a
     * Projector, then hook up the next one so that it can supply
     * query parameters. 
	 *
	 * @param fsm the FSM handling the connection to
     * the server
     */
    // DAVID: Nothing calls receptorNotReadyIndication()
    public void receptorNotReadyIndication(ClientSideFSM fsm) {
		_LOG.fatal(new Strings(new Object[] {
			"ClientState not ready ", fsm,
			" At last you call me?!  REMOVE COMMENT FROM SOURCE CODE"}));

		ServerSideFSM serverFSM =
			(ServerSideFSM)_serverContactToServerSideFSM.get(fsm.getServerContact());
		if (null != serverFSM) {
			Receptor r = fsm.getReceptor();
			Projector p = serverFSM.getProjector();
			removeReceptor(p, r);
		}
    }

    /* 
     * Ask if a fetch message should be responded to.  Should we
     * accept a client?
     * @param msg the Fetch message
     * @return boolean true iff we should accept the client
     */
    public boolean admitClient(ClientSideFSM.FetchRequest msg) {
		_LOG.debug(new Strings(new Object[] {"admitClient ", msg, " ?"}));
		return true;
    }

    /* 
     * Inform the corresponding Projector of the changed fields and
     * their values.
     * @param receptor the Receptor
     */
    public void receptorStateChangeIndication(Receptor r) {
		// DAVID: Replace this with something useful
		if (_LOG.isEnabledFor(Level.DEBUG)) r.print();
    }

    /**
	 * Add Selectables to the SelectableSet.
	 *
     * @param ss the SelectableSet
     */
	protected void buildSelectableSet(SelectableSet ss) {
	}

    /* 
     * Asynchronously inform the thread running through this
     * ControlLogic that it is time to stop
     */
    protected final void stopRunning() {
	_running = false;
    }

    /* 
     * Determine if the ControlLogic is active
     * @return true iff the thread is active.
     */
    protected final boolean isRunning() {
	return _running;
    }

	/**
	 * Add <CODE>r</CODE> to the LinkedList of Receptors for
	 * <CODE>p</CODE>.  If <CODE>r</CODE> is the first Receptor for
	 * <CODE>p</CODE> then tell <CODE>p</CODE> to take its query
	 * parameters from <CODE>r</CODE>.
	 */
	private final void addReceptor(Projector p, Receptor r) {
		LinkedList receptors = (LinkedList)_projector2Receptor.get(p);
		if (null == receptors) {
			receptors = new LinkedList();
			_projector2Receptor.put(p, receptors);
		}

		receptors.add(r);
		if (1 == receptors.size()) {
			// p has no Receptor to give it query parameters
			p.setReceptor(r);
			// p gets its query parameters from r.
		}
	}

	/**
	 * Remove <CODE>r</CODE> from the LinkedList of Receptors for
	 * <CODE>p</CODE>.  If <CODE>r</CODE> is the first Receptor in the
	 * LinkedList then tell <CODE>p</CODE> to take its query
	 * parameters from the next Receptor in the LinkedList.  If there
	 * isn't a next Receptor, then <CODE>p</CODE> gets no query
	 * parameters.
	 */
	private final void removeReceptor(Projector p, Receptor r) {
		LinkedList receptors = (LinkedList)_projector2Receptor.get(p);
		if (null != receptors) {
			// Special case: the first Receptor in the LinkedList
			// supplies p with query parameters.  If r is that
			// Receptor, then make the next Receptor supply the query
			// parameters.
			if (receptors.getFirst() == r) {
				Receptor r1 = null;

				try {
					r1 = (Receptor)receptors.get(1);
				} catch (IndexOutOfBoundsException ex) {
				}

				p.setReceptor(r1);
			}

			receptors.remove(r);
			if (0 == receptors.size()) {
				_projector2Receptor.remove(p);
			}
		}
	}
}

// ControlLogic.java

