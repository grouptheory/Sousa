// Clock.java

package mil.navy.nrl.cmf.sousa;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import mil.navy.nrl.cmf.sousa.util.MapUtil;

/**
 * A class which provides a mechanism to manage a bundle of Alarms and
 * their associated AlarmHandlers in a temporally accurate way.
 * 
 * @version 	$Id: Clock.java,v 1.4 2006/01/31 16:09:44 bilal Exp $
 * @author 	Bilal Khan
 * @author 	David Talmage
 */

public final class Clock {
    
    /**
     * A class which represents a temporal description
     * 
     * @version 	$Id: Clock.java,v 1.4 2006/01/31 16:09:44 bilal Exp $
     * @see     Clock
     * @author 	Bilal Khan
     * @author 	David Talmage
     */
    public final static class Alarm {
	final Clock _clock;
	final int _period;
	final Object _assoc; // User data
	final AlarmHandler _handler;
	final boolean _recurring;
	boolean _enabled;
	long _expiry;
	
	/** 
	 * Constructor
	 *
	 * @param clock  the clock to which the Alarm is registered
	 * @param period the time 
	 * @param recurring does the alarm repeat
	 * @param assoc	  an associated parameter
	 * @param handler the handler to call back when the alarm expires
	 */
	private Alarm(Clock clock, int period, boolean recurring, Object assoc,
		      AlarmHandler handler) {
	    _clock = clock;
	    _period = period;
	    _recurring = recurring;
	    _assoc = assoc;
	    _handler = handler;
	    _enabled = true;
	    _expiry = System.currentTimeMillis() + _period;
	}
	
	/** 
	 * Trigger the Alarm
	 */
	private void trigger() {
	    if (_enabled) {
		disable();
		
		// recurring: auto-enable unless disable() is called in
		// AlarmHandler (i.e. _handler.handle(this))
		//
		// non-recurring: auto-disable unless enable() is called
		// in AlarmHandler
		
		if (_recurring) _enabled = true;
		
		_handler.handle(this);
		
		if (_recurring && _enabled) enable();
	    }
	}
	
	/** 
	 * Disable the Alarm - If you disable() an Alarm before
	 * Clock.processAlarms() invokes its trigger(), the
	 * AlarmHandler won't be notified.
	 */
	public void disable() {
	    _enabled = false;
	    _clock.deregister(this);
	}
	
	/** 
	 * Enable the Alarm
	 */
	public void enable() {
	    _expiry = System.currentTimeMillis() + _period;		
	    _enabled = true;
	    _clock.register(this);
	}
	
	/** 
	 * Get the associated Object
	 */
	public Object getAssoc() {
	    return _assoc;
	}
	
	/** 
	 * Convert to String representation
	 */
	public String toString() {
	    return super.toString() + 
		"{ clock:" + _clock + " period:" + _period +
		" recurring:" + _recurring + " assoc:" + _assoc +
		" handler:" + _handler + " enabled:" + _enabled +
		" expiry:" + _expiry + "}";
	}
    }
    
    /**
     * A class which represents how to respond to an Alarm
     * 
     * @version 	$Id: Clock.java,v 1.4 2006/01/31 16:09:44 bilal Exp $
     * @see     Clock
     * @author 	Bilal Khan
     * @author 	David Talmage
     */
    public interface AlarmHandler {
	/** 
	 * To make the Alarm reoccur, call m.enable() in handle().
	 * @param m the Alarm that expired
	 */
	void handle(Alarm m);
    };
    
    // Clock internals follow

    private final long SELECT_TIME_NO_ALARMS = 10000;
    // Long --> List of Clock.Alarm
    private final TreeMap _alarms = new TreeMap();
    private final Entity _entity;
    
    /** 
     * Constructor
     * @param e the Entity with which the Alarm is associated
     */
    Clock(Entity e) {
	_entity = e;
    }
    
    /** 
     * Make a new Alarm and register it
     *
     * @param period the time 
     * @param recurring does the alarm repeat
     * @param assoc	  an associated parameter
     * @param handler the handler to call back when the alarm expires
     * @return     a new Alarm that is registered with this Clock
     */
    public Alarm setAlarm(int period, boolean recurring, Object assoc, 
			  AlarmHandler handler) {
	Alarm a = new Alarm(this, period, recurring, assoc, handler);
	register(a);
	return a;
    }
    
    /** 
     * Register an Alarm with this Clock
     * @param a the Alarm
     */
    private void register(Alarm a) {
	MapUtil.addToMapSet(_alarms, new Long(a._expiry), a);	
    }
    
    /** 
     * Deregister an Alarm from this Clock
     */
    private void deregister(Alarm a) {
	MapUtil.removeFromMapSet(_alarms, new Long(a._expiry), a);	
    }
    
    /** 
     * Return the time to the next Alarm to expire (or
     * SELECT_TIME_NO_ALARMS if no Alarm is registered).
     * @return time until the next Alarm 
     */
    // Entity calls timeToNextAlarm()
    long timeToNextAlarm() {
	long t = SELECT_TIME_NO_ALARMS;
	if (_alarms.size() > 0) {
	    long now = System.currentTimeMillis();
	    Long next = (Long)_alarms.firstKey();
	    t = (next.longValue() - now);
	    if (t < 1) t = 1;
	}
	return t;
    }
    
    /** 
     * Process all the alarms that have expired since the last time this method was called.
     */
    void processAlarms() {
	boolean done = false;
	long now = System.currentTimeMillis();
	
	// Always check _alarms.size() because TreeMap.firstKey() throws
	// NoSuchElementException when the Map is empty.
	while (!done && (_alarms.size() > 0)) {
	    Long firstKey = (Long)_alarms.firstKey();
	    if (firstKey.longValue() > now) done = true;
	    else {
		Set alarms = (Set)_alarms.remove(firstKey);
		for (Iterator i = alarms.iterator(); i.hasNext(); ) {
		    ((Alarm)i.next()).trigger();
		}
	    }
	}
    }
}

// Clock.java
