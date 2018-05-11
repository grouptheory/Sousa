package mil.navy.nrl.cmf.sousa.idol.user;
import mil.navy.nrl.cmf.sousa.spatiotemporal.Vector3d;

public class SetPositionCommand extends AbstractLocalCommand {
	private final Vector3d _position; // longitude, latitude, elevation
	private final Vector3d _width; // degrees longitude, degrees latitude, meters elevation
	private final Vector3d _hpr; // heading, pitch, roll

	/* position is mandatory
	 * width is optional
	 * hpr is optional
	 */
    public SetPositionCommand(Vector3d position, Vector3d width, Vector3d hpr)
	{
		_position = new Vector3d(position);
		if (null != width) 
			_width = new Vector3d(width);
		else _width = null;

		if (null != hpr) 
			_hpr = new Vector3d(hpr);
		else 
			_hpr = null;
	}

	public final void execute(LocalCommandObject c) {
		c.setPosition(_position, _width, _hpr);
	}
}
