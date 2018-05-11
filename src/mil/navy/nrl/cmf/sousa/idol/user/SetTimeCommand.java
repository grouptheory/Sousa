package mil.navy.nrl.cmf.sousa.idol.user;
import java.util.Calendar;

public final class SetTimeCommand extends AbstractLocalCommand {

	private final Calendar _timeLower;
	private final Calendar _timeUpper;

	public SetTimeCommand(Calendar timeLowerBound, Calendar timeUpperBound) {
		_timeLower = (Calendar)timeLowerBound.clone();
		_timeUpper = (Calendar)timeUpperBound.clone();
	}

	public void execute(LocalCommandObject c) {
		c.setTime(_timeLower, _timeUpper);
	}
}
