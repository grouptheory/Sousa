// File: ConsumerViewInterpreter.java

package mil.navy.nrl.cmf.sousa.directory;


import java.io.Serializable;
import java.util.Map.Entry;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import mil.navy.nrl.cmf.sousa.Field;
import mil.navy.nrl.cmf.sousa.State;
import mil.navy.nrl.cmf.sousa.ViewInterpreter;
import mil.navy.nrl.cmf.sousa.spatiotemporal.QueryFields;
import mil.navy.nrl.cmf.sousa.util.Strings;

import org.apache.log4j.Logger;

/**
   This view interpreter listens to the DIRECTORY field of the
   AuthoritativeState.  It projects the ADDED and REMOVED fields.

   <P>

   Inputs from State: 
   <UL>
   <LI>{@link DirectoryFields#DIRECTORY_FIELDNAME} is a Map with
   {@link mil.navy.nrl.cmf.sousa.ServerContact} as keys and String as
   values. The String is a human-readable description of the
   information offered by advertiser.
   </UL>

   <P>

   Outputs to clients:
   <UL>
   <LI>{@link DirectoryFields#ADDED} is a
   Map:ServerContact-->String as described above.
   <LI>{@link DirectoryFields#REMOVED} is a
   Map:ServerContact-->String as described above.
   </UL>
 */
public class ConsumerViewInterpreter
	extends ViewInterpreter 
{
	protected static final Logger _LOG = Logger.getLogger(ConsumerViewInterpreter.class);

	/**
	   Cached copy of the authoritative State's {@link
	   DirectoryFields#DIRECTORY_FIELDNAME}.
	 */
	private final HashMap _directory = new HashMap();

	/**
	   Class constructor that uses a State.  This
	   ConsumerViewInterpreter watches the <CODE>Field</CODE> {@link
	   DirectoryFields#DIRECTORY_FIELDNAME} in <CODE>s</CODE>.  It
	   outputs two fields: {@link DirectoryFields#ADDED} and
	   {@link DirectoryFields#REMOVED}.

	   <P>
	   It complains if DIRECTORY_FIELDNAME is not a member of <CODE>s</CODE>.

	   <P>

	   <EM>BUG: Does not throw an exception (such as
	   NoSuchFieldException) if <CODE>s</CODE> has no
	   <CODE>Field</CODE> named {@link
	   DirectoryFields#DIRECTORY_FIELDNAME}.  Throwing an Exception
	   has consequences for the {@link
	   mil.navy.nrl.cmf.sousa.Projector} and for the other {@link
	   mil.navy.nrl.cmf.sousa.ViewInterpreter} classes.</EM>
	*/
	public ConsumerViewInterpreter(State s) {
		super(s);

		HashSet fields = new HashSet();

		// Only these Fields of s are of interest. 
		fields.add(DirectoryFields.DIRECTORY_FIELDNAME);

		// Tell s the names of its Fields that are of interest to us.
		// s notifies us whenever one of them changes.
		s.attachFieldListener(fields, this);

		try {
			_directory.putAll((Map)s.getField(DirectoryFields.DIRECTORY_FIELDNAME));
		} catch (NoSuchFieldException ex) {
			_LOG.error(new Strings(new Object[] {
				"ConsumerViewInterpreter(", s, "): ", ex}));
		}
	}

	// ViewInterpreter

	/**
	   A <CODE>ConsumerViewInterpreter</CODE> is dirty if and only if
	   one of the <CODE>Fields</CODE> it watches is dirty.
	 */
	public boolean isDirty(State parameters){
		return isDirty();
	}

	/**
	   Ensures that there is a {@link
	   mil.navy.nrl.cmf.sousa.Field.ChangeMessage} for {@link
	   DirectoryFields#ADDED} and for {@link
	   DirectoryFields#REMOVED} in the {@link
	   mil.navy.nrl.cmf.sousa.State.ChangeMessage}.

	   @param parameters the <CODE>Receptor</CODE> state that
	   parameterizes the query

	   @return a <CODE>State.ChangeMessage</CODE> that contains
	   <CODE>Field.ChangeMessages</CODE> for
	   <CODE>ADDED</CODE> and
	   <CODE>REMOVED</CODE>.
	 */
	protected State.ChangeMessage getCurrentFieldValues(State parameters) {
		//
		// super.getCurrentFieldValues() invokes interpret().
		// this.getCurrentFieldValues() must Fill in the Fields that
		// interpret() doesn't provide.
		//
		// If changes is missing a field (i.e ADDED or REMOVED), we
		// will add that field to it.  In addition, we will copy the
		// value of the authoritative State's DIRECTORY field into the
		// Field.ChangeMessage for ADDED.
		//
		State.ChangeMessage changes = super.getCurrentFieldValues(parameters);
		Field.ChangeMessage added = null;
		Field.ChangeMessage removed = null;
		List fieldChangeMessages = changes.getMessages();

		for (Iterator i = fieldChangeMessages.iterator(); i.hasNext(); ) {
			Field.ChangeMessage fieldChange = (Field.ChangeMessage)i.next();
			if (fieldChange._fname.equals(DirectoryFields.ADDED)) {
				added = fieldChange;
			}
			
			if (fieldChange._fname.equals(DirectoryFields.REMOVED)) {
				removed = fieldChange;
			}
		}

		if (null == added) {
			added = new Field.ChangeMessage(DirectoryFields.ADDED, 
											(Serializable)new HashMap());
			changes.addFieldChangeMessage(added);
		}

		try {
			Map directory = (Map)getField(DirectoryFields.DIRECTORY_FIELDNAME);
			Map add = (Map)added._value;
			add.putAll(directory);

		} catch (NoSuchFieldException ex) {
			// Can't happen?  Shouldn't happen!
			_LOG.error(new Strings(new Object[] {
				"getCurrentFieldValues(", parameters, "): ", ex}));
		}

		if (null == removed) {
			removed = new Field.ChangeMessage(DirectoryFields.REMOVED, 
											  (Serializable)new HashMap());
					changes.addFieldChangeMessage(removed);
		}

		return changes;
	}


	/**
	   Places new directory entries into {@link
	   DirectoryFields#ADDED} and ADDED.  Place the expired
	   directory entries into {@link
	   DirectoryFields#REMOVED}.

	   <P>
	   This implementation ignores parameters.
	*/
	protected State.ChangeMessage interpret(List fieldChanges,
											State parameters)
	{
		State.ChangeMessage answer = new State.ChangeMessage();

		// Watch for and project changes in the DIRECTORY field
		//
		// There ought to be just one entry in fieldChanges.  It must
		// be DirectoryFields.DIRECTORY_FIELDNAME.
		//
		for (Iterator i= fieldChanges.iterator(); i.hasNext(); ) {
			Field.ChangeMessage entityFCM = (Field.ChangeMessage)i.next();

			_LOG.debug(new Strings(new Object[] {
				this, " interpret() ", entityFCM._fname, "=", entityFCM._value
			}));

			if (entityFCM._fname.equals(DirectoryFields.DIRECTORY_FIELDNAME)) {
				HashMap removed = new HashMap();
				HashMap added = new HashMap();

				calculateDifferences((HashMap)entityFCM._value, added, removed);

				_directory.putAll(added);

				for (Iterator j = removed.keySet().iterator(); j.hasNext(); ) {
					_directory.remove(j.next());
				}

				if (added.size() > 0)
					answer.addFieldChangeMessage(new Field.ChangeMessage(DirectoryFields.ADDED, added));

				if (removed.size() > 0)
					answer.addFieldChangeMessage(new Field.ChangeMessage(DirectoryFields.REMOVED, removed));
			}
		}

		return answer;
	}


	/**
	   Calculates the set difference between {@link #_directory} and
	   <CODE>newValue</CODE>, copying the additions to
	   <CODE>added</CODE> and the deletions to <CODE>removed</CODE>.

	   @param newValue the current contents of the directory
	   @param added the new directory entries
	   @param removed the expired directory entries
	*/
	private void calculateDifferences(HashMap newValue, HashMap added, HashMap removed) {

		// Additions to _directory
		for (Iterator i = newValue.entrySet().iterator(); 
			 i.hasNext();) {
			Map.Entry e = (Map.Entry)i.next();
			Object key = e.getKey();
			if (! _directory.containsKey(key)) {
				Serializable value = (Serializable)e.getValue();
				added.put(key, value);
			}
		}

		for (Iterator i = _directory.entrySet().iterator();
			 i.hasNext();) {
			Map.Entry e = (Map.Entry)i.next();
			Object key = e.getKey();
			if (! newValue.containsKey(key)) {
				Serializable value = (Serializable)e.getValue();
				removed.put(key, value);
			}
		}
	}
};
