package mil.navy.nrl.cmf.sousa;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;

/**
 * A single key-value pair within a State
 */
public final class Field implements Serializable {

    //// nested classes
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Message which encapsulates an atomic change to the Field
     */
    public final static class ChangeMessage implements Serializable {
	private static final long serialVersionUID = 1L;
	public final String _fname;
	public final Serializable _value;
	
	/**
	 * The field-name and its new value, in a message format
	 */
	public ChangeMessage(String fname, Serializable value) {
	    _fname = fname;
	    _value = value;
	}
    }
    
    //// data members
    
    // The ambient State in which this Field resides
    private final State _state;
    // The name of this Field
    private final String  _name;
    // the value of this Field
    private Serializable _value;
    // the FieldListener objects attached to this Field
    private final HashSet _listeners = new HashSet();
    
    //// package methods
    
    /*
     * Construct a new Field with a given name and Value, associated with State s
     * @param s the field name
     * @param value the current value
     * @param state the owning State
     */
    /*

     */
    Field(String s, Serializable value, State state) {
	_name = s;
	_value = value;
	_state = state;
    }
    
    /*
     * Attach a FieldListener to this Field
     * @param fl the field listener which receives synchronous callback on changes to value
     */
    final void attachFieldListener(FieldListener fl) {
	_listeners.add(fl);
	fl.attachFieldNotification(_name);
    }
    
    /*
     * Detach a FieldListener to this Field
     * @param fl the field listener which receives synchronous callback on changes to value
     */
    final void detachFieldListener(FieldListener fl) {
	_listeners.remove(fl);
	fl.detachFieldNotification(_name);
    }
    
    /*
     * Set the value of this Field
     * @param newValue the new value of this field
     */
    final void setValue(Serializable newValue) {
	_value = newValue;
	fireNotifications();
    }
    
    /*
     * Get the value of this Field as an immutable Serializable
     * @return the current value of this field
     */
    final Serializable getValue() {
	return _value;
    }
    
    /*
     * Inform all listeners of this Field that it has changed.
     */
    final void fireNotifications() {
	Field.ChangeMessage fcm = new Field.ChangeMessage(_name, _value);
	for (Iterator it = _listeners.iterator(); it.hasNext();) {
	    FieldListener fl = (FieldListener)it.next();
	    fl.recvNotification( _name );
	}
    }
    
    /*
     * build a Field.ChangeMessage for this Field
     * @return a Field.ChangeMessage with the field's present value
     */
    final ChangeMessage makeMessage() {
	return new Field.ChangeMessage(_name, _value);
    }
    
    /*
     * apply a Field.ChangeMessage to this Field
     * @param fcm the Field.ChangeMessage to be applied
     */
    final void applyMessage(Field.ChangeMessage fcm) {
	this.setValue(fcm._value);
    }
    
}
