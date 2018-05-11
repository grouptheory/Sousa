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


public final class CityRenderer extends AbstractRenderer {
	protected static final Logger _LOG = Logger.getLogger(CityRenderer.class);

	private static final String LAYERNAME = "city";

	/**
	   The name of the {@link
	   mil.navy.nrl.cmf.sousa.spatiotemporal.QueryResultHandle}
	   field that contains the name of a city, coverage, model, or point
	*/
	private final String _mapnameField = "mapname";
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

	/**
	   The name of the {@link
	   mil.navy.nrl.cmf.sousa.spatiotemporal.QueryResultHandle}
	   field that contains the population of a city.
	*/
	private final String _cityPopulationField = "pop";

	private final String _cityElevationField = "minelev";

	public CityRenderer() {}

	protected void add(QueryResultHandle h) {
		String name = (String)h.getFieldValue(_mapnameField);
		final Double latitude = 
			(Double)h.getFieldValue(_modelLatitudeField);
		final Double longitude = 
			(Double)h.getFieldValue(_modelLongitudeField);
		final Integer elevation =
			(Integer)h.getFieldValue(_cityElevationField);

		// DAVID: As of April, 2007, USGS no longer includes
		// population in the gnis information.  Consequently, our city
		// name database doesn't have population.

		/* DAVID: Magic number just to get us started.  This will go
		 away eventually.
		*/
		//		int population = 1000000; 

		/*
		Integer population = 
			(Integer)h.getFieldValue(_cityPopulationField);
		*/

		// DAVID: It would be nice to scale the label
		// as a function of population.

		// Scale values:
		// 1.0D too big -- labels are way out in space
		// 0.5D too big -- labels are way out in space

		// 0.0001D a little too big if all labels are that size

		// 0.00005D just right if all labels are that size.
		// It's best seen at 10000 m.

		// 0.0000001552D too small

		// elevation	scale
		// 100000000
		// 10000000
		// 1000000
		// 100000	0.00005D
		// 10000
		// 1000
		// 100

		// Patrick wrote: when i made an annotation label, the scale was:

		// float labelScale = lerp(clamp(elev()/2500000.0f, 0.0f, 1.0f), 0.00000015f, 0.011f);

		// elev() is user elevation in meters.  labelScale might have to be scaled by
		// the earth radius, since globe1 used a unit radius sphereoid, but the new
		// app uses a real life radius sphereoid.

		// // linear interpolation from start to stop by x
		// lerp(x, start, stop) = x * (stop-start) + start;
					
		// // clamp x to [a,b]
		// // a <= b
		// clamp(x, a, b) = min(max(x, a), b);

		// double scale = scaleByElevation(population.intValue());

		CityRenderer._LOG.info(new Strings(new Object[] 
			{this, ": Telling ZUI to display label \"", 
			 name, "\" at latitude ", latitude, 
			 " longitude ", longitude
			} ));

		for (Iterator i = _renderables.iterator(); i.hasNext(); ) {
			Renderable renderable = (Renderable)i.next();

			try {
					
				renderable.loadstatictext(LAYERNAME,
										  name, name, Color.WHITE, .0005 /*scale*/, new XYZd()
											  {
												  // ZUI expects lon/lat/elev
												  public double getX() { return longitude.doubleValue(); }
												  public double getY() { return latitude.doubleValue(); }
												  public double getZ() { return elevation.doubleValue(); }
												  public void setX(double x) { }
												  public void setY(double y) { }
												  public void setZ(double z) { }
												  public void set(double x, double y, double z) { }
											  });
			} catch (IOException ex) {
				CityRenderer._LOG.error(new Strings(new Object[] 
					{this, ": error displaying label ",
					 name, " in ", renderable, ": ", ex
					} ));
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
		String name = (String)h.getFieldValue(_mapnameField);

		removeObject(LAYERNAME, name);
	}

	// Utility

	/**
	   Maps elevation into a scale value for displaying text in the ZUI.
	   The larger the elevation, the bigger the text will be.
	   <P>
	   <EM>TODO: Get a real explanation from Patrick</EM>
	*/
	private double scaleByElevation(int elevation) 
	{
		// Note magic constants here.  If you change them here, change
		// them in scaleByElevation(double).
		//
		return lerp(clamp(elevation/2500000.0D, 0.0D, 1.0D), 0.00000015D, 0.011D);
	}

	/**
	   Force x to be between a and b, inclusive.  In the jargon,
	   "clamp x to [a,b]".

	   @param x the victim
	   @param a the lower bound
	   @param b the upper bound
	*/
	private double clamp(double x, double a, double b) 
	{
		return Math.min(Math.max(x, a), b);
	}

	/**
	   Linear interpolation from start to stop by x

	   @param x the step
	   @param start the lower bound
	   @param stop the upper bound
	*/
	private double lerp(double x, double start, double stop) 
	{
		return x * (stop - start) + start;
	}
}
