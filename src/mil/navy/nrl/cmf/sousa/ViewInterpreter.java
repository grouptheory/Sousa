package mil.navy.nrl.cmf.sousa;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


/**
 * A <CODE>ViewInterpreter</CODE> is a <CODE>Fieldlistener</CODE> that
 * transforms its <CODE>Fields</CODE> of interest into new values in
 * another set of output <CODE>Fields</CODE>.  It makes the
 * transformation in response to a query.  The transformation has two
 * inputs: the <CODE>Fields</CODE> of interest and the <EM>query
 * parameters</EM>, a State other than the one that contains the
 * <CODE>Fields</CODE> of interest.
 */
public abstract class ViewInterpreter extends FieldListener {
    
    /**
     * The set of names of query parameter <CODE>Fields</CODE> that
     * have changed.  {@link #setDirtyParameterFields(List)} adds
     * <CODE>Field</CODE> names to
     * <CODE>_dirtyParameterFields</CODE>. {@link
     * #clearDirtyParameterFields()} empties it.
     */
    private final Set _dirtyParameterFields = new HashSet();
    
    /**
     * Class constructor that uses a State.
     * <P>
     * 
     * Usage note: Classes that extend ViewInterpreter must call
     * {@link State#attachFieldListener(Set, FieldListener)} in their
     * constructors if they want to be notified about changes in
     * <CODE>s</CODE>.
     * 
     * @param s the state that contains the <CODE>Fields</CODE> of
     * interest
     */
    public ViewInterpreter(State s) {
	super(s);
    }
    
    /**
     * Transform the changes in the <CODE>Fields</CODE> of interest
     * and some query <CODE>parameters</CODE> into a subset of the output
     * <CODE>Fields</CODE>.
     * 
     * @param parameters the query parameters
     * @return a <CODE>State.ChangeMessage</CODE> that contains one
     * {@link Field.ChangeMessage} for each changed output <CODE>Field</CODE>
     */
    public final State.ChangeMessage makeMessage(State parameters) {
	List fieldChanges = null;
	State.ChangeMessage answer = null;
	State.ChangeMessage scm = super.makeMessage();
	
	if (null != scm)
	    fieldChanges = scm.getMessages();
	
	answer = interpret(fieldChanges, parameters);
	
	clearDirtyParameterFields();
	
	return answer;
    }
    
    /**
     * Records the names of the query parameter <CODE>Fields</CODE>
     * that have changed.
     * 
     * @param parameters one {@link Field.ChangeMessage} for each
     * query parameter <CODE>Field</CODE> that has changed
     */
    final void setDirtyParameterFields(List parameters) {
	if (null != parameters) {
	    for (Iterator i = parameters.iterator(); i.hasNext(); ) {
		Field.ChangeMessage m = (Field.ChangeMessage)i.next();
		_dirtyParameterFields.add(m._fname);
	    }
	}
    }
    
    /**
     * Returns the set of names of query parameter <CODE>Fields</CODE>
     * that have changed.
     * 
     * @return a set of <CODE>Field</CODE> names
     */
    protected final Set getDirtyParameterFields() {
	return Collections.unmodifiableSet(_dirtyParameterFields);
    }
    
    /**
     * Clears the set of changed query parameter <CODE>Field</CODE> names.
     */
    final void clearDirtyParameterFields() {
	_dirtyParameterFields.clear();
    }
    
    /**
     * Builds a State.ChangeMessage that contains the current values of
     * all output <CODE>Fields</CODE>.  Invokes {@link #interpret(List, State)}
     * to learn some of the output <CODE>Field</CODE> values.
     * 
     * <P>
     * 
     * Implementation note: <EM>Ensure that the {@link
     * #interpret(List, State)} includes each output
     * <CODE>Field</CODE> in its output.</EM>
     * 
     * @param parameters the query parameters
     * @return a <CODE>State.ChangeMessage</CODE> that contains one
     * {@link Field.ChangeMessage} for each output <CODE>Field<CODE>
     * */
    protected State.ChangeMessage getCurrentFieldValues(State parameters) {
	State.ChangeMessage scm = getCurrentFieldValues();
	if (scm != null) {
	    List initialValues = scm.getMessages();
	
	    // initialValues contains all of the values of the Fields of
	    // the authoritative State that this ViewInterpreter listens to.
	    //
	    // parameterValues contains all of the values of the Fields in
	    // a Receptor State.
	    return interpret(initialValues, parameters);
	}
	else {
	    return null;
	}
    }
    
    /**
     * Transforms the changes in <CODE>Fields</CODE> of interest along
     * with the query parameters into a
     * <CODE>State.ChangeMessage</CODE> that contains one {@link
     * Field.ChangeMessage} for each changed output
     * <CODE>Field</CODE>. <CODE>interpret</CODE> may return null.
     * <CODE>interpret</CODE> may return a State.ChaneMessage with no
     * Field.ChangeMessages inside.
     * 
     * @param fieldChanges a <CODE>List</CODE> of
     * <CODE>Field.ChangeMessages</CODE> from the <CODE>Fields</CODE>
     * of interest.  It may be <CODE>null</CODE>.
     * @param parameters the query parameters.  It may be <CODE>null</CODE>.
     * @return a <CODE>State.ChangeMessage</CODE> containing the
     * changes to the output <CODE>Fields</CODE>.  It may be
     * <CODE>null</CODE> or empty.
     */
    protected abstract State.ChangeMessage interpret(List fieldChanges, State parameters);
    
    
    /**
     * This ViewInterpreter decides if it is interested in writing
     * given the changes in its query parameters.  It may consult
     * {@link #getDirtyParameterFields}.
     * 
     * @param parameters the query parameters. It may be <CODE>null</CODE>.
     * 
     * @return <CODE>true</CODE> if the <CODE>ViewInterpreter</CODE>
     * is interested in writing; <CODE>false</CODE> otherwise
     */
    public abstract boolean isDirty(State parameters);
    
    /**
     * Transforms the unqualified name of a field into a qualified
     * one.  A qualified field name uniquely identifies the kind of
     * <CODE>ViewInterpreter</CODE> that provided it.  It follows the
     * pattern <B>fully qualified class name</B>_<B>unqualified field
     * name</B>.
     * 
     * @param classOfAViewInterpreter the <CODE>Class</CODE> of the
     * <CODE>ViewInterpreter</CODE> that provides the field.
     * @param unqualifiedName the unqualified name of the field.
     * @return the qualified field name
     * @throws java.lang.IllegalArgumentException if
     * <CODE>classOfAViewInterpreter</CODE> isn't assignable to
     * ViewInterpreter.class.
     */
    public static String getQualifiedFieldName(Class classOfAViewInterpreter,
					       String unqualifiedName)
    {
	if (ViewInterpreter.class.isAssignableFrom(classOfAViewInterpreter)) {
	    return classOfAViewInterpreter.getName() + "_" + unqualifiedName;
	} else {
	    throw new IllegalArgumentException(classOfAViewInterpreter.getName() + 
					       " is not assignable to ViewInterpreter.class.");
	}
    }
    
    
}
