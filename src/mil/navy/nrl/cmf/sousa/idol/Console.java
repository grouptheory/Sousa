package mil.navy.nrl.cmf.sousa.idol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;
import mil.navy.nrl.cmf.sousa.util.StackTrace;
import mil.navy.nrl.cmf.sousa.util.Strings;
import org.apache.log4j.Logger;

/**
   <code>Console</code> reads lines from <code>System.in</code> and
   delegates their interpretation to a <code>CommandHandler</code>.
 */
public final class Console
extends Thread
{
    private static final Logger _LOG = 
		Logger.getLogger(Console.class);

    /**
	   The command interpreter
    */
    /*@ non_null */ private final CommandHandler _handler;

	/**
	   Indicates to whether the thread should continue running or not.
	   The value <code>true</code> means "continue running".  The
	   value <code>false</code> means "stop".

	   @see #isRunning()
	   @see #stopRunning()
	*/
    private boolean _running = true;

	// Constructors

	/**
	   Class constructor that takes a <code>Properties</code> and a
	   <code>CommandHandler<code>.
	   <P>
	   This implementation ignores the <code>Properties</code>.

	   @param p the <code>Properties</code> of this <code>Console</code>
	   @param ch the <code>CommandHandler</code> for this <code>Console</code>
	*/
	Console(/*@ non_null */ Properties p, /*@ non_null */ CommandHandler ch)
	{
		super("Console");

		this._handler = ch;
	}

	/**
	   Class constructor that takes onle a
	   <code>CommandHandler<code>.
	   <P>
	   @param ch the <code>CommandHandler</code> for this <code>Console</code>
	*/
        public Console(/*@ non_null */ CommandHandler ch)
	{
		super("Console");
		this._handler = ch;
	}

	// java.lang.Thread

	/**
	   Reads lines from <code>System.in</code> and delgates their
	   interpretation to the <code>CommandHandler</code>.  Exits if
	   isRunning returns false.

	   @see java.lang.Thread#run()
	   @see #isRunning()
	*/
	public synchronized final void
		run()
	{
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		String line = null;

		try {
			while (this.isRunning() && (null != (line = reader.readLine()))) {
				try {
					_handler.handleCommand(line);
				} catch (RuntimeException ex) {
					_LOG.error(new Strings(new Object[]
						{this, ":", StackTrace.formatStackTrace(ex)}));
				}
			}
		} catch (IOException ex) {
			_LOG.error(new Strings(new Object[]
				{this, ":", StackTrace.formatStackTrace(ex)}));
		}

		if (line == null) {
			if (this.isRunning()) {
				_handler.handleCommand("quit");
			}
		}

		_LOG.warn("Console exiting.");
	}


	/**
	   Tells this <code>Console</code> to exit.
	 */
        public final void stopRunning() {
		_running = false;
	}

	/**
	   Tells this <code>Console</code> if it should continue running.

	   @return <code>true<code> means "continue running"; <code>false</code> means "stop running".
	 */
	protected final boolean isRunning() {
		return _running;
	}

	// ****************************************************************
	// ****************************************************************

	/**
	   CommandHandler interprets command strings
	*/
	public interface
		CommandHandler
	{
		/**
		   Interprets a command string

		   @param command the command string
		*/
		public void
			handleCommand(/*@ non_null */ String command);
	}; // CommandHandler
}; // Console
