package mil.navy.nrl.cmf.sousa.idol.user;

import java.awt.Color;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import mil.navy.nrl.cmf.sousa.Field;
import mil.navy.nrl.cmf.sousa.Renderer;
import mil.navy.nrl.cmf.sousa.Renderable;
import mil.navy.nrl.cmf.sousa.State;
import mil.navy.nrl.cmf.sousa.spatiotemporal.*;
import mil.navy.nrl.cmf.sousa.util.Strings;
import mil.navy.nrl.cmf.stk.*;
import org.apache.log4j.Logger;


public final class CoverageRenderer extends AbstractRenderer {
	protected static final Logger _LOG = Logger.getLogger(CoverageRenderer.class);

	private static final String LAYERNAME = "raster";
	private static final int _LAYER = 1;

	/**
	   The name of the {@link
	   mil.navy.nrl.cmf.sousa.spatiotemporal.QueryResultHandle}
	   field that contains the name of a city, coverage, model, or coverage
	*/
	private static final String _mapnameField = "mapname";

	public CoverageRenderer() {}

	protected void add(QueryResultHandle h) {
		String name = (String)h.getFieldValue(_mapnameField);

		for (Iterator i = _renderables.iterator(); i.hasNext(); ) {
			Renderable renderable = (Renderable)i.next();
		
			// Use name as basepath 
			try {
				CoverageRenderer._LOG.info(new Strings(new Object[] 
					{this, 
					 ": Telling Renderable to load spherepatch \"",
					 name, "\" in layer ", LAYERNAME, " of ", renderable
					} ));

				// Assume whole-earth image is first loaded and should be
				// in "base" layer with displacement 0, set subsequent images
				// to "raster" layer with displacement 1
				renderable.loadpatch(LAYERNAME, name, name, _LAYER);

			} catch (IOException ex) {
				CoverageRenderer._LOG.error(new Strings(new Object[] 
					{this, ": error loading coverage ",
					 name, ": ", ex
					} ));
			} catch (IllegalArgumentException ex) {
				CoverageRenderer._LOG.error(new Strings(new Object[] 
					{this, ": error loading coverage ",
					 name, ": ", ex
					} ));
			}
		}
	}


	protected void remove(QueryResultHandle h) {
		String name = (String)h.getFieldValue(_mapnameField);

		removeObject(LAYERNAME, name);
	}

	protected void change(QueryResultHandle h) {
		String name = (String)h.getFieldValue(_mapnameField);

		CoverageRenderer._LOG.info(new Strings(new Object[] 
			{this, 
			 ": Telling Renderable to change spherepatch \"",
			 name, "\" in layer ", LAYERNAME
			} ));

		removeObject(LAYERNAME, name);

		for (Iterator i = _renderables.iterator(); i.hasNext(); ) {
			Renderable renderable = (Renderable)i.next();

			// Use name as basepath 
			try {
				renderable.loadpatch(LAYERNAME, name, name, _LAYER);
			} catch (IOException ex) {
				ClientInitializer._LOG.error(new Strings(new Object[] 
					{this, ": error loading spherepatch ",
					 name, " in ", renderable, ": ", ex
					} ));
			}
		}
	}
}
