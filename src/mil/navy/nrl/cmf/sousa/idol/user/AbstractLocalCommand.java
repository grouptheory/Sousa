package mil.navy.nrl.cmf.sousa.idol.user;

public abstract class AbstractLocalCommand implements LocalCommand, RoutableCommand {
    private Object _source = null;

    public AbstractLocalCommand() {};

    // mil.navy.nrl.cmf.sousa.idol.user.RoutableCommand
    public final Object getSource() {
	return _source;
    }

    public final void setSource(Object s) {
	_source = s;
    }
}
