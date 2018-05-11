package mil.navy.nrl.cmf.sousa.idol.user;

import mil.navy.nrl.cmf.sousa.*;

import java.io.IOException;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import mil.navy.nrl.cmf.annotation.ServerVI;
import mil.navy.nrl.cmf.sousa.Field;
import mil.navy.nrl.cmf.sousa.Renderer;
import mil.navy.nrl.cmf.sousa.Renderable;
import mil.navy.nrl.cmf.sousa.State;
import mil.navy.nrl.cmf.sousa.spatiotemporal.*;
import mil.navy.nrl.cmf.sousa.util.Strings;
import mil.navy.nrl.cmf.stk.*;
import org.apache.log4j.Logger;


public final class AnnotationRenderer extends AbstractRenderer {
	protected static final Logger _LOG = Logger.getLogger(AnnotationRenderer.class);

	private static final String LAYERNAME = "annotation";

	/**
	   The name of the {@link
	   mil.navy.nrl.cmf.sousa.spatiotemporal.QueryResultHandle}
	   field that contains the name of a city, coverage, model, or point
	*/
	private final String _mapnameField = "mapname";

	// DAVID: The RESULTS_*_FIELDNAME duplicate the identically named
	// constants defined in AbstractRenderer.  They are here because
	// AnnotationRenderer services fields defined by ServerVI and so
	// their qualified names don't match those defined in
	// AbstractRenderer.  It's a consequence of prefixing each
	// Receptor field with the name of the ViewInterpreter class that
	// provides it. There must be a better way.

	/**
	   The name of the RESULTS_ADDED field provided by a
	   spatiotemporal server's ServerVI.
	 */
	private final static String RESULTS_ADDED_FIELDNAME = 
		ViewInterpreter.getQualifiedFieldName(ServerVI.class, 
											  QueryFields.RESULTS_ADDED);
	/**
	   The name of the RESULTS_CHANGED field provided by a
	   spatiotemporal server's ServerVI.
	 */
	private final static String RESULTS_CHANGED_FIELDNAME = 
		ViewInterpreter.getQualifiedFieldName(ServerVI.class, 
											  QueryFields.RESULTS_CHANGED);
	/**
	   The name of the RESULTS_REMOVED field provided by a
	   spatiotemporal server's ServerVI
	 */
	private final static String RESULTS_REMOVED_FIELDNAME = 
		ViewInterpreter.getQualifiedFieldName(ServerVI.class, 
											  QueryFields.RESULTS_REMOVED);

	public AnnotationRenderer() {}

	public void render(State.ChangeMessage msg) {
		List changes = msg.getMessages();

		for (Iterator i = changes.iterator(); i.hasNext(); ) {
			Field.ChangeMessage fnotif = (Field.ChangeMessage)i.next();
			if (RESULTS_ADDED_FIELDNAME.equals(fnotif._fname)) {
				showAdded(((Set)(fnotif._value)).iterator());
			} else if (RESULTS_REMOVED_FIELDNAME.equals(fnotif._fname)) {
				showRemoved(((Set)(fnotif._value)).iterator());
			} else if (RESULTS_CHANGED_FIELDNAME.equals(fnotif._fname)) {
				showChanged(((Set)(fnotif._value)).iterator());
			} else {
				ModelRenderer._LOG.warn(new Strings(new Object[] 
					{this, 
					 ": render(): expected field",
					 RESULTS_ADDED_FIELDNAME, " or ",
					 RESULTS_REMOVED_FIELDNAME, " or ",
					 RESULTS_CHANGED_FIELDNAME, 
					 " but found ", 
					 fnotif._fname, " instead."
					}));
			}
		}
	}

	protected void add(QueryResultHandle h) {
		for (Iterator i = _renderables.iterator(); i.hasNext(); ) {
			Renderable renderable = (Renderable)i.next();

			try {
				renderable.loadObject(LAYERNAME, h);
			} catch (IOException ex) {
				_LOG.error(new Strings(new Object[] 
					{this, ": showAdded(): while adding ",
					 h, " to ", renderable, " : ", 
					 ex}));
			}
		}
	}

	protected void change(QueryResultHandle h) {
		for (Iterator i = _renderables.iterator(); i.hasNext(); ) {
			Renderable renderable = (Renderable)i.next();

			_LOG.debug(new Strings(new Object[] 
				{"Changed ", h, " in ", renderable}));
		}
	}

	protected void remove(QueryResultHandle h) {
		for (Iterator i = _renderables.iterator(); i.hasNext(); ) {
			Renderable renderable = (Renderable)i.next();

			try {
				renderable.unloadObject(LAYERNAME, h);
			} catch (IOException ex) {
				_LOG.error(new Strings(new Object[] 
					{this, ": showRemoved(): while removing ",
					 h, " from ", renderable, " : ", 
					 ex}));
			}
		}
	}
}
