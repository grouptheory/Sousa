package mil.navy.nrl.cmf.sousa;

import java.io.IOException;
import mil.navy.nrl.cmf.norm4j.NormEvent;
import mil.navy.nrl.cmf.norm4j.NormIOException;
import mil.navy.nrl.cmf.norm4j.NormInstance;
import mil.navy.nrl.cmf.norm4j.NormInsufficientResourcesException;
import mil.navy.nrl.cmf.norm4j.NormObject;
import mil.navy.nrl.cmf.norm4j.NormObjectData;
import mil.navy.nrl.cmf.norm4j.NormSession;
import mil.navy.nrl.cmf.sousa.util.Strings;
import org.apache.log4j.Logger;

/**
   <CODE>RunnableNormInstance</CODE> is a <CODE>NormInstance</CODE>
   that is also <CODE>Runnable</CODE>.
 */
public class RunnableNormInstance extends NormInstance 
    implements Runnable 
{
    private static final Logger _LOG = 
		Logger.getLogger(RunnableNormInstance.class);
    
    
    /**
	   A mutex that protects {@link #_running}.
    */
    private final Object _runningMonitor = new Object();

	/**
	   <CODE>_running</CODE> is set to false in destroyInstance().
	   <CODE>_running</CODE> is examined by {@link #run()} after each 
	   <CODE>NormEvent</CODE>.
	   <P>
	   Since <CODE>Thread.stop()</CODE> is deprecated, we use 
	   <CODE>_running</CODE> and
	   <CODE>_runningMonitor</CODE> to tell <CODE>run</CODE> when to exit.
	 */
    private boolean _running = true;
    
    /**
	   The RunnableNormInstance needs its own Thread.
	*/
    private final Thread _normThread = new Thread(this, "Entity-NORM");


    /**
	   Class constructor that uses a ByteArray as its Serializer

	   @param priorityBoost <CODE>true</CODE> to raise the
	   NormInstance's priority; <CODE>false</CODE> otherwise
	*/
    public RunnableNormInstance(boolean priorityBoost) 
		throws NormInsufficientResourcesException
    {
		super(priorityBoost);

		// start the thread
		_normThread.start();
    }

    /**
	   Adds the Object contained in <CODE>n</CODE> to the input queue
	   for a higher layer to read.

	   @param n an event of type NormEvent.NORM_RX_OBJECT_COMPLETED
	 */
    public void reportReceiverObject(NormEvent n) 
    {
		NormObject nobject = null;
		NormSession session = null;
	
		// DAVID: If the owner of the SelectableNormReceiverSession is
		// dilligent about reading, then the
		// SelectableNormReceiverSession's message queue probably won't
		// grow without bound.  A NormSenderSession has a fixed length
		// queue, after all.
		nobject = n.getObject();
		try {
	    
			// DAVID: For now, at least, only NormObjectData is important.
			if(nobject instanceof NormObjectData){
				byte[] data = ((NormObjectData)nobject).detachData();
				session = n.getSession();
				Object userData = session.getUserData();

				// Construct an Object from the received objects
				// byte array and give it to the receiver session.

				if (userData instanceof SelectableNormSession) {
					_LOG.info(new Strings(new Object[]
						{"Received ", new Integer(data.length), " bytes"}));

					Object m = 
						((SelectableNormSession)userData)._serializer.toObject(data);

					_LOG.debug(new Strings(new Object[] { 
							   "Thread ", Thread.currentThread().getName(), 
							   " received ", m
							   } ));
					((SelectableNormSession)userData).addToInQ(m);
				} else {
					_LOG.error(new Strings(new Object[] {
						"Expected SelectableNormSession in user data.  Got ",
						userData, " instead."
					} ));
				}
			} else {
				_LOG.error(new Strings(new Object[] {
					this, ":",
					"Data object expected. Received ",
					nobject.getClass().getName()
				} ));
			}					
		} catch(ClassNotFoundException ex) {
			_LOG.error(new Strings(new Object[] {
				this, ":",
				"Class not found: ",
				nobject.getClass().getName()
			} ));
		} catch(IOException ex) {
			_LOG.error(new Strings(new Object[] {
				this, ":", ex
			} ));
		}
    }
    
    /**
	   Tells {@link #run()} to exit then cleans up the
	   <CODE>NormInstance</CODE>.
	*/
    public void destroyInstance() 
    {
		synchronized (_runningMonitor) {
			_running = false;
		}
	
		super.destroyInstance();
    }
    
    // java.lang.runnable
    
    /**
	   <CODE>run</CODE> is an event loop.  It keeps track of the
	   available writing slots in the NormSession.  It reports new
	   Objects as they arrive from the sender.  <CODE>run</CODE> loops
	   continuously until {@link #_running} is false.
	*/
    public void run() 
    {
		int eventType = NormEvent.NORM_EVENT_INVALID;
		NormEvent n = null;
		NormSession session = null;
		Object userData = null;
		boolean running;
		NormObject normObj = null;

		synchronized (_runningMonitor) {
			running = _running;
		}
	
		while (running) {
			try {
				while ((n = getNextEvent()) != null) {
					eventType = n.getType();	

					_LOG.debug(n);
								
					switch (eventType) {
			
					case NormEvent.NORM_TX_QUEUE_VACANCY:
						// Tell the NormSession that it is writable
						session = n.getSession();
						userData = session.getUserData();
			
						if (userData instanceof SelectableNormSession)
							((SelectableNormSession)userData).incrementWritable();
						else {
							_LOG.error(new Strings(new Object[] {
								"Expected SelectableNormSession in user data.  Got ",
								userData, " instead."
							} ));
						}
						break;
			
					case NormEvent.NORM_TX_QUEUE_EMPTY:
						// Tell the NormSession that it is writable
			
						// DAVID: What's the relationship between
						// NORM_TX_QUEUE_EMPTY and NORM_TX_QUEUE_VACANCY?
						// Does NORM send NORM_TX_QUEUE_VACANCY after the
						// last unacked messages is acked?  If so, does it
						// also send NORM_TX_QUEUE_EMPTY? My guess is that
						// it does not; that's what NORM_TX_QUEUE_EMPTY is
						// for.
			
						session = n.getSession();
						userData = session.getUserData();
			
						if (userData instanceof SelectableNormSession) {
							((SelectableNormSession)userData).incrementWritable();
						} else {
							_LOG.error(new Strings(new Object[] {
								"Expected SelectableNormSession in user data.  Got ",
								userData, " instead."
							} ));
						}
						break;
			
					case NormEvent.NORM_TX_OBJECT_SENT:
						_LOG.info(new Strings(new Object[] 
							{n, " ", new Long(System.currentTimeMillis())}));
						break;
			
					case NormEvent.NORM_TX_OBJECT_PURGED:
						// This isn't an error for the NormSenderSession.
						// NORM_TX_OBJECT_PURGED indicates one of two
						// things.  It can indicate that all receivers
						// have successfully received the NormObject.  It
						// can indicate that the NormSenderSession has
						// tried to send too many NormObjects, causing the
						// NORM protocol engine to discard the oldest
						// NormObject in the transmit queue.
			
						// The latter won't happen as long as this
						// RunnableNormInstance is used only with
						// SelectableNormReceiverSessions and
						// SelectableNormSenderSessions.
						_LOG.info(new Strings(new Object[] 
							{n, " ", new Integer(((NormObjectData)n.getObject()).accessData().length)}));
						break;
			
						//case NormEvent.NORM_LOCAL_SENDER_CLOSED:
			
					case NormEvent.NORM_REMOTE_SENDER_INACTIVE:
						// TODO: Signal the NormSession that the remote
						// end has gone away.  Treat this as a close().
						// Any further reading or writing on the
						// NormSession must raise an IOException.
						_LOG.debug(n);
			
						// Tell the NormSession that it is in error
						session = n.getSession();
						userData = session.getUserData();
			
						if (userData instanceof SelectableNormSession)
							((SelectableNormSession)userData).setError(new IOException(n.toString()));
						else {
							_LOG.error(new Strings(new Object[] {
								"Expected SelectableNormSession in user data.  Got ",
								userData, " instead."
							} ));
						}
						break;
			
			
					case NormEvent.NORM_RX_OBJECT_NEW:
						// Start of per-object rx timing
						normObj = n.getObject();
						_LOG.info(new Strings(new Object[]
							{n, " ", new Long(System.currentTimeMillis())}));
						break;
			
					case NormEvent.NORM_RX_OBJECT_COMPLETED:
						// End of per-object rx timing
						// Give the NormSession the contents of the NormEvent
						normObj = n.getObject();
						_LOG.info(new Strings(new Object[]
							{n, " ", new Long(System.currentTimeMillis())}));
						reportReceiverObject(n);
						break;
			
					default:
						break;
					}
				}
			} catch (NormIOException ex) {
				_LOG.error(new Strings(new Object[] {
					this, ":", ex
				} ));
			}
	    
			synchronized(_runningMonitor) {
				running = _running;
			}
		}
    }
}
