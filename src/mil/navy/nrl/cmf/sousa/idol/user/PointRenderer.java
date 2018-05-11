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


public final class PointRenderer extends AbstractRenderer {
	protected static final Logger _LOG = Logger.getLogger(PointRenderer.class);

	private static final String LAYERNAME = "point";

	/**
	   The name of the {@link
	   mil.navy.nrl.cmf.sousa.spatiotemporal.QueryResultHandle}
	   field that contains the name of a city, coverage, model, or point
	*/
	private static final String _mapnameField = "mapname";

	public PointRenderer() {}

	// If h has a velocity field, then add the point as a fluxed object.
	// Otherwise, add it as a dynamic object.
	//
	// TODO: If this is too much for Performer, then add a point
	// without velocity as a static object.
	//
	protected void add(QueryResultHandle h) {
		String name = (String)h.getFieldValue(_mapnameField);
		final Vector3d position = (Vector3d)h.getFieldValue(QueryClientFields.POSITION_FIELDNAME);
		final Vector3d velocity = (Vector3d)h.getFieldValue("velocity");
		final Long time = (Long)h.getFieldValue("time");

		for (Iterator i = _renderables.iterator(); i.hasNext(); ) {
			Renderable renderable = (Renderable)i.next();

			try {
				if (null != velocity) {
					renderable.loadfluxedmodel(LAYERNAME, name, 
											   "sphere-blue", 0.005, 
											   (double)time.longValue(), new XYZd() {
													   // ZUI expects lon/lat/elev, position is lat/lon/elev
													   public double getX() { return position.y; }
													   public double getY() { return position.x; }
													   public double getZ() { return position.z; }
													   public void setX(double x) { }
													   public void setY(double y) { }
													   public void setZ(double z) { }
													   public void set(double x, double y, double z) { }
												   }, new XYZd() {
														   public double getX() { return velocity.y; }
														   public double getY() { return velocity.x; }
														   public double getZ() { return velocity.z; }
														   public void setX(double x) { }
														   public void setY(double y) { }
														   public void setZ(double z) { }
														   public void set(double x, double y, double z) { }
													   });
				} else {
					renderable.loaddynamicmodel(LAYERNAME, name, 
												"sphere-blue", 0.005, 
												new XYZd() {
													// ZUI expects lon/lat/elev, position is lat/lon/elev
													public double getX() { return position.y; }
													public double getY() { return position.x; }
													public double getZ() { return position.z; }
													public void setX(double x) { }
													public void setY(double y) { }
													public void setZ(double z) { }
													public void set(double x, double y, double z) { }
												});
				}
			} catch (IOException e) {
				e.printStackTrace(System.err);
			}
		}
	}


	protected void remove(QueryResultHandle h) {
		String name = (String)h.getFieldValue(_mapnameField);

		removeObject(LAYERNAME, name);
	}

	// If h has a velocity field, then change the point as a fluxed object.
	// Otherwise, change it as a dynamic object.
	//
	// TODO: If this is too much for Performer, then change a point
	// without velocity as a static object.
	//
	protected void change(QueryResultHandle h) {
		String name = (String)h.getFieldValue(_mapnameField);
		final Vector3d position = (Vector3d)h.getFieldValue(QueryClientFields.POSITION_FIELDNAME);
		final Vector3d velocity = (Vector3d)h.getFieldValue("velocity");
		final Long time = (Long)h.getFieldValue("time");

		for (Iterator i = _renderables.iterator(); i.hasNext(); ) {
			Renderable renderable = (Renderable)i.next();

			try {
				if (null != velocity) {
					renderable.updatefluxedobject(LAYERNAME, name,
												  (double)time.longValue(), new XYZd() {
														  // ZUI expects lon/lat/elev, position is lat/lon/elev
														  public double getX() { return position.y; }
														  public double getY() { return position.x; }
														  public double getZ() { return position.z; }
														  public void setX(double x) { }
														  public void setY(double y) { }
														  public void setZ(double z) { }
														  public void set(double x, double y, double z) { }
													  }, new XYZd() {
															  public double getX() { return velocity.y; }
															  public double getY() { return velocity.x; }
															  public double getZ() { return velocity.z; }
															  public void setX(double x) { }
															  public void setY(double y) { }
															  public void setZ(double z) { }
															  public void set(double x, double y, double z) { }
														  });
				} else {
					renderable.updatedynamicobject(LAYERNAME, name, 
												   new XYZd() {
													   // ZUI expects lon/lat/elev, position is lat/lon/elev
													   public double getX() { return position.y; }
													   public double getY() { return position.x; }
													   public double getZ() { return position.z; }
													   public void setX(double x) { }
													   public void setY(double y) { }
													   public void setZ(double z) { }
													   public void set(double x, double y, double z) { }
												   });
				}
			} catch (IOException e) {
				e.printStackTrace(System.err);
			}
		}
	}
}
