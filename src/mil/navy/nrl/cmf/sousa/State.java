package mil.navy.nrl.cmf.sousa;

import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.apache.log4j.Logger;

/**
 * A collection of Fields in an Entity, and its cached copies at clients.
 */
public final class State implements Serializable {
    private static final Logger _LOG = 
	Logger.getLogger(State.class);
    
    //// nested classes
    
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    /**
     * A Message which encapsulates an atomic change to the
     * State.
     */
    // DAVID: ChangeMessage must become public in order to make
    // concrete classes derived from ViewInterpreter.
    // See ViewInterpreter.interpret().
    public final static class ChangeMessage implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	final LinkedList _fcms = new LinkedList(); // FieldChangeMessages
	
	public ChangeMessage() {}
	
	public void addFieldChangeMessage(Field.ChangeMessage fcm) {
	    _fcms.addLast(fcm);
	}
	
	// PURPOSE: Return an immutable copy of the list of
	// Field.ChangeMessages.
	// PRECONDITION: _fcms is not empty
	// POSTCONDITION: none
	public List getMessages() {
	    return Collections.unmodifiableList(_fcms);
	}
	
	int size() {
	    return _fcms.size();
	}
    }
    
    //// data members
    
    // A Map from String to Field.
    private final TreeMap _fields = new TreeMap();
    
    //// public methods
    
    /**
     * add a Field to this Entity's authoritative state
     *
     * @param name the name of the field
     * @param value its initial value
    */
    public final void addField(String name, Serializable value) {
	_fields.put(name, new Field(name, value, this));
    }
    
    /**
     * mutate a field in the authoritative state of an Entity
     *
     * @param name the name of the field
     * @param value its initial value
    */
    public final void setField(String name, Serializable value)
	throws NoSuchFieldException 
    {
	Field f = (Field)_fields.get(name);
	
	if (null == f) throw new NoSuchFieldException(name);
	
	f.setValue(value);
    }

    /**
     * Return the value of a Field in this State.  The Field must
     * exist.  It is an error to ask for a Field that is not in this
     * State.
     *
     * @param name the field name 
     * @return the value of the field <CODE>name</CODE>
    */
    public final Serializable getField(String name) 
	throws NoSuchFieldException 
    {
	Field f = (Field)_fields.get(name);
	
	if (null == f) throw new NoSuchFieldException(name);
	
	return f.getValue();
    }
    
    
    /**
     * Constructor to make a new authoritative State object which
     * resides in the Entity.
    */
    public State() {
    }
        
    /**
     * Attach a listener to a set of Fields.  Ignores members of
     * fnames that are not names of Fields in this State.
     *
     * @param fnames the names of fields
     * @param fl the listener
    */
    public final void attachFieldListener(Set fnames, FieldListener fl)
    {
	StringBuffer errorBuf = new StringBuffer();
	for (Iterator it = fnames.iterator(); it.hasNext();) {
	    String s = (String)it.next();
	    Field f = (Field)_fields.get(s);
	    if (null != f)
		f.attachFieldListener(fl);
	    else {
		// DAVID: This is a reasonable use of String
		// concatenation in Logger output.  Level.ERROR is
		// severe and will probably be enabled by all users at
		// run-time.  In addition, this kind of error should
		// be rare.
		//
		_LOG.error(this + ".attachFieldListener(): No such field: " + s);
	    }
	}
    }
    
    /**
     * Detach a listener from a set of Fields.  Ignores members of
     * fnames that are not names of Fields in this State.
     *
     * @param fnames the names of fields
     * @param fl the listener
    */
    public final void detachFieldListener(Set fnames, FieldListener fl) {
	// Doing it this way instead of the idiomatic
	//   for (Iterator it = fnames.iterator(); it.hasNext(); ) {...}
	// avoids a ConcurrentModificationException from 
	// FieldListener.detachFieldNotification()
	Object[] fieldNames = fnames.toArray();
	for (int it = 0; it < fieldNames.length; it++) {
	    Field f = (Field)_fields.get((String)fieldNames[it]);
	    if (null != f)
		f.detachFieldListener(fl);
	    else {
		// DAVID: This is a reasonable use of String
		// concatenation in Logger output.  Level.ERROR is
		// severe and will probably be enabled by all users at
		// run-time.  In addition, this kind of error should
		// be rare.
		//
		_LOG.error(this + ".detachFieldListener(): No such field: " +
			   (String)fieldNames[it]);
	    }
	    
	}
    }
    
    //// package methods
    
    /**
     * Returns a Set of the names of the Fields in the State.
     *
     * @return a Set of Strings
    */
    public final Set getFieldNames() {
	return _fields.keySet();
    }
    
    /**
     * Build a State.ChangeMessage for changed fields
     *
     * @param fnames a set of Field names
     * @return a message encoding the changes to fields named within fnames.
    */
    final State.ChangeMessage makeMessage(Set fnames) {
	State.ChangeMessage scm = null;
	for (Iterator it = fnames.iterator(); it.hasNext();) {
	    String s = (String)it.next();
	    Field f = (Field)_fields.get(s);	    
	    if (null != f) {
		Field.ChangeMessage fcm = f.makeMessage();
		if (scm == null) {
		    scm = new State.ChangeMessage();
		}
		scm.addFieldChangeMessage(fcm);
	    } else {
		// DAVID: This is a reasonable use of String
		// concatenation in Logger output.  Level.ERROR is
		// severe and will probably be enabled by all users at
		// run-time.  In addition, this kind of error should
		// be rare.
		//
		_LOG.error(this + ".makeMessage(): No such field: " + s);
	    }
	}
	return scm;
    }
    
    /**
     * Apply a State.ChangeMessage to this State
     *
     * @return scm a State.ChangeMessage which provides information about new values of fields
    */
    final void applyMessage(State.ChangeMessage scm) {
	for (Iterator it = scm._fcms.iterator(); it.hasNext();) {
	    Field.ChangeMessage fcm = (Field.ChangeMessage)it.next();
	    Field f = (Field)_fields.get(fcm._fname);
	    if (null != f) 
		f.applyMessage(fcm);
	    else {
		// DAVID: This is a reasonable use of String
		// concatenation in Logger output.  Level.ERROR is
		// severe and will probably be enabled by all users at
		// run-time.  In addition, this kind of error should
		// be rare.
		//
		_LOG.error(this + ".applyMessage(): No such field: " + 
			   fcm._fname);
	    }
	}
    }
    
    /**
     * Print the contents of this State
    */
    public void print() {
	for (Iterator i = _fields.entrySet().iterator(); i.hasNext(); ) {
	    Map.Entry entry = (Map.Entry)i.next();
	    System.out.println("["+entry.getKey() + "]=" + 
			       ((Field)entry.getValue()).getValue());
	}
    }
}
