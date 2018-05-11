package mil.navy.nrl.cmf.sousa.idol.user;

/**
   Convenience methods for creating strings in the
   language for controlling OssimPlanet.
*/
final public class OssimPlanetLanguage {
	/**
	   Change position, heading, pitch, and roll
	 */
	static public String setlatlonelevhpr(/* non-null */ String actionReceiverName,
										  double longitude, double latitude, 
										  double elevation,
										  double heading, double pitch, 
										  double roll)
		throws IllegalArgumentException
	{
		StringBuffer buf = new StringBuffer();
		buf.append(actionReceiverName);
		buf.append(" setlatlonelevhpr ");
		buf.append(Double.toString(latitude));
		buf.append(" ");
		buf.append(Double.toString(longitude));
		buf.append(" ");
		buf.append(Double.toString(elevation));
		buf.append(" ");
		buf.append(Double.toString(heading));
		buf.append(" ");
		buf.append(Double.toString(pitch));
		buf.append(" ");
		buf.append(Double.toString(roll));

		return buf.toString();
	}

	/**
	   Add a point object to the scene graph.

	   @param actionReceiverName the name of the OssimPlanet subsystem to which this command is directed.  Must not be null.  E.g. ":idolbridge".

	   @param layerValue a layer constructed by OssimPlanetLayer.layername().  Must not be null.
	   @param idValue an identifier constructed by OssimPlanetLayer.id().  Must not be null.
	   @param positionValue a position constructed by OssimPlanetLayer.position().  Must not be null.
	   @param geometryValue a geometry constructed by one of the OssimPlanetLayer geometry methods.  Must not be null.
	   @param descriptionValue description constructed by OssimPlanetLayer.description().  May be null.
	   @param velocityValue velocity constructed by OssimPlanetLayer.velocity().  May be null.
	*/
	static public String addPoint(/* non-null */ String actionReceiverName,
								  /* non-null */ String layerValue, 
								  /* non-null */ String idValue,
								  /* non-null */ String positionValue,
								  /* non-null */ String geometryValue,
								  /* optional */ String descriptionValue,
								  /* optional */ String velocityValue)
		throws IllegalArgumentException
	{
		if (null == actionReceiverName)
			throw new IllegalArgumentException("Argument 'actionReceiverName' must not be null");

		if (null == layerValue)
			throw new IllegalArgumentException("Argument 'layerValue' must not be null");

		if (null == idValue)
			throw new IllegalArgumentException("Argument 'idValue' must not be null");

		if (null == positionValue)
			throw new IllegalArgumentException("Argument 'positionValue' must not be null");

		if (null == geometryValue)
			throw new IllegalArgumentException("Argument 'geometryValue' must not be null");

		StringBuffer buf = new StringBuffer();
		buf.append(actionReceiverName);
		buf.append(" addChild " );
		buf.append(layerValue);
		buf.append(idValue);
		if (null != descriptionValue)
			buf.append(descriptionValue);
		buf.append(positionValue);
		if (null != velocityValue)
			buf.append(velocityValue);
		buf.append(geometryValue);

		return buf.toString();
	}

	/**
	   Move a point object that is already in the scene graph.

	   @param actionReceiverName the name of the OssimPlanet subsystem to which this command is directed.  Must not be null.  E.g. ":idolbridge".

	   @param layerValue a layer constructed by OssimPlanetLayer.layername().  Must not be null.
	   @param idValue an identifier constructed by OssimPlanetLayer.id().  Must not be null.
	   @param positionValue a position constructed by OssimPlanetLayer.position().  Must not be null.
	   @param velocityValue velocity constructed by OssimPlanetLayer.velocity().  May be null.
	*/
	static public String movePoint(/* non-null */ String actionReceiverName,
								  /* non-null */ String layerValue, 
								  /* non-null */ String idValue,
								  /* non-null */ String positionValue,
								  /* optional */ String velocityValue)
		throws IllegalArgumentException
	{
		if (null == actionReceiverName)
			throw new IllegalArgumentException("Argument 'actionReceiverName' must not be null");

		if (null == layerValue)
			throw new IllegalArgumentException("Argument 'layerValue' must not be null");

		if (null == idValue)
			throw new IllegalArgumentException("Argument 'idValue' must not be null");

		if (null == positionValue)
			throw new IllegalArgumentException("Argument 'positionValue' must not be null");

		StringBuffer buf = new StringBuffer();
		buf.append(actionReceiverName);
		buf.append(" updateChild " );
		buf.append(layerValue);
		buf.append(idValue);
		buf.append(positionValue);
		if (null != velocityValue)
			buf.append(velocityValue);

		return buf.toString();
	}

	/**
	   Add an image to the scene graph.

	   @param actionReceiverName the name of the OssimPlanet subsystem to which this command is directed.  Must not be null.  E.g. ":idolbridge".

	   @param layerValue a layer constructed by OssimPlanetLayer.layername().  Must not be null.
	   @param idValue an identifier constructed by OssimPlanetLayer.id().  Must not be null.
	   @param imageValue an image constructed by OssimPlanetLayer.image().  Must not be null.
	   @param descriptionValue description constructed by OssimPlanetLayer.description().  May be null.
	*/
	static public String addImage(/* non-null */ String actionReceiverName,
								  /* non-null */ String layerValue, 
								  /* non-null */ String idValue,
								  /* non-null */ String imageValue,
								  /* optional */ String descriptionValue)
		throws IllegalArgumentException
	{
		if (null == actionReceiverName)
			throw new IllegalArgumentException("Argument 'actionReceiverName' must not be null");

		if (null == layerValue)
			throw new IllegalArgumentException("Argument 'layerValue' must not be null");

		if (null == idValue)
			throw new IllegalArgumentException("Argument 'idValue' must not be null");

		if (null == imageValue)
			throw new IllegalArgumentException("Argument 'imageValue' must not be null");

		if (null == descriptionValue)
			throw new IllegalArgumentException("Argument 'descriptionValue' must not be null");

		StringBuffer buf = new StringBuffer();
		buf.append(actionReceiverName);
		buf.append(" addChild " );
		buf.append(layerValue);
		buf.append(idValue);
		if (null != descriptionValue)
			buf.append(descriptionValue);
		buf.append(imageValue);

		return buf.toString();
	}

	/**
	   Remove an object from a layer of the scene graph.

	   @param actionReceiverName the name of the OssimPlanet subsystem to which this command is directed.  Must not be null.  E.g. ":idolbridge".

	   @param layerValue a layer constructed by OssimPlanetLayer.layername().  Must not be null.
	   @param idValue an identifier constructed by OssimPlanetLayer.id().  Must not be null.
	*/
	static public String removeObject(/* non-null */ String actionReceiverName,
									  /* non-null */ String layerValue, 
									  /* non-null */ String idValue)
		throws IllegalArgumentException
	{
		if (null == actionReceiverName)
			throw new IllegalArgumentException("Argument 'actionReceiverName' must not be null");

		if (null == layerValue)
			throw new IllegalArgumentException("Argument 'layerValue' must not be null");

		if (null == idValue)
			throw new IllegalArgumentException("Argument 'idValue' must not be null");

		StringBuffer buf = new StringBuffer();
		buf.append(actionReceiverName);
		buf.append(" removeChild " );
		buf.append(layerValue);
		buf.append(idValue);

		return buf.toString();
	}

	/**
	   Remove a scene graph layer and its contents.

	   @param actionReceiverName the name of the OssimPlanet subsystem to which this command is directed.  Must not be null.  E.g. ":idolbridge".

	   @param layerValue a layer constructed by OssimPlanetLayer.layername().  Must not be null.
	*/
	static public String removeLayer(/* non-null */ String actionReceiverName,
									 /* non-null */ String layerValue)
		throws IllegalArgumentException
	{
		if (null == actionReceiverName)
			throw new IllegalArgumentException("Argument 'actionReceiverName' must not be null");

		if (null == layerValue)
			throw new IllegalArgumentException("Argument 'layerValue' must not be null");

		StringBuffer buf = new StringBuffer();
		buf.append(actionReceiverName);
		buf.append(" removeLayer " );
		buf.append(layerValue);

		return buf.toString();
	}

	//
	// Geometry is model, icon, or text
	//

	static public String model(/* non-null */ String filename,
							   /* optional */ String orientationValue,
							   /* optional */ String scaleValue)
	{
		StringBuffer buf = new StringBuffer();
		buf.append("{Model ");
		buf.append(OssimPlanetLanguage.filename(filename));
		if (null != orientationValue)
			buf.append(orientationValue);
		if (null != scaleValue)
			buf.append(scaleValue);

		return buf.toString();
	}

	static public String icon(/* non-null */ String name)
	{
		return "{Icon " + filename(name) + "} ";
	}

	static public String text(/* non-null */ String value,
							  /* optional */ String scaleValue,
							  /* optional */ String colorValue)
	{
		StringBuffer buf = new StringBuffer();
		buf.append("{Text ");
		buf.append(label(value));
		if (null != scaleValue)
			buf.append(scaleValue);
		if (null != colorValue)
			buf.append(colorValue);

		return buf.toString();
	}


	//
	// Methods for creating the components of statements in the
	// OssimPlanet language
	//


	static public String color(double red, double green, double blue, 
							   double alpha)
	{
		return "{Color " + red + " " + green + " " + blue + " " + alpha + "} ";
	}

	static public String description(/* non-null */ String name)
		throws IllegalArgumentException
	{
		if (null == name)
			throw new IllegalArgumentException("Argument 'name' must not be null");

		return "{Description " + name + "} ";
	}

	static public String filename(/* non-null */ String name) 
		throws IllegalArgumentException
	{
		if (null == name)
			throw new IllegalArgumentException("Argument 'name' must not be null");

		return "{Filename " + name + "} ";
	}

	static public String id(/* non-null */ String name)
		throws IllegalArgumentException
	{
		if (null == name)
			throw new IllegalArgumentException("Argument 'name' must not be null");

		return "{Id " + name + "} ";
	}

	static public String label(/* non-null */ String value)
		throws IllegalArgumentException
	{
		if (null == value)
			throw new IllegalArgumentException("Argument 'value' must not be null");

		return "{Label " + value + "} ";
	}

	static public String layername(/* non-null */ String name) 
		throws IllegalArgumentException
	{
		if (null == name)
			throw new IllegalArgumentException("Argument 'name' must not be null");
		return "{LayerName " + name + "} ";
	}

	static public String orientation(double heading, double pitch, double roll)
	{
		return "{Orientation " + heading + " " + pitch + " " + roll + "} ";
	}

	// TODO: an enum for type: relative, clamp, absolute
	static public String position(double latitude, double longitude, 
								  double elevation,
								  /* non-null */ String type)
		throws IllegalArgumentException
	{
		if (null == type)
			throw new IllegalArgumentException("Argument 'type' must not be null");

		return "{Position " + latitude + " "  + longitude + " " +
			elevation + " " + type + "} " ;
	}

	static public String scale(double scaleX, double scaleY, double scaleZ)
	{
		return "{Scale " + scaleX + " " + scaleY + " " + scaleZ + "} ";
	}

	static public String velocity(double time, 
								  double deltaLatitude, double deltaLongitude, 
								  double deltaElevation)
	{
		return "{Velocity " + time + " " +
			deltaLatitude + " " + deltaLongitude + " " +
			deltaElevation + "} ";
	}

	// Image-specific text
	/**
	   @param name the name of an image file.
	 */
	static public String image(String name)
	{
		return "{Image " + filename(name) + "} ";
	}
}
