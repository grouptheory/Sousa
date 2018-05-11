package mil.navy.nrl.cmf.sousa;

import java.io.Serializable;
import java.util.List;

/**
 * A Renderer writes into a Renderable.  It's an output interpreter
 * for State.ChangeMessage.
 * 
 * @version 	$Id: Renderer.java,v 1.2 2006/10/02 15:54:58 talmage Exp $
 * @author 	David Talmage
 */
public interface Renderer extends Serializable {
	/**
	   This Renderer writes into each of its Renderables.
	 */
	public void render(State.ChangeMessage msg);

	/**
	   Tells this Renderer the Renderables into which it may write.

	   @param renderables List of Renderable
	 */
	public void setRenderables(List renderables);
}
