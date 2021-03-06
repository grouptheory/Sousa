package mil.navy.nrl.cmf.sousa.idol.user;

import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import mil.navy.nrl.cmf.sousa.Receptor;
import mil.navy.nrl.cmf.sousa.ServerContact;
import mil.navy.nrl.cmf.sousa.util.Strings;
import org.apache.log4j.Logger;

/**
   <CODE>CommandQueue</CODE> is a queue of <CODE>Command</CODE>s.  It
   is thread safe.  It executes the <CODE>Command</CODE>s according to
   their type.

 */
public class CommandQueue implements Runnable {

	private static final Logger _LOG = Logger.getLogger(CommandQueue.class);

	private final LinkedList _inQ = new LinkedList();
	private final List _locals;
	private final RemoteCommandObject _remote;

    // Don't modify routes after you call _commandsThread.start().  At
    // best, the additions will be ignored.  At worst, there will be a
    // ConcurrentModificationException.
	public CommandQueue(List localCommandObjects, 
			    RemoteCommandObject remote) {
	    _locals = localCommandObjects;
	    _remote = remote;
	}

    /**
	   Puts a Command into the queue.

	   @param ser the Command for execution later
     */
    public final synchronized void put(Command ser) {
		_inQ.addLast(ser);
		notify();
    }

    /**
	   Remove and return the first Command from the queue.

	   @return the first Command in the queue.
     */
    private final synchronized Command get() {
		while (0 == _inQ.size()) {
			try {
				wait();
			} catch (InterruptedException ex) {
			}
		}

		return (Command)_inQ.removeFirst();
    }

    public void run() {


	while (true) {
	    Command c = get();
	    if (null != c) {

		// So far, only LocalCommand is a RoutableCommand.
		if (LocalCommand.class.isAssignableFrom(c.getClass())) {

		    // Route c to all
		    // LocalCommandObjects but the one
		    // that sent it.
		    for(Iterator i = _locals.iterator(); i.hasNext(); ) {
			LocalCommandObject local = (LocalCommandObject)i.next();
			if (local != ((AbstractLocalCommand)c).getSource()) {
				_LOG.debug(new Strings(new Object[]
					{"Routing ", c, " to ", local}));
			    ((AbstractLocalCommand)c).execute(local);
			} else {
				_LOG.debug(new Strings(new Object[]
					{"Not routing ", c, " to its source, ", local}));
			}
		    }
		} else if (RemoteCommand.class.isAssignableFrom(c.getClass())) {
		    Receptor r = _remote.getReceptor(((RemoteCommand)c).getServerContact());

		    if (null != r) {
			((RemoteCommand)c).execute(_remote);
		    } else {
			_LOG.error(new Strings(new Object[]
			    {"No Receptor for ", 
			     ((RemoteCommand)c).getServerContact()}));
		    }
		} else {
		    _LOG.error(new Strings(new Object[]
			{c, " is neither LocalCommand nor RemoteCommand."}));
											   
		}
	    }
	}
    }
}
