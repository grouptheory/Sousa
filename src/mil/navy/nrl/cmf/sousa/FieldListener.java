package mil.navy.nrl.cmf.sousa;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * An object which listens to a set of Fields of a State.
 */
public class FieldListener {
    
    //// data members
    
    // the State which contains the Fields to which this FieldListener
    // can be attached.
    private final State _state;
    // the (String) names of the Fields to which this FieldListener is
    // listening, all residing in the State _state.
    private final HashSet _fields = new HashSet();
    // the subset of Fields named in _fields which are dirty.
    private final HashSet _dirty = new HashSet();
    
    
    //// public methods
    
    /*
     * Construct a FieldListener, associated with a given State
     * @param s the State to which this FieldListener will be associated
     */
    public FieldListener(State s) {
	_state = s;
    }
    
    /*
     * Build a State.ChangeMessage for the changed fields. Returns
     * null if there are no Fields.
     * @return a State.ChangeMessage which bundles Field.ChangeMessages
     */
    public State.ChangeMessage makeMessage() {
	State.ChangeMessage scm = _makeMessage(_dirty);
	_dirty.clear();
	
	return scm;
    }
    
    //// package methods
    
    
    /*
     * Confirm that this FieldListener has been attached to a Field.
     * @param fname the name of the Field
     */
    final void attachFieldNotification(String fname) {
	_fields.add(fname);
	
	// Record as dirty to ensure that there is a value for the
	// fname in the output of makeMessage().
	_dirty.add(fname);
    }
    
    /*
     * Confirm that this FieldListener is no longer attached to a Field.
     *
     * If you intend to detach all fields by iterating over
     * getFields() and calling detachFieldNotification() once per
     * field, you'll get a ConcurrentModificationException.  Try
     * this instead:
     *
     * Object[] fields = someFieldListener.getFields().toArray();
     * for (int i=0; i < fields.length; i++) {
     *    detachFieldNotification(fields[i]);
     * }
     *
     * @param fcm the Field.ChangeMessage to be applied
     * @return a Field.ChangeMessage with the field's present value
     */
    final void detachFieldNotification(String fname) {
	_fields.remove(fname);
	_dirty.remove(fname);
    }
    
    /*
     * Notify this FieldListener that a Field has changed.
     *
     * @param fname the name of the field
     */
    final void recvNotification(String fname) {
	_dirty.add(fname);
    }
    
    /*
     * Are any of the Fields to which this FieldListener listens
     * dirty?  If so, return true.  Return false otherwise.
     * @return true if there are any dirty Fields; false otherwise.
     */
    protected final boolean isDirty() {
	return (_dirty.size() > 0);
    }
    
    //// protected methods
    
    /*
     * Return the immutable set of Fields that the FieldListener listens to.
     * @return a Set of Fields
     */
    protected final Set getFields() {
	return Collections.unmodifiableSet(_fields);
    }
    
    /**
     * Delegates to State.getField() to return the value of a Field if
     * this FieldListener is listening to that Field and if it is a
     * Field of _state.  Throws NoSuchFieldException if this
     * FieldListener isn't listening to the Field.
     *
     * @param name the name of a Field
     * @return the value of the Field
     * @throws NoSuchFieldException if not listening to
     * <code>name</code> or if State.getField() throws it.
     * @see State#getField(String)
    */
    protected final Serializable getField(String name) 
	throws NoSuchFieldException 
    {
	if (! _fields.contains(name))
	    throw new NoSuchFieldException(name);
	
	return _state.getField(name);
    }
    
    /*
     * Build a State.ChangeMessage for the current values of all fields. Returns null if there are no Fields.
     * @return a State.ChangeMessage containing Field.ChangeMessages for all changed fields.
     */
    protected State.ChangeMessage getCurrentFieldValues() {
	State.ChangeMessage scm = _makeMessage(_fields);
	_dirty.clear();
	
	return scm;
    }
    
    //// private methods
    
    /*
     * Build a State.ChangeMessage for a Set of Fields.  Returns null if there are no Fields.
     * @param fields the fields of interest
     * @return a Field.ChangeMessage with the field's present value
     */
    private final State.ChangeMessage _makeMessage(Set fields) {
	State.ChangeMessage scm = null;
	
	if (fields.size() > 0) {
	    scm = _state.makeMessage(fields);
	}
	
	return scm;
    }
}
