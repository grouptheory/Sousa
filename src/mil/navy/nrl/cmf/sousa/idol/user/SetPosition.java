package mil.navy.nrl.cmf.sousa.idol.user;

import mil.navy.nrl.cmf.sousa.spatiotemporal.Vector3d;

public interface SetPosition {
	// position must not be null.
	// width and hpr may be null.
	public void setPosition(Vector3d position, Vector3d width, Vector3d hpr);
}
