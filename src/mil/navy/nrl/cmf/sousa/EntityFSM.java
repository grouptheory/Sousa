// File: EntityFSM.java

package mil.navy.nrl.cmf.sousa;

import java.io.Serializable;
import java.util.LinkedList;

import mil.navy.nrl.cmf.sousa.util.Strings;

import org.apache.log4j.Logger;

/**
 * EntityFSM is an abstractv class that contains all common
 * information about a single instance of the Entity's as a client or
 * server.
 *
 * An EntityFSM object includes the following state information:
 * <ul>
 * <li> P2PChannel control channel
 * <li> EntityFSM.State representing the current state of the FSM
 * <li> LinkedList serving as the outbound queue
 * <li> boolean deathWarrant indicating the FSM is about to be freed
 * </ul>
 * <p>
 *
 * @version 	$Id: EntityFSM.java,v 1.7 2007/05/01 20:37:07 talmage Exp $
 * @author 	Bilal Khan
 * @author 	David Talmage
 */
 
public abstract class EntityFSM implements Selectable.Handler, 
										   Clock.AlarmHandler {

	/**
	 * The log4j log for the EntityFSM.
	 */
    private static final Logger _LOG = 
		Logger.getLogger(EntityFSM.class);

		/**
		 * A Request is an abstract base class representing request
		 * messages.
		 */
    static class Request implements Serializable {
		private static final long serialVersionUID = 1L;}
    static final Request BOOT_REQUEST = new Request();

		/**
		 * A Response is an abstract base class representing response
		 * messages.
		 */
    static class Response implements Serializable {
		private static final long serialVersionUID = 1L;
		}

	
		/**
		 * A State is an interface for control aspects of an
		 * EntityFSM.  A concrete State must contain no data members
		 * and must be a singleton instance.  All instance-specific
		 * data members must be held in the EntityFSM context classes,
		 * which are passed in to the FSM.State whenever a method is
		 * called.
		 */
    public interface State {
		/** 
		 * Handle a signal from a Selectable object.
		 * @param sel         the Selectable
		 * @param st          the Signal
		 * @param context     the FSMContext
		 * @return the new EntityFSM.State
		 */
		public EntityFSM.State handle(Selectable sel, SignalType st, 
									  EntityFSM context);
		
		/** 
		 * Handle a timer expiry
		 * @param m           the Alarm that expired
		 * @param context     the FSMContext
		 * @return the new EntityFSM.State
		 */
		public EntityFSM.State handle(Clock.Alarm m, EntityFSM context);
		
		/** 
		 * Handle the arrival of an Indication from the lower layer
		 * @param x           the request message
		 * @param context     the FSMContext
		 * @return the new EntityFSM.State
		 */
		public EntityFSM.State handleIndication(Serializable x, EntityFSM context);
		
		/** 
		 * Handle the arrival of an Request from the higher layer
		 * @param req         the request 
		 * @param context     the FSMContext
		 * @return the new EntityFSM.State
		 */
		public EntityFSM.State handleRequest(EntityFSM.Request req, 
											 EntityFSM context);
		
		/** 
		 * Return the name of this FSM
		 * @return the String representation of the name
		 */
		public String name();
    };

    
	/**
	 * The basic No Operation State for an EntityFSM, in which all
	 * transitions are no operation self-loops.
	 */
    static final EntityFSM.State NULL_STATE = new EntityFSM.State() {
		
			/** 
			 * @see EntityFSM.State#handle(Selectable, ...)
			 */
			public EntityFSM.State handle(Selectable sel, SignalType st, 
										  EntityFSM context) {
				return context.dispatch(sel, st, this);
			}

			/** 
			 * @see EntityFSM.State#handle(Clock.Alarm, ...)
			 */
			public EntityFSM.State handle(Clock.Alarm m, EntityFSM context) {
				return this;
			}
			
			/** 
			 * @see EntityFSM.State#handleIndication(...)
			 */
			public EntityFSM.State handleIndication(Serializable x, 
													EntityFSM context) {
				return this;
			}

			/** 
			 * @see EntityFSM.State#handleRequest(...)
			 */
			public EntityFSM.State handleRequest(EntityFSM.Request req, 
												 EntityFSM context) {
				return this;
			}

			/** 
			 * @see EntityFSM.State#name()
			 */
			public String name() { return "EntityFSM.NULL_STATE"; }
		};
	
	/** 
	 * Handle a signal from a Selectable object by asking the
	 * State to do something within this context.  Base writing
	 * functionality: takes from the outQ and writes.  Base
	 * reading functionality: messages read are dispatched to the
	 * state for handling.
	 *
	 * @param sel         the Selectable
	 * @param st          the Signal
	 * @param state       the State
	 * @return the new EntityFSM.State
	 */
    final EntityFSM.State dispatch(Selectable sel, SignalType st, 
								   EntityFSM.State state) {
		EntityFSM.State next = null;
		if (sel == _cc) {
			//TODO - When we implement full crypto and change keys
			//when a leaf disconnects, it might be necessary for _cc
			//to throw a ProtocolException to signal that the crypto
			//parameters have changed.  dispatch() must catch
			//ProtocolException and transition to the current state.
			if (st == SignalType.READ) {
				Serializable x = (Serializable)_cc.read();
				_LOG.debug(new Strings(new Object[] {"EntityFSM read: ", x}));
				next = state.handleIndication(x, this);
			}
			else if (st == SignalType.WRITE) {
				Serializable x = (Serializable)_outQ.removeFirst();
				_LOG.debug(new Strings(new Object[] {"EntityFSM wrote: ", x}));
				_cc.write(x);
				next = state;

				if (_deathWarrant) {
				    attemptKill();
				}
			}
			else { // Error
				_LOG.error("EntityFSM error: " + sel + ": " + sel.getError());
				deregister();
				next = NULL_STATE;
			}
		}
		return next;
    }

	/** 
	 * The point-to-point control channel connection to the remote
	 * entity.
	 */
    final P2PChannel _cc;
	/** 
	 * The current State of this FSM.
	 */
    protected EntityFSM.State _currentState = NULL_STATE;
	/** 
	 * Outbound messages queue, which is a list of Serializables.
	 */
    protected final LinkedList _outQ = new LinkedList();
	/** 
	 * Boolean indicating that this FSM is ready to die once all
	 * outbound messages have been written into the control channel.
	 */
    private boolean _deathWarrant = false;

	/** 
	 * Construct a new EntityFSM for managing the state of the FSM
	 * over a given control channel.
	 *
	 * @param cc          the point-to-point control channel.
	 */
	EntityFSM(P2PChannel cc) {
		_cc = cc;
	}

	//// schedule actions
	
	/** 
	 * Schedule the writing of a serializable on the control channel.
	 * How is this implemented?  We put the message in the outbound
	 * queue, break select, causing it to rebuild the selectableset so
	 * that the control channel of this FSM is included in the write
	 * set.
	 *
	 * @param ser          the serializable object to write.
	 */
	final void scheduleWrite(Serializable ser) {
		synchronized (_outQ) {
			_outQ.addLast(ser);
			wakeup();
		}
	}

	/** 
	 * Wait until the outQ is empty, then kill this FSM.  This method
	 * must be called repeatedly--once in each call to handle(), until
	 * the FSM is killed (deregistered from the Focus/Nimbus) at which
	 * point it will cease to get time.
	 */
        final void flushThenKill() {
	    _deathWarrant = true;
	    if (_deathWarrant) attemptKill();
	}
	
	/** 
	 * Attempt to kill this FSM.
	 */
	abstract void attemptKill();

	/** 
	 * Interrupt the Entity thread, thereby breaking it's select()
	 */
    abstract void wakeup();

	/** 
	 * Disconnect this FSM because the underlying p2pchannel is dead.
	 */
    abstract void deregister();

	/** 
	 * Handle IO by delegating to the current State
	 * @param sel         the Selectable
	 * @param st          the Signal
	 */
    public final void handle(Selectable sel, SignalType st) {
		EntityFSM.State next = _currentState.handle(sel,st,this);
		_LOG.debug(new Strings(new Object[] 
			{"handleIO(", sel.getClass().getName(), "@", 
			 new Integer(sel.hashCode()), ",", st, ") ", this, ": ", 
			 _currentState.name(), "-->", next.name()}));
		_currentState = next;
    }

	/** 
	 * Handle timers by delegating to the current State
	 * @param m         the Alarm
	 */
    public final void handle(Clock.Alarm m) {
		EntityFSM.State next = _currentState.handle(m, this);
		_LOG.debug(new Strings(new Object[] 
			{"handleTimers(", m, ") ", this, ": ", 
			 _currentState.name(), "-->", next.name()}));
		_currentState = next;
    }

	/** 
	 * Handle requests by delegating to the current State
	 * @param req         the Request
	 */
    public final void handleRequest(EntityFSM.Request req) {
		EntityFSM.State next = _currentState.handleRequest(req, this);
		_LOG.debug(new Strings(new Object[] 
			{"handleRequest(", req, ") ", this, ": ", 
			 _currentState.name(), "-->", next.name()}));
		_currentState = next;
	
    }
}
