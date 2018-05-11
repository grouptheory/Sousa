package mil.navy.nrl.cmf.sousa;

import java.io.IOException;

import mil.navy.nrl.cmf.norm4j.NormInstance;
import mil.navy.nrl.cmf.norm4j.NormNodeId;
import mil.navy.nrl.cmf.norm4j.NormObject;
import mil.navy.nrl.cmf.norm4j.NormSession;
import mil.navy.nrl.cmf.sousa.util.StackTrace;
import mil.navy.nrl.cmf.sousa.util.Strings;
import org.apache.log4j.Logger;

/**
   <CODE>SelectableNormSession</CODE> is a <CODE>Selectable</CODE>
   NAK-Oriented Reliable Multicast transport.

   @see mil.navy.nrl.cmf.norm4j.NormSession
 */
public class SelectableNormSession extends Selectable
{
    private static final Logger _LOG = 
		Logger.getLogger(SelectableNormSession.class);
	
	/**   
	   Size of the I/O buffer.  Value for getSession().StartSender().
	   See p. 24 of "NORM Developer's Guide".  

	   <P>

	   <EM>TODO: This should go in NormSession instead of
	   SelectableNormSession.</EM>
	*/
	public static final long BUFF_SIZE = 1024 * 4096;

	/**   
	   Size of an I/O segment.  Value for getSession().StartSender().
	   See p. 24 of "NORM Developer's Guide".  

	   <P>

	   <EM>TODO: This should go in NormSession instead of
	   SelectableNormSession.</EM>
	*/
	public static final int SEGMENT_SIZE = 1400;

	/**   
	   Size of an I/O segment.  Value for getSession().StartSender().
	   See p. 24 of "NORM Developer's Guide".  

	   <P>

	   <EM>TODO: This should go in NormSession instead of
	   SelectableNormSession.</EM>
	*/
	public static final short BLOCK_SIZE = 60;

	/**   
	   Number of parity bits.  Value for getSession().StartSender().
	   See p. 24 of "NORM Developer's Guide".  

	   <P>

	   <EM>TODO: This should go in NormSession instead of
	   SelectableNormSession.</EM>
	*/
	public static final short NUM_PARITY = 0;

	/**
	   The reliable multicast transport.
	 */
    private NormSession _session;

	/**
	   Multicast address of the NormSession.
	*/
	private final String _address;

	/**
	   Multicast port of the NormSession.
	*/
	private final int _port;

    
	/**
	   For turning Objects into arrays of bytes.
	 */
	final Serializer _serializer;

    // These pertain to writing.

	/**
	  The mutex that protects _slotsForWriting
	*/
	private Object _writingMutex = new Object();

	/**
	   Number of slots that {@link #_session} has available for
	   writing new objects.  The initial value is 8.  This magic
	   number comes from "NORM Developer's Guide", p. 29.  It's the
	   default value of the countMin parameter of
	   NormSetTransmitCacheBounds().  The default value of the
	   countMax parameter of NormSetTransmitCacheBounds() is 256.
	*/
	private int _slotsForWriting = 8;

	/**
	   Class Constructor that supplies a default Serializer,
	   ByteArray.

	   @param norm the RunnableNormInstance in which to create a NormSession
	   @param address the multicast address of the sender
	   @param port the multicast port of the sender
	   @param localID the local identifier of the NormSession
	*/
    public SelectableNormSession(RunnableNormInstance norm, 
								 String address, 
								 int port, 
								 NormNodeId localID) 
    {
		super();
		_session = new NormSession((NormInstance)norm, address, port, localID);
		_session.setUserData(this);
		_address = new String(address);
		_port = port;
		_serializer = new ByteArray();
    }

	/**
	   Class Constructor that uses the Serializer supplied by the
	   caller

	   @param norm the RunnableNormInstance in which to create a NormSession
	   @param address the multicast address of the sender
	   @param port the multicast port of the sender
	   @param localID the local identifier of the NormSession
	   @param s the serializing object
	*/
    public SelectableNormSession(RunnableNormInstance norm, 
								 String address, 
								 int port, 
								 NormNodeId localID,
								 Serializer s) 
    {
		super();
		_session = new NormSession((NormInstance)norm, address, port, localID);
		_session.setUserData(this);
		_address = new String(address);
		_port = port;
		_serializer = s;
    }

	/**
	   Returns the NormSession.

	   @return the NormSession
	*/
    NormSession getSession() 
    {
		return _session;
    }

    /**
	   Returns the multicast address.

	   @return the address
	*/
    public String getAddress() {
		return _address;
	}

	/**
	   Returns the multicast port.

	   @return the port
	*/
	public int getPort() {
		return _port;
	}

    //
    // Read-related methods
    //
    
    // Called in the RunnableNormInstance's thread
	/**
	   Appends <CODE>m</CODE> to the input queue

	   @param m an object now available for reading by a higher layer
	 */
    public void addToInQ(Object m) 
    {
		put_inQ(m);
    }
    
    // Called in the main thread
	/**
	   Permit or deny NORM the ability to reuse the Rx port of this NormSession.
	   @param enable true to permit port reuse; false to deny port reuse.
	   @param bindToSessionAddress binds to receiver socket when true
	 */
    public void setRxPortReuse(boolean enable, boolean bindToSessionAddress) 
    {
		_session.setRxPortReuse(enable, bindToSessionAddress);
    }
    
    //
    // Write-related methods
    //
    
	/**
	   Enqueues <CODE>ser</CODE> in the output queue then writes
	   <CODE>MIN(_outQ.size(), _slotsForWriting)</CODE> Objects into
	   the NormSession.  Writing occurs in the caller's thread.

	   @param ser the new object for the output queue
	*/
	public void write(Object ser) {
		byte[] data;
		Object m = null;

		super.write(ser);		// adds ser to _outQ

		// DAVID: Is it better to use _outQ to protect
		// _slotsForWriting?
		synchronized (_writingMutex) {
			while ((_slotsForWriting > 0) && (dataWrite() > 0)) {
				try {
					m = take_outQ();
					data = _serializer.toByteArray(m);
					if (null != data) {
						_LOG.info(new Strings(new Object[]
							{this, ": Writing ", new Integer(data.length), 
							 " bytes into ", _session, " ", 
							 new Long(System.currentTimeMillis())}));

						NormObject result = _session.sendData(data);

						if (null == result) {
							// This should never happen.
							//
							// BUG: When NormSession.sendData()
							// returns null, we drop the object on the
							// floor.  There is no way to put it back
							// in the output queue.
							_LOG.error("_session.sendData() returned null!");
						}

						_slotsForWriting --;

					} else { // shouldn't happen!
						_LOG.error("Serializer.toByteArray() returned null!");
					}
					_LOG.debug(new Strings(new Object[] 
						{this, ": wrote ", m}));
				} catch (IOException ex) {
					_LOG.fatal(new Strings(new Object[] 
						{this, ":", StackTrace.formatStackTrace(ex)}));
					System.exit(1);
				}
			}
		}
	}

	/**
	   Notes that there is now one more slot available for writing.
	 */
    public void incrementWritable() 
    {
		synchronized (_writingMutex) {
			_slotsForWriting ++;
		}
	
    }
    

    //
    // Methods for gracefully ending the NormSession
    //
    
    public void close()
    {
		if (null != _session) {
			_session.stopSender(true);
			_session.stopReceiver(1000);
			_session.destroySession();
			_session = null;
		}
    }

    boolean isClosed() {
		return (_session == null);
    }
}
