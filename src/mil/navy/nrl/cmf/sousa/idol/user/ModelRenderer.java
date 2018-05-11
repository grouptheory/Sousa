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


public final class ModelRenderer extends AbstractRenderer {
	protected static final Logger _LOG = Logger.getLogger(ModelRenderer.class);

	private static final String LAYERNAME = "model";

	/**
	   The name of the {@link
	   mil.navy.nrl.cmf.sousa.spatiotemporal.QueryResultHandle}
	   field that contains the name of a city, coverage, model, or model
	*/
	private static final String _mapnameField = "mapname";

	/**
	   The name of the {@link
	   mil.navy.nrl.cmf.sousa.spatiotemporal.QueryResultHandle} field
	   that contains the latitude of a city or a model.
	*/
	private final String _modelLatitudeField = "north";

	/**
	   The name of the {@link
	   mil.navy.nrl.cmf.sousa.spatiotemporal.QueryResultHandle} field
	   that contains the longitude of a city or a model.
	*/
	private final String _modelLongitudeField = "east";

	public ModelRenderer() {}

	// If h has a velocity field, then add the model as a fluxed object.
	// Otherwise, add it as a dynamic object.
	//
	// TODO: If this is too much for Performer, then add a model
	// without velocity as a static object.
	//
	protected void add(QueryResultHandle h) {
		// mapname is the name of the model
		String name = (String)h.getFieldValue(_mapnameField);

		// minelev is elevation
		// maxelev is elevation, the same value as minelev

		// north is latitude
		final Double latitude = 
			(Double)h.getFieldValue(_modelLatitudeField);

		// east is longitude
		final Double longitude = 
			(Double)h.getFieldValue(_modelLongitudeField);
					

		for (Iterator i = _renderables.iterator(); i.hasNext(); ) {
			Renderable renderable = (Renderable)i.next();

			try {
				ModelRenderer._LOG.info(new Strings(new Object[] 
					{this, ": add(): Telling Renderer to load model \"", 
					 name, "\" in ", renderable, 
					 " at latitude ", latitude, 
					 " longitude ", longitude
					} ));

				renderable.loaddynamicmodel(LAYERNAME,
											name, name, 0.0000001552D, new XYZd() {
													// ZUI expects lon/lat/elev
													public double getX() { return longitude.doubleValue(); }
													public double getY() { return latitude.doubleValue(); }
													public double getZ() { return 0.0; }
													public void setX(double x) { }
													public void setY(double y) { }
													public void setZ(double z) { }
													public void set(double x, double y, double z) { }
												});
			} catch (IOException ex) {
				ModelRenderer._LOG.error(new Strings(new Object[] 
					{this, ": add(): error loading model ",
					 name, " in ", renderable, ": ", ex
					} ));
			}
		}
	}

	// If h has a velocity field, then change the model as a fluxed object.
	// Otherwise, change it as a dynamic object.
	//
	// TODO: If this is too much for Performer, then change a model
	// without velocity as a static object.
	//
	protected void change(QueryResultHandle h) {
		for (Iterator i = _renderables.iterator(); i.hasNext(); ) {
			Renderable renderable = (Renderable)i.next();

			// mapname is the name of the model
			String name = (String)h.getFieldValue(_mapnameField);

			// minelev is elevation
			// maxelev is elevation, the same value as minelev

			// north is latitude
			final Double latitude = 
				(Double)h.getFieldValue(_modelLatitudeField);

			// east is longitude
			final Double longitude = 
				(Double)h.getFieldValue(_modelLongitudeField);
					
			try {
				ModelRenderer._LOG.info(new Strings(new Object[] 
					{this, ": change(): Telling Renderer to change model \"", 
					 name, "\" in ", renderable, 
					 " at latitude ", latitude, 
					 " longitude ", longitude
					} ));

				renderable.updatedynamicobject(LAYERNAME,
											   name, new XYZd() {
													   // ZUI expects lon/lat/elev
													   public double getX() { return longitude.doubleValue(); }
													   public double getY() { return latitude.doubleValue(); }
													   public double getZ() { return 0.0; }
													   public void setX(double x) { }
													   public void setY(double y) { }
													   public void setZ(double z) { }
													   public void set(double x, double y, double z) { }
												   });
			} catch (IOException ex) {
				ModelRenderer._LOG.error(new Strings(new Object[] 
					{this, ": change(): error changing model ",
					 name, " in ", renderable, ": ", ex
					} ));
			}
		}
	}

	protected void remove(QueryResultHandle h) {
		String name = (String)h.getFieldValue(_mapnameField);

		removeObject(LAYERNAME, name);
	}
}
