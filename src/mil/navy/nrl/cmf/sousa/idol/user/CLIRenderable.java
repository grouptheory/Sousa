package mil.navy.nrl.cmf.sousa.idol.user;
import java.awt.Color;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import mil.navy.nrl.cmf.sousa.Renderable;
import mil.navy.nrl.cmf.sousa.util.Strings;
import mil.navy.nrl.cmf.stk.XYZd;
import org.apache.log4j.*;

/**
   CLIRenderable
*/
final class CLIRenderable
    implements Renderable
{
	/**
	   _LOG
	*/
	private static final Logger _LOG = Logger.getLogger(CLIRenderable.class);

	/**
	   Part of a hack that assumes that the first image drawn on the
	   scene graph is the base earth image and ensures that the base
	   image is drawn as far from the viewer as possible.  This
	   eliminates flimmer.
	*/
	private boolean _nolayers = true;

	/**
	   Message terminator
	*/
	static final String EOC = new String(new char[] { 0 });


	// Constructors

	/**
	   CLIRenderable(Properties, QueryHandler, LocalCommandObject)
	   @methodtype ctor
	   @param p .
	   @param qh .
	   @param commandQueue .
	*/
	CLIRenderable(/*@ non_null */ Properties p)
	{ }


	// mil.navy.nrl.cmf.sousa.Renderer


	final public void
		loaddynamicmodel(String layername, String name, String filename, 
						 double scale, XYZd position)
		throws IOException
	{
		sendToZUI(OssimPlanetLanguage.addPoint(":idolbridge",
											   OssimPlanetLanguage.layername(layername),
											   OssimPlanetLanguage.id(name),
											   OssimPlanetLanguage.position(position.getY(),
																			position.getX(), 
																			position.getZ(),
																			"relative"),
											   OssimPlanetLanguage.model(filename,
																		 /* orientation */ null, 
																		 OssimPlanetLanguage.scale(scale, 
																								   scale, 
																								   scale)),
											   null, null));

	}

	final public void
		loadfluxedmodel(String layername, String name, String filename, 
						double scale, double time, XYZd position, XYZd velocity)
		throws IOException
	{
		sendToZUI(OssimPlanetLanguage.addPoint(":idolbridge",
											   OssimPlanetLanguage.layername(layername),
											   OssimPlanetLanguage.id(name),
											   OssimPlanetLanguage.position(position.getY(),
																			position.getX(), 
																			position.getZ(),
																			"relative"),
											   OssimPlanetLanguage.model(filename,
																		 /* orientation */ null, 
																		 OssimPlanetLanguage.scale(scale, 
																								   scale, 
																								   scale)),
											   null, 
											   OssimPlanetLanguage.velocity(time,
																			velocity.getY(),
																			velocity.getX(),
																			velocity.getZ())
											   ));
	}

	final public void
		loadstatictext(String layername, String name, String text, Color color,
					   double scale, XYZd position)
		throws IOException
	{
		sendToZUI(OssimPlanetLanguage.addPoint(":idolbridge",
											   OssimPlanetLanguage.layername(layername),
											   OssimPlanetLanguage.id(name),
											   OssimPlanetLanguage.position(position.getY(),
																			position.getX(), 
																			position.getZ(),
																			"relative"),
											   OssimPlanetLanguage.text(text,
																		OssimPlanetLanguage.scale(scale, 
																								  scale, 
																								  scale),
																		OssimPlanetLanguage.color((double)(color.getRed()/255.0),
																								  (double)(color.getGreen()/255.0), 
																								  (double)(color.getBlue()/255.0),
																								  (double)(color.getAlpha()/255.0))),
											   null, null));
	}

	final public void
		updatedynamicobject(String layername, String name, XYZd position)
		throws IOException
	{
		sendToZUI(OssimPlanetLanguage.movePoint(":idolbridge",
												OssimPlanetLanguage.layername(layername),
												OssimPlanetLanguage.id(name),
												OssimPlanetLanguage.position(position.getY(),
																			 position.getX(), 
																			 position.getZ(),
																			 "relative"),
												null));
	}

	final public void
		updatefluxedobject(String layername, String name, double time, 
						   XYZd position, XYZd velocity)
		throws IOException
	{
		sendToZUI(OssimPlanetLanguage.movePoint(":idolbridge",
												OssimPlanetLanguage.layername(layername),
												OssimPlanetLanguage.id(name),
												OssimPlanetLanguage.position(position.getY(),
																			 position.getX(), 
																			 position.getZ(),
																			 "relative"),
												OssimPlanetLanguage.velocity(time,
																			 velocity.getY(),
																			 velocity.getX(),
																			 velocity.getZ())
												));
	}

	final public void
		loadpatch(String layername, String name, String filename, 
				  int displacement)
		throws IOException
	{
		sendToZUI(OssimPlanetLanguage.addImage(":idolbridge",
											   OssimPlanetLanguage.layername(layername),
											   OssimPlanetLanguage.id(name),
											   OssimPlanetLanguage.image(filename),
											   OssimPlanetLanguage.description(filename)));
	}

	final public void
		unloadSceneGraphObject(String layername, String name)
		throws IOException
	{
		sendToZUI(OssimPlanetLanguage.removeObject(":idolbridge",
												   OssimPlanetLanguage.layername(layername),
												   OssimPlanetLanguage.id(name)));
	}

	public void 
		loadObject(String layername, Object obj)
		throws IOException
	{
		_LOG.info(new Strings(new Object[] 
			{ "loadObject(", layername, ", ", obj, ")" }));
	}

	public void 
		unloadObject(String layername, Object obj)
		throws IOException 
	{
		_LOG.info(new Strings(new Object[] 
			{ "unloadObject(", layername, ", ", obj, ")" }));
	}

	/**
	   deleteLayer(String, String)
	   @methodtype command
	   @param layername .
	   @throws IOException .
	*/
	final public void
		deleteLayer(String layername)
		throws IOException
	{
		sendToZUI(OssimPlanetLanguage.removeLayer(":idolbridge", 
												  OssimPlanetLanguage.layername(layername)));
	}

	final public Set getContentTypes() {
		Set answer = new HashSet();
		answer.add("x-idol/x-annotation");
		answer.add("x-idol/x-city");
		answer.add("x-idol/x-coverage");
		answer.add("x-idol/x-directory");
		answer.add("x-idol/x-model");
		answer.add("x-idol/x-point");

		return answer;
	}

	// Utility

	/**
	   sendToZUI(String)
	   @methodtype command
	   @param message .
	   @throws IOException .
	*/
	private void
		sendToZUI(String message)
		throws IOException
	{
		_LOG.info(message);
	}

}; // CLIRenderable
