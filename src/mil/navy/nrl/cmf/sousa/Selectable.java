package mil.navy.nrl.cmf.sousa;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * An object that can originate signals to one or more
 * SelectableSets to which they belong.
 */
public abstract class Selectable
{
	protected static final Logger _LOG = Logger.getLogger(Selectable.class);

    //// nested classes 

	/**
	   Each Selectable in a {@link SelectableSet} has a
	   <CODE>Selectable.Handler</CODE> for a {@link SignalType}.
	 */
	public interface Handler {

		/**
		   Performs the type of I/O on <CODE>sel</CODE> appropriate to <CODE>st</CODE>.

		   @param sel the <CODE>Selectable</CODE>
		   @param st the type of I/O to perform on the <CODE>Selectable</CODE>
		*/
		void handle(Selectable sel, SignalType st);
    }

    //// data members

	/**
	   Maintains Lists of {@link Selectable.Handler} according to
	   {@link SignalType}.  The keys are <CODE>SignalType</CODE>.  The
	   values are <CODE>LinkedList</CODE>.
	*/
    private final HashMap _selectableSets = new HashMap(); 

	/**
	   The default maximum size of {@link #_inQ} and {@link #_outQ}.
	 */
    protected static final int DEFAULT_QUEUE_CAPACITY = 2;

    /**
	   List of objects that are ready to be read from the channel
	*/
    protected final LinkedList _inQ = new LinkedList();

	/**
	   The actual capacity of {@link #_inQ}.
	 */
    protected final int _inQCapacity;

    /** 
		List of objects waiting to be written to the channel
	*/
    protected final LinkedList _outQ = new LinkedList();

	/**
	   The actual capacity of {@link #_outQ}.
	*/
    protected final int _outQCapacity;

    /**
	   Error status of the Channel
	*/
    private Exception _error = null;

    //// package methods

    /**
	  Gets an Object from {@link #_inQ} since it has been completely
      read from the channel and reconstituted.

	  @return a reconstituted object.
	*/
    public Object read() {
		Object ser = null;
		synchronized(_inQ) {
			if (dataRead() > 0) {
				boolean wakeup = false;
				ser = (Object)_inQ.removeFirst();
			}
			else {
				// Shouldn't happen!
				_LOG.fatal("Input queue is empty");
				Thread.dumpStack();
				System.exit(1);
			}
		}
		return ser;
    }

    /**
	   Puts <CODE>ser</CODE> into {@link #_outQ} in preparation for writing on the
	   channel.

	   @param ser the non-<CODE>null</CODE> <CODE>Object</CODE> to put into <CODE>_outQ</CODE>.
	*/
    public void write(Object ser) {
		synchronized(_outQ) {
			if (spaceWrite() > 0) {
				_outQ.addLast(ser);
			} else {
				// Shouldn't happen!
				_LOG.fatal("Output queue is full");
				Thread.dumpStack();
				System.exit(1);
			}
		}
    }

    /**
	   Registers this <CODE>Selectable</CODE> in <CODE>ss</CODE> as an
	   <CODE>st</CODE> signal recipient.

	   @param ss the <CODE>SelectableSet</CODE>
	   @param st the type of I/O desired
     */
    void addSelectableSet(SelectableSet ss, SignalType st)
    {
		LinkedList list = _getList(st);
		synchronized (list) {
			if (!list.contains(ss)) {
				list.addLast(ss);
			}
		}

		if ((st == SignalType.READ) && (this.dataRead() > 0)) {
			ss.recvNotification(this, SignalType.READ, false);
		}
	
		if ((st == SignalType.WRITE) && (this.spaceWrite() > 0)) {
			ss.recvNotification(this, SignalType.WRITE, false);
		}
	
		if ((st == SignalType.ERROR) && (null != this.getError())) {
			ss.recvNotification(this, SignalType.ERROR, false);
		}
    }

    /**
	   Deregister this <CODE>Selectable</CODE> from <CODE>ss</CODE> as
	   an <CODE>st</CODE> signal recipient.

	   @param ss the <CODE>SelectableSet</CODE>
	   @param st the type of I/O desired
     */
    void removeSelectableSet(SelectableSet ss, SignalType st)
    {
		LinkedList list = _getList(st);
		synchronized (list) {
			list.remove(ss);
		}
    }

    /*
	  Sets the error state of this <CODE>Selectable</CODE>.

	  @param e the error
     */
    synchronized void setError(Exception e) {
	_error = e;

	if (_LOG.isEnabledFor(Level.DEBUG)) {
		_LOG.debug(this.getClass().getName()+"@"+this.hashCode()+".setError() called with exception:");
		e.printStackTrace();
	}
	broadcastNotification(SignalType.ERROR);
    }

    /**
	   Gets the error state.

	   @return the error
     */
    public synchronized Exception getError() 
    {
	Exception answer = null;
	answer = _error;
	return answer;
    }

    //// protected methods

    /**
	   Class constructor for a Selectable with the default capacities
	   in both its read queue and its write queue.

	   @see #DEFAULT_QUEUE_CAPACITY
     */
    protected Selectable() 
    {
		_outQCapacity = Selectable.DEFAULT_QUEUE_CAPACITY;
		_inQCapacity = Selectable.DEFAULT_QUEUE_CAPACITY;
    }

    /**
	   Returns the number of items in the read queue.

	   @return the number of items in the read queue
	*/
    protected int dataRead()
    {
		int answer = 0;
		synchronized (_inQ) {
			answer = _inQ.size();
		}
		return answer;
    }

    /**
	   Returns the number of empty slots in the read queue.

	   @return the number of empty slots in the read queue
     */
    protected int spaceRead()
    {
		int answer = 0;
		synchronized (_inQ) {
			answer = _inQCapacity - _inQ.size();
		}
		return answer;
    }

    /**
	   Returns the number of items in the write queue.

	   @return the number of items in the write queue
     */
    protected int dataWrite()
    {
		int answer = 0;
		synchronized (_outQ) {
			answer = _outQ.size();
		}
		return answer;
    }

    /**
	   Returns the number of empty slots in the write queue.

	   @return the number of empty slots in the write queue
     */
    protected int spaceWrite()
    {
		int answer = 0;
		synchronized (_outQ) {
			answer = _outQCapacity - _outQ.size();
		}
		return answer;
    }

    /**
	   Puts <CODE>ser</CODE> into the read queue, delivering it to the higher
	   layer.

	   @param ser the object for the higher layer
     */
    protected final void put_inQ(Object ser) {
		synchronized(_inQ) {
			// put the object in the _inQ
			_inQ.addLast(ser);
			if (dataRead() > 0) {
				broadcastNotification(SignalType.READ);
			}
		}
    }

    /**
	   Takes a message from the outQ for processing by the
	   lower layer.

	   @return the object for the lower layer
     */
    protected final Object take_outQ() {
		synchronized(_outQ) {
			Object ser = (Object)_outQ.removeFirst();
			if (spaceWrite() > 0) {
				broadcastNotification(SignalType.WRITE);
			}
			return ser;
		}
    }

    /**
	   Send the <CODE>st</CODE> signal to every {@link SelectableSet} that is 
	   registered as an <CODE>st</CODE> recipient.

	   @param st the type of I/O
     */
    protected void broadcastNotification(SignalType st)
    {
		LinkedList list = _getList(st);
		LinkedList listcopy;
		synchronized (list) {
			listcopy = (LinkedList)list.clone();
		}

		for (Iterator it = listcopy.iterator(); it.hasNext();) {
			SelectableSet ss = (SelectableSet)it.next();
			ss.recvNotification(this, st, true);
		}
    }


    /**
	   Tells the state of this Selectable.

	   return <CODE>true</CODE> if this Selectable is closed;
	   <CODE>false</CODE> otherwise.
	*/
    abstract boolean isClosed();


    /**
	   Closes this Selectable.

	   @throws IOException if the closing operation fails
	*/
    abstract void close() throws IOException;

    //// private methods 

	/**
	   Determines the <CODE>LinkedList</CODE> of {@link Selectable.Handler}
	   that handle I/O operation <CODE>st</CODE>.  Creates the
	   <CODE>List</CODE> if one does not exist.

	   @param st the I/O operation
	   @return the <CODE>LinkedList</CODE> of
	   <CODE>Selectable.Handler</CODE> for <CODE>st</CODE>
	 */
    private LinkedList _getList(SignalType st)
    {
		LinkedList list = null;
		synchronized(_selectableSets) {
			list = (LinkedList)_selectableSets.get(st);
			if (list == null) {
				list = new LinkedList();
				_selectableSets.put(st, list);
			}
		}
		return list;
    }
};
