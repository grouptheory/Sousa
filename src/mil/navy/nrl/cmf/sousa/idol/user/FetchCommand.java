package mil.navy.nrl.cmf.sousa.idol.user;
import mil.navy.nrl.cmf.sousa.ServerContact;

public class FetchCommand extends AbstractLocalCommand {
    private final ServerContact _serverContact;
    private final int _session;

    public FetchCommand(ServerContact sc, int session) {
		_serverContact = sc;
		_session = session;
    }

    public void execute(LocalCommandObject c) {
	c.scheduleFetch(_serverContact, _session);
    }
}
