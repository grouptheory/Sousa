package mil.navy.nrl.cmf.sousa.spatiotemporal;

import mil.navy.nrl.cmf.sousa.Field;
import mil.navy.nrl.cmf.sousa.State;
import mil.navy.nrl.cmf.sousa.ViewInterpreter;
import java.io.Serializable;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.apache.log4j.Logger;

public class SpatiotemporalViewInterpreter 
	extends ViewInterpreter {
    protected static final Logger _LOG = 
		Logger.getLogger(SpatiotemporalViewInterpreter.class);

	// Set of String.  Names the Fields of the authoritative State
	// that SpatiotemporalViewInterpreter copies into the Projector
	// State.


    public SpatiotemporalViewInterpreter(State s) 
    {
		super(s);
		HashSet fields = new HashSet();

		// These are the Fields of s that interest us.
		fields.add(QueryClientFields.POSITION_FIELDNAME);
		fields.add(QueryClientFields.WIDTH_FIELDNAME);
		fields.add(QueryClientFields.TIMELOWERBOUND_FIELDNAME);
		fields.add(QueryClientFields.TIMEUPPERBOUND_FIELDNAME);
		fields.add(QueryClientFields.FIELDS_FIELDNAME);

		// Tell s to notify us whenver one of the Fields changes.
		s.attachFieldListener(fields, this);
    }

	public boolean isDirty(State parameters){
		return isDirty();
	}

	protected State.ChangeMessage getCurrentFieldValues(State parameters) {
		//
		// super.getCurrentFieldValues() invokes interpret().
		// this.getCurrentFieldValues() must Fill in the Fields that
		// interpret() doesn't provide.
		//
		State.ChangeMessage changes = super.getCurrentFieldValues(parameters);
		boolean needPosition = true;
		boolean needWidth = true;
		boolean needTimeLowerBound = true;
		boolean needTimeUpperBound = true;
		boolean needFields = true;
		List fieldChangeMessages = changes.getMessages();
		
		for (Iterator i = fieldChangeMessages.iterator(); i.hasNext(); ) {
			Field.ChangeMessage fieldChange = (Field.ChangeMessage)i.next();
			if (fieldChange._fname.equals(QueryClientFields.POSITION_FIELDNAME)) {
				needPosition = false;
			}
			
			if (fieldChange._fname.equals(QueryClientFields.WIDTH_FIELDNAME)) {
				needWidth = false;
			}

			if (fieldChange._fname.equals(QueryClientFields.TIMELOWERBOUND_FIELDNAME)) {
				needTimeLowerBound = false;
			}

			if (fieldChange._fname.equals(QueryClientFields.TIMEUPPERBOUND_FIELDNAME)) {
				needTimeUpperBound = false;
			}

			if (fieldChange._fname.equals(QueryClientFields.FIELDS_FIELDNAME)) {
				needFields = false;
			}
		}

		// Fill in the Field values that super.getCurrentFieldValues()
		// didn't return with values from the State.  It's a fatal
		// error for one of these Fields to (1) not be listened to; or
		// (2) not be part of State.
		try {
			if (needPosition) {
				Vector3d v = (Vector3d)getField(QueryClientFields.POSITION_FIELDNAME);
				Field.ChangeMessage m = 
					new Field.ChangeMessage(QueryClientFields.POSITION_FIELDNAME, v);

				changes.addFieldChangeMessage(m);
			}

			if (needWidth) {
				Vector3d v = (Vector3d)getField(QueryClientFields.WIDTH_FIELDNAME);
				Field.ChangeMessage m = 
					new Field.ChangeMessage(QueryClientFields.WIDTH_FIELDNAME, v);

				changes.addFieldChangeMessage(m);
			}

			if (needTimeLowerBound) {
				Calendar v = (Calendar)getField(QueryClientFields.TIMELOWERBOUND_FIELDNAME);
				Field.ChangeMessage m = 
					new Field.ChangeMessage(QueryClientFields.TIMELOWERBOUND_FIELDNAME, v);

				changes.addFieldChangeMessage(m);
			}

			if (needTimeUpperBound) {
				Calendar v = (Calendar)getField(QueryClientFields.TIMEUPPERBOUND_FIELDNAME);
				Field.ChangeMessage m = 
					new Field.ChangeMessage(QueryClientFields.TIMEUPPERBOUND_FIELDNAME, v);

				changes.addFieldChangeMessage(m);
			}

			if (needFields) {
				Set v = (Set)getField(QueryClientFields.FIELDS_FIELDNAME);
				Field.ChangeMessage m = new Field.ChangeMessage(QueryClientFields.FIELDS_FIELDNAME,
																(Serializable)v);
				changes.addFieldChangeMessage(m);

			}
		} catch (NoSuchFieldException ex) {
			_LOG.fatal(ex);
		}

		return changes;
	}

	/**
	   Copies all of the members of <CODE>fieldChanges</CODE> that are
	   also members of {@link #_fields} in to a new
	   <CODE>State.ChangeMessage</CODE>.  Ignores
	   <CODE>parameters</CODE>.
	 */
    protected State.ChangeMessage interpret(List fieldChanges,
											State parameters)
    {
		Set fields = getFields();
		State.ChangeMessage answer = new State.ChangeMessage();
		if (null != fieldChanges) {
			for (Iterator i = fieldChanges.iterator(); i.hasNext(); ) {
				Field.ChangeMessage fcm = (Field.ChangeMessage)i.next();

				if (fields.contains(fcm._fname)) {
					answer.addFieldChangeMessage(fcm);
				}
			}
		}

		return answer;
	}
}
