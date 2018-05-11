package mil.navy.nrl.cmf.sousa.idol.user;

public final class ShutdownCommand extends AbstractLocalCommand
{
	public ShutdownCommand() {}

	public void execute(LocalCommandObject c) {
		c.shutdown();
	}
}