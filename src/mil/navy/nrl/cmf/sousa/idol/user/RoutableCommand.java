package mil.navy.nrl.cmf.sousa.idol.user;

public interface RoutableCommand extends Command {
    public Object getSource();
    public void setSource(Object s);
}
