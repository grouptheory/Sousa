package edu.psu.arl.uaim.sousa;

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
import mil.navy.nrl.cmf.sousa.idol.user.*;
import mil.navy.nrl.cmf.stk.*;
import org.apache.log4j.Logger;


public final class UAIMRenderer extends AbstractRenderer {
	protected static final Logger _LOG = Logger.getLogger(UAIMRenderer.class);

	private static final String LAYERNAME = "UAIM";
	private static final int _LAYER = 1;
	

        private static final String _mapnameField = "mapname";

        /**
         * Default Constructor 
         */
	public UAIMRenderer() {
        }

	protected void add(QueryResultHandle h) {
            _LOG.info("UAIMRenderer add");
	}

        /**
         * remove method removes the layer from the renderer
         */
	protected void remove(QueryResultHandle h) {
            _LOG.info("UAIMRenderer remove");
	    String name = (String)h.getFieldValue(_mapnameField);
	    removeObject(LAYERNAME, name);
	}

	protected void change(QueryResultHandle h) {
            _LOG.info("UAIMRenderer change");
	}
}
