package mil.navy.nrl.cmf.sousa;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import mil.navy.nrl.cmf.sousa.util.Strings;
import org.apache.log4j.Logger;

/**
 * A P2PChannel is a full-duplex reliable point to point
 * Channel.
 */
class P2PChannel extends NonBlockingObjectChannel {
	private static final Logger _LOG = 
		Logger.getLogger(P2PChannel.class);

    // the factory that made this p2pchannel
    private final P2PChannelFactory _factory;
    // the SocketChannel
    private final SocketChannel _sc;
    // SelectionKeys 
    private SelectionKey _skread;
    private SelectionKey _skwrite;
    
    // length prefix (integer) size in bytes
    private static final int INT_SIZE = 4;
    
    // Indirect byte buffer for reading from the SocketChannel
    private  ByteBuffer _readbuf = null; // allocated by NIO_read()
    private int _readBytesExpected;
    private int _readBytesReceived;
    private boolean _readPrefix;

	//
	// For gathering timing info.  Turn on log4j INFO level to see.
	//
	// time of reading first byte
    private long _readStartTime = 0;

	// time in milliseconds to deserialize an object after reading
	private long _deserializeTime = -1; 

	// time of writing first byte
	private long _writeStartTime = 0;

	// time in milliseconds to deserialize an object after reading
	private long _serializeTime = -1;

    // Indirect byte buffer for writing to the SocketChannel
    private ByteBuffer _writebuf = null; // allocated by serialize()
    private final ByteArrayOutputStream _writebytestream = new ByteArrayOutputStream();
    private int _writeBytesExpected;
    private int _writeBytesSent;

	// the remote server, or null if the remote endpoint is our client
	private final ServerContact _addr;

    /* 
     * PURPOSE: Make a P2PChannel over a SocketChannel
     * PRECONDITION: SocketChannel is an active connected Channel.
     * POSTCONDITION: A new P2PChannel is made, which buffers incoming
     * data and reconstitutes objects, enqueues outgoing objects and
     * sends them as buffers.
     */
    P2PChannel(SocketChannel sc, ServerContact addr, P2PChannelFactory fac) {
		_sc = sc;
		_addr = addr;
		_factory = fac;
		readbuf_reset();
		writebuf_reset();
    }

    /* 
     * PURPOSE: Make a P2PChannel over a SocketChannel
     */
    public String toString() {
		String s = "";
		if (_addr != null) {
			s = "P2PChannel to server "+_addr+"";
		}
		else {
			s = "P2PChannel from client";
		}
		return s;
	}

    /* 
     * PURPOSE: Returns the server contact address if this is a
     * P2PChannel to a server, and null if this is a P2PChannel to a
     * client.
	 */
	ServerContact getServerContact() {
		return _addr;
	}

    // is the P2PChannel closed?
    boolean isClosed() {
	return (!_sc.isOpen());
    }

    // close this P2PChannel
    void close() throws IOException {
		_sc.close();
		deregister();
    }

    // sousa.P2PChannel
    
    /* 
     * PURPOSE: Register this P2PChannel with a Selector.
     * PRECONDITION: selector!=null.
     * POSTCONDITION: This P2PChannel is registered with the
     * specified selector and will be notified on READ & WRITE events.
     */
    public void register(Selector selector) {
		// save the Selector
		super.register(selector);

		//if (! _sc.isConnected()) {
		//System.out.println("********* Registering a P2PChannel that is already dead!");		
		//}

		//System.out.println("P2PChannel register: _readBytesExpected="+_readBytesExpected);
		//System.out.println("P2PChannel register: spaceRead()="+spaceRead());

		// DAVID: It seems to me that both calls to _sc.register()
		// should go in the try block.  Bilal had some question about
		// the threading so we agreed to leave the code as is for now.
		if (_skread == null) {
		    try {
				_skread = _sc.register(selector, NonBlockingObjectChannel.OP_NONE, this);
		    }
		    catch (ClosedChannelException ex) {
				// DAVID: Should this report the exception?
				_LOG.error(ex);
				setError(ex);
				deregister();
				return;
		    }
		} 

		if (null != _skread) {
			synchronized(_skread) {
				// Recording _sc to selector (type OP_READ)
				if ((_readBytesReceived < _readBytesExpected) && (spaceRead() > 0)) {
					// save the key
					_skread.interestOps(SelectionKey.OP_READ);
				}
			}
		}

		if (_skwrite == null) {
			try {
				_skwrite = _sc.register(selector, NonBlockingObjectChannel.OP_NONE, this);
				selector.wakeup();
			}
			catch (ClosedChannelException ex) {
				_LOG.error(ex);
				setError(ex);
				deregister();
				return;
			}
		}

		if (null != _skwrite) {
			synchronized(_skwrite) {
				//System.out.println("P2PChannel register: _writeBytesExpected="+_writeBytesExpected);
				//System.out.println("P2PChannel register: dataWrite()="+dataWrite());

				// Recording _sc to selector (type OP_WRITE)
				if ((_writeBytesSent < _writeBytesExpected) || (dataWrite() > 0)) {
					_skwrite.interestOps(SelectionKey.OP_WRITE);
					// System.out.println("P2PChannel registered for write");
				}
			}
		}
    }
    
    /* 
     * PURPOSE: Deregister this P2PChannel with a Selector.
     * PRECONDITION: This P2PChannel is previously registered with the
     * specified selector.
     * POSTCONDITION: This P2PChannel is no longer registered with the
     * Selector.
     */
    void deregister() {
		if (_skread!=null) {
			synchronized (_skread) {
				_skread.cancel();
				_skread = null;
				// System.out.println("XXX Nuked _skread");
			}
		}
	
		if (_skwrite!=null) {
			synchronized (_skwrite) {
				_skwrite.cancel();
				_skwrite = null;
			}
		}
		_factory.deregisterP2PChannel(this);
    }
    
    /* 
     * PURPOSE: Give the P2PChannel time to handle the READ & WRITE events.
     * PRECONDITION: key.associated-data==this
     * POSTCONDITION: Incoming data can be read or written.
     */
    public void handle(SelectionKey key) {

		// System.out.println("XXX P2P is asked to handle, writable="+key.isWritable()+", readable="+key.isReadable()+" valid="+key.isValid());

		if ( ! key.isValid()) return;
		// is the channel readable?
		if (key.isReadable()) NIO_read();
	
		if ( ! key.isValid()) return;
		// is the channel writable?
		if (key.isWritable()) NIO_write();
    }
    
    /* 
     * PURPOSE: Give the P2PChannel time to handle the WRITEs.
     * PRECONDITION: Called by handle().
     * POSTCONDITION: Data for the Object being currently transmitted
     * is written out, and if that is completed, then another enqueued
     * Object is serialized in preparation for writing.
     */
    private void NIO_write() {
		if (null != getError()) return;
		//System.out.println("NIO write");
	
		if (_writeBytesSent == _writeBytesExpected) {
			Object ser = take_outQ();
			writebuf_reset();
			serialize(ser);
			_writeStartTime = System.currentTimeMillis();
		}
	
		try {
			int numBytesWritten = _sc.write(_writebuf);
			_writeBytesSent += numBytesWritten;

			if (_writeBytesSent == _writeBytesExpected) {
				long writeTime = System.currentTimeMillis() - _writeStartTime;
				_LOG.info(new Strings(new Object[]
					{"Time writing ", new Long(writeTime), " ms serializing ",
					 new Long(_serializeTime), " ms ", 
					 new Integer(_writeBytesExpected), " bytes" }));
			}
		}
		catch (IOException e) {
			_LOG.error(e);
			// Connection may have been closed
			setError(e);
			deregister();
		}
    }
    
    /* 
     * PURPOSE: Give the P2PChannel time to handle the READs.
     * PRECONDITION: Called by handle().
     * POSTCONDITION: Data is read and processed into Objects if possible.
     */
    private void NIO_read() {
		if (null != getError()) return;
		try {
			if ((_readPrefix) && (_readBytesReceived == 0)) 
				_readStartTime = System.currentTimeMillis();

			int numBytesRead = _sc.read(_readbuf);

			if (numBytesRead > 0) {
				_readBytesReceived += numBytesRead;
		
				// reading the prefix?
				if (_readPrefix) {
					// we can read the header count
					if (_readBytesReceived == _readBytesExpected) {
						// figure out how many bytes in the next object:
						// It's the length of the buffer, returned by
						// _readbuf.getInt(0) minus the size of an int.
						int prefix = _readbuf.getInt(0);
						_readBytesExpected = prefix - INT_SIZE;
						_readBytesReceived -= INT_SIZE;
						_readbuf = ByteBuffer.allocate(_readBytesExpected);
						_readbuf.limit(_readBytesExpected);
						_readPrefix = false;
					}
				}
				// or reading the object?
				else {
					// has the object arrived in its entirety?
					if (_readBytesReceived == _readBytesExpected) {
						long readTime = System.currentTimeMillis() - _readStartTime;
						Object ser = deserialize();
						put_inQ(ser);
						readbuf_reset();

						_LOG.info(new Strings(new Object[]
							{"Time reading ", new Long(readTime), " ms deserializing ", 
							 new Long(_deserializeTime), " ms ",
							 new Integer(_readBytesExpected), " bytes"}));

					} else if (_readBytesReceived > _readBytesExpected) {
						_LOG.error("_readBytesReceived=" + _readBytesReceived + " > _readBytesExpected=" + _readBytesExpected);
					}
				}
			} /* Notice that there isn't anything to do for
				 numBytesRead == 0.  That's intentional.  It's not an
				 error for a SocketChannel's read() to return 0.
				 Reading 0 bytes shouldn't require all the code,
				 above, right?
			   */
			else if (numBytesRead < 0) {
				// Socket closed by remote peer.  No more bytes can be
				// read from the channel
				close();
				setError(new SocketException("Socket closed by remote peer"));
				_LOG.error("Socket closed by remote peer");
			}
		}
		catch (IOException e) {
			// Connection may have been closed
			setError( e );
			deregister();
			_LOG.error(e);
		}
    }
    
    /* 
     * PURPOSE: Reconstitute an Object from the _readbuf
     * PRECONDITION: Enough data has been received to reconstitute an
     * entire object.
     * POSTCONDITION: A Java object is deserialized.
     */
    private Object deserialize() {
		Serializable obj = null;
		try {
			long startTime = System.currentTimeMillis();
			_deserializeTime = -1;
			// save the size
			int readBufSize = _readbuf.position();
			// To read the bytes, flip the buffer
			_readbuf.flip();
			ByteArrayInputStream readbytestream = new ByteArrayInputStream(_readbuf.array());
			readbytestream.reset();
			ObjectInputStream in = new ObjectInputStream(readbytestream) ;

			try {
				// read the Serializable in
				obj = (Serializable)in.readObject();
				_deserializeTime = System.currentTimeMillis() - startTime;
			}
			catch (ClassNotFoundException ex) {
				setError( ex );
				deregister();
				_LOG.error(ex);
			}
			// close the ObjectInputStream
			in.close();
    	}
		catch (IOException e) {
			setError( e );
			deregister();
			_LOG.error(e);
		}
		return obj;
    }
    
    /* 
     * PURPOSE: Write an Object into the _writebuf
     * PRECONDITION: 
     * POSTCONDITION: Creates a new _writebuf.  The Java object obj is serialized.
     */
    private void serialize(Object obj) {
		try {
			long startTime = System.currentTimeMillis();
			_serializeTime = -1;

			// reset the ByteArrayOutputStream
			_writebytestream.reset();
	    
			// build an ObjectOutputStream
			ObjectOutputStream out = new ObjectOutputStream(_writebytestream) ;
			// write the Serializable out
			out.writeObject(obj);
			// close the ObjectOutputStream
			out.close();
	    
			// Get the bytes of the serialized object and its byte length
			byte[] buf = _writebytestream.toByteArray();
			int length = _writebytestream.size();
	    
			_writebuf = ByteBuffer.allocate(length + INT_SIZE);

			// write the length of the serialized object into the outbound ByteBuffer
			_writebuf.putInt(length + INT_SIZE);
			// write the serialized object into the outbound ByteBuffer
			_writebuf.put(buf);
	    
			// set the number of bytes that need to be transmitted
			_writeBytesExpected = length + INT_SIZE;
			// shorten the buffer
			_writebuf.limit(_writeBytesExpected);

			_writebuf.flip();
			_serializeTime = System.currentTimeMillis() - startTime;
		}
		catch (IOException e) {
			_LOG.error(e);
			setError( e );
			deregister();
		}
    }
    
    /* 
     * PURPOSE: Reset the read ByteBuffer
     * PRECONDITION: The readByteBuffer contents have been processed.
     * POSTCONDITION: The read ByteBuffer is reset.
     */
    private void readbuf_reset() {
		_readbuf = ByteBuffer.allocate(INT_SIZE);
		_readbuf.clear();
		_readBytesReceived = 0;
		_readBytesExpected = INT_SIZE;
		_readbuf.limit(_readBytesExpected);
		_readPrefix = true;
    }
    
    /* 
     * PURPOSE: Reset the read ByteBuffer
     * PRECONDITION: The readByteBuffer contents have been processed.
     * POSTCONDITION: The read ByteBuffer is reset.
     */
    private void writebuf_reset() {
		_writeBytesSent = 0;
		_writeBytesExpected = 0;
    }

    /* 
	 * DEBUGGING
     */
    void debug() {
		System.out.println("sc.isConnected      ="+_sc.isConnected());
		System.out.println("sc.ch.sock.isClosed ="+ _sc.socket().isClosed());

		System.out.println("sc.ch.sock.isInputShutdown  ="+_sc.socket().isInputShutdown());
		System.out.println("sc.ch.sock.isOutputShutdown ="+_sc.socket().isOutputShutdown());

		System.out.println("_skread.isNull      ="+_skread==null);
		if (_skread!=null) {
			System.out.println("   _skread.isValid ="+_skread.isValid());
			System.out.println("   _skread.isReadable ="+_skread.isReadable());
		}
		System.out.println("_skwrite.isNull     ="+_skwrite==null);
		if (_skwrite!=null) {
			System.out.println("   _skwrite.isValid ="+_skwrite.isValid());
			System.out.println("   _skwrite.isWritable ="+_skwrite.isWritable());
		}
    }

    /* 
	 * DEBUGGING
     */
    void zero() {
		if (_skread!=null) {
			synchronized (_skread) {
				_skread.cancel();
				_skread = null;
			}
		}
	
		if (_skwrite!=null) {
			synchronized (_skwrite) {
				_skwrite.cancel();
				_skwrite = null;
			}
		}
    }
}
