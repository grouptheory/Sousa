package mil.navy.nrl.cmf.sousa.idol.user;

import mil.navy.nrl.cmf.sousa.Clock;
import mil.navy.nrl.cmf.sousa.idol.Console;

public interface Client
	extends Console.CommandHandler, ZUI.QueryHandler, ZUI.MessageHandler,
			Clock.AlarmHandler
{
};
