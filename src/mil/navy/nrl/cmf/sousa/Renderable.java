package mil.navy.nrl.cmf.sousa;

import java.awt.Color;
import java.io.IOException;
import java.io.Serializable;
import java.util.Set;
import mil.navy.nrl.cmf.stk.XYZd;

/**
 * A Renderable is something that can be written into by a Renderer.
 * A scene graph is an example of a Renderable.
 * 
 * @version 	$Id: Renderable.java,v 1.3 2006/10/02 15:54:58 talmage Exp $
 * @author 	David Talmage
 * @author 	Dardo Kleiner
 * @author 	Patrick Melody
 */
public interface Renderable extends Serializable {
	/**
	   loaddynamicmodel(String, String, String, double, XYZd)
	   @methodtype command
	   @param layername .
	   @param name .
	   @param filename .
	   @param scale .
	   @param position .
	   @throws IOException .
	*/
	public void loaddynamicmodel(String layername, String name, String filename, 
								 double scale, XYZd position)
		throws IOException;

	/**
	   loadfluxedmodel(String, String, String, double, double, XYZd, XYZd)
	   @methodtype command
	   @param layername .
	   @param name .
	   @param filename .
	   @param scale .
	   @param time .
	   @param position .
	   @param velocity .
	   @throws IOException .
	*/
	public void loadfluxedmodel(String layername, String name, String filename, 
								double scale, double time, XYZd position, 
								XYZd velocity)
		throws IOException;

	/**
	   loadstatictext(String, String, String, XYZd, double)
	   @methodtype command
	   @param layername .
	   @param name .
	   @param text .
	   @param color .
	   @param scale .
	   @param position .
	   @throws IOException .
	*/
	public void loadstatictext(String layername, String name, String text, 
							   Color color, double scale, XYZd position)
		throws IOException;

	/**
	   updatedynamicobject(String, String, XYZd)
	   @param layername .
	   @param name .
	   @param position .
	   @throws IOException .
	*/
	public void updatedynamicobject(String layername, String name, 
									XYZd position)
		throws IOException;

	/**
	   updatefluxedobject(String, String, double, XYZd, XYZd)
	   @param layername .
	   @param name .
	   @param time .
	   @param position .
	   @param velocity .
	   @throws IOException .
	*/
	public void updatefluxedobject(String layername, String name, double time, 
								   XYZd position, XYZd velocity)
		throws IOException;

	/**
	   loadpatch(String, String, String, int)
	   @methodtype command
	   @param layername .
	   @param name .
	   @param filename .
	   @param displacement .
	   @throws IOException .
	*/
	public void loadpatch(String layername, String name, String filename, 
						  int displacement)
		throws IOException;

	/**
	   unloadobject(String, String)
	   @methodtype command
	   @param layername .
	   @param name .
	   @throws IOException .
	*/
	public void unloadSceneGraphObject(String layername, String name)
		throws IOException;

	public void loadObject(String layername, Object obj)
		throws IOException;

	public void unloadObject(String layername, Object obj)
		throws IOException;

	/**
	   deleteLayer(String)
	   @methodtype command
	   @param layername .
	   @throws IOException .
	*/
	public void deleteLayer(String layername)
		throws IOException;

	/**
	   getContentTypes()

	   Answers the question, "Which RFC2045 MIME ContentTypes do you
	   support?"  It's all of the things that can be rendered in this
	   Renderable.

	   @return a Set of the ContentTypes that this Renderable supports
	 */
	public Set getContentTypes();
}
