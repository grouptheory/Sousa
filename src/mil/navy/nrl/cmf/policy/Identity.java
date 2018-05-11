package mil.navy.nrl.cmf.policy;

import java.io.Serializable;

public class Identity implements Serializable {
	private String _user = "";
	private String _location = "";

	// Required by the Skaringa XML serialization framework.
	private Identity() {
	}

	public Identity(String user, String location) {
		_user = user;
		_location = location;
	}

	public String user() {
		return _user;
	}

	public String location() {
		return _location;
	}

	public String toString() {
		return _user + "@" + _location;
	}
}
