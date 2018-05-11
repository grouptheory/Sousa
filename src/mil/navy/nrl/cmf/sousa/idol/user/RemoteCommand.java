package mil.navy.nrl.cmf.sousa.idol.user;

import mil.navy.nrl.cmf.sousa.ServerContact;

public interface RemoteCommand extends Command {
	public ServerContact getServerContact();
	public void execute(RemoteCommandObject c);
}
