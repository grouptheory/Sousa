package mil.navy.nrl.cmf.sousa.idol.user;

import mil.navy.nrl.cmf.sousa.Receptor;
import mil.navy.nrl.cmf.sousa.ServerContact;

public interface RemoteCommandObject {
	public Receptor getReceptor(ServerContact s);
}
