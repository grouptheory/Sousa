package mil.navy.nrl.cmf.sousa.idol.user;
import java.util.Calendar;
import mil.navy.nrl.cmf.sousa.spatiotemporal.Vector3d;

/**
 * Set Position and Time atomically
 */
public final class SetPositionAndTimeCommand extends AbstractLocalCommand {
	private final Calendar _timeLower;
	private final Calendar _timeUpper;
	private final Vector3d _position;
	private final Vector3d _width;
	private final Vector3d _hpr;

	/*
	 * width and hpr are optional.
	 */
	public SetPositionAndTimeCommand(Vector3d position, 
									 Vector3d width,
									 Vector3d hpr,
									 Calendar timeLowerBound, 
									 Calendar timeUpperBound) {

		_position = new Vector3d(position);
		if (null != width)
			_width = new Vector3d(width);
		else 
			_width = null;
		if (null != hpr)
			_hpr = new Vector3d(hpr);
		else
			_hpr = null;

		_timeLower = (Calendar)timeLowerBound.clone();
		_timeUpper = (Calendar)timeUpperBound.clone();
	}

	public void execute(LocalCommandObject c) {
		c.setPosition(_position, _width, _hpr);
		c.setTime(_timeLower, _timeUpper);
	}
}
