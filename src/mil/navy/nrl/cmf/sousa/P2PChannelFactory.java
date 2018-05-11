package mil.navy.nrl.cmf.sousa;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;

/**
 * A factory for making P2PChannel objects corresponding to
 * incoming connections from clients and outgoing connections to
 * servers.
 */
public class P2PChannelFactory extends NonBlockingObjectChannel implements Runnable {
	private static final Logger _LOG = 
		Logger.getLogger(P2PChannelFactory.class);

    // the Entity in which this P2PChannelFactory resides.
    protected final Entity _entity;
	
    // the low-level I/O thread
    private final Thread _io_thread = new Thread(this, "P2PChannelFactory-NIO");
    // default select time for the NIO thread
    private final long DEFAULT_SELECT_TIME_NIO = 10000;

    // the ServerSocketChannel on which to listen for incoming client
    // connection requests.
	//
    private final ServerSocketChannel _ss;
    // accept() SelectionKey
    private SelectionKey _skaccept;
    // connect SelectionKeys as a map: SelectionKey->ServerContact
    private final HashMap _skconnect = new HashMap();
    // set of all presently active P2PChannels made by this Factory
    private final HashSet _channels = new HashSet();
	
    // the listening port
    private final int _port;
	
    /* 
     * PURPOSE: Make P2PChannelFactory which can produce outbound
     * inbound and outbound P2PChannels.
     * PRECONDITION: port>1024 is unbound, e!=null.
     * POSTCONDITION: A new P2PChannelFactory is made, which produces
     * incoming P2PChannels by listening and accepting connections on
     * the specified port.  Outgoing socket channels are made
     * synchronously upon request.  
     */
    public static P2PChannelFactory newInstance(int port, Entity e) throws IOException {
		return new P2PChannelFactory(port, e);
    }
	

    /* 
     * PURPOSE: Make a P2PChannelFactory which can produce outbound
     * inbound and outbound P2PChannels.
     * PRECONDITION: port>1024 is unbound, e!=null.
     * POSTCONDITION: A new P2PChannelFactory is made, which
     * produces incoming P2PChannels by listening and accepting
     * connections on the specified port.  Outgoing socket channels
     * are made synchronously upon request.
     */
    private P2PChannelFactory(int port, Entity e) throws IOException {
		// save a reference to the owning Entity
		_entity = e;
		// save the port 
		_port = port;
		// create the server socket channel
		_ss = ServerSocketChannel.open();
		// bind the port
		_ss.socket().bind(new java.net.InetSocketAddress(port));
		// _ss uses nonblocking I/O
		_ss.configureBlocking(false);
		// start running
		_io_thread.start();
    }

    // get the port number
    int getPort() {
	return _port;
    }

    // is the P2PChannelFactory closed?
    boolean isClosed() {
		return (!_ss.isOpen());
    }

    // close this P2PChannelFactory
    void close() throws IOException {
		_ss.close();
    }

    /* 
     * PURPOSE: The low-level IO thread processes read and write
     * requests from and to all NIO SocketChannels which underly this
     * factory and the P2PChannels it has made.
     * PRECONDITION: none.
     * POSTCONDITION: give ChannelHandlers time as needed.
     */
    public void run() {
		try {
			int ct=0;
			_selector = Selector.open();
			do {
				// register this P2PChannelFactory for accept/connect as appropriate
				this.register(_selector);
				
				try {
					// DEBUGGING
				    //System.out.println(Thread.currentThread().getName()+" selecting on "+_selector.keys().size()+" keys.");
					
					// DAVID: Why not use _selector.select() instead?
					// Bilal says that HUPs don't break the select.
					_selector.select(DEFAULT_SELECT_TIME_NIO);
				    //System.out.println(Thread.currentThread().getName()+" select() returns.");
					
					
					Set keys = _selector.selectedKeys();
					for (Iterator it=keys.iterator(); it.hasNext();) {
						SelectionKey sk= (SelectionKey)it.next();
						it.remove();
						NonBlockingObjectChannel ch = (NonBlockingObjectChannel)sk.attachment();
						// handle the SelectableKey
						ch.handle(sk);
						// cancel connect keys as they are handled
						if (_skconnect.get(sk) != null) {
							_skconnect.remove(sk);
							try {
								sk.interestOps(SelectionKey.OP_READ);
							} catch (CancelledKeyException ex) {
								_LOG.error(ex);
							}
						}
					}
				}
				catch (IOException ex) {
					_LOG.error(ex);
				}
			}
			
			while(true);
		}
		catch (IOException ex) {
			_LOG.error(ex);
		}
    }
	
    /* 
     * PURPOSE: Register this P2PChannelFactory and all (undisposed)
     * P2PChannels it has made with a Selector.
     * PRECONDITION: selector!=null.
     * POSTCONDITION: This P2PChannelFactory is registered with the
     * specified selector and will be notified on ACCEPT events.
     */
    public void register(Selector selector) {
		// save the Selector
		super.register(selector);

		// are we registered to accept?
		if (_skaccept == null) {
			// Recording _ss to selector (type OP_ACCEPT)
			try {
				_skaccept = _ss.register(selector, NonBlockingObjectChannel.OP_NONE, this);
			}
			catch (ClosedChannelException ex) {
				_LOG.error(ex);
				setError(ex);
			}
		}
		
		// is there space for incoming connections?
		if ((spaceRead() > 0)) {
			// sensitive to acceptability
			_skaccept.interestOps(SelectionKey.OP_ACCEPT);
		} else {
			// sensitive to nothing
			_skaccept.interestOps(NonBlockingObjectChannel.OP_NONE);
		}
		
		// are there outbound connection requests?
		if ((dataWrite() > 0)) {
			// make a new output channel
			try {
				SocketChannel cs = SocketChannel.open();
				// use non-blocking I/O for the new connection
				cs.configureBlocking(false);
				// get the next destination
				ServerContact addr = (ServerContact)take_outQ();
				// issue initial phase of connect
				cs.connect(new java.net.InetSocketAddress(addr.getHost(), addr.getPort()));
				// sensitive to ability to complete connect
				SelectionKey sk = cs.register(selector,SelectionKey.OP_CONNECT,this);
				// save the key for this connect
				_skconnect.put(sk,addr);				
			}
			catch (ClosedChannelException ex) {
				_LOG.error(ex);
				setError(ex);
			}
			catch (IOException ex) {
				_LOG.error(ex);
				setError(ex);
			}
		}

		synchronized(_channels) {
		    for (Iterator it=_channels.iterator(); it.hasNext();) {
			P2PChannel p2p = (P2PChannel)it.next();
			//System.out.println("Registering p2pchannel: "+p2p);
			p2p.register(selector);
		    }
		}
    }
	
    /* 
     * PURPOSE: Deregister this P2PChannelFactory with a Selector.
     * PRECONDITION: This P2PChannelFactory is previously registered with the
     * specified selector.
     * POSTCONDITION: This P2PChannelfactory is no longer registered with the
     * Selector.
     */
    void deregister() {
    }
	
    /* 
     * PURPOSE: Give the P2PChannelFactory time to handle the ACCEPT event.
     * PRECONDITION: key.associated-data==this

     * POSTCONDITION: Incoming connection is accepted and a
     * P2PChannel is placed in the incoming connections queue.
     */
    public void handle(SelectionKey key) {
		SocketChannel sc = null;
		ServerContact addr = null;

		// if isAcceptable == true, an incoming connection is ready
		if (key.isAcceptable()) {
			try {
				// accept the connection
				sc = _ss.accept();
				// use non-blocking I/O for the new connection
				sc.configureBlocking(false);
			}
			catch (IOException ex) {
				_LOG.error(ex);
				setError(ex);
			}
		}
		// if isConnectable == true, an outgoing connection is ready
		else if (key.isConnectable()) {
			
			sc = (SocketChannel)key.channel();
			try {
				sc.finishConnect();
			}
			catch (IOException ex) {
				_LOG.error(ex);

				try {
					sc.close();
				}
				catch (IOException ex2) {
					_LOG.error(ex);
				}
				finally {
					sc = null;					
				}
			}
			finally {
				addr = (ServerContact)_skconnect.get(key);
			}
		}
		
		P2PChannel cc = null;
		if (sc != null) {
			cc = new P2PChannel(sc, addr, this);
			registerP2PChannel(cc);
			put_inQ(cc);
		}
    }

    private void registerP2PChannel(P2PChannel child) {
	synchronized(_channels) {
	    _channels.add(child);
	}
    }

    void deregisterP2PChannel(P2PChannel child) {
	synchronized(_channels) {
	    _channels.remove(child);
	}
    }

	public InetAddress getInetAddress() {
		return _ss.socket().getInetAddress();
	}

	// Usage: P2PChannelFactory -s
	// Usage: P2PChannelFactory -c serverhostname
	//
	// Run server first.
	// Run client second within 10 seconds.
	//
	// Client connects to server.  Server sends a series of Strings,
	// each twice as large as the one before.  Client and server
	// report the lengths of the Strings.
	//
	public static void main(String args[]) {
		if (args[0].equals("-s")) { // run as server
			try {
				P2PChannelFactory f = P2PChannelFactory.newInstance(9000, null);
				int numReady;
				P2PChannel toClient = null;
				SelectableSet selectables = new SelectableSet();

				// Listen for connect attempts: select() until readable
				// Notice that there is no Selectable.Handler.
				selectables.addSelectable(f, SignalType.READ, null);
				numReady = selectables.select(10000);
				for (Iterator i = selectables.iterator(SignalType.READ); i.hasNext(); ) {
					Selectable s = (Selectable)i.next();
					toClient = (P2PChannel)s.read();
				}

				// Once there is a connection, write a series of
				// increasingly large objects.

				for (int buflen=1024; buflen < (1024 * 1024 * 8); buflen *=2) {
					char[] buf = new char[buflen];
					java.util.Arrays.fill(buf, 'A');

					selectables.clearAllSelectables(SignalType.ERROR);
					selectables.clearAllSelectables(SignalType.READ);
					selectables.clearAllSelectables(SignalType.WRITE);
					selectables.addSelectable(toClient, SignalType.WRITE, null);
					numReady = selectables.select(10000);
					for (Iterator i = selectables.iterator(SignalType.WRITE); i.hasNext(); ) {
						Selectable s = (Selectable)i.next();
						_LOG.warn("Writing a String of length " + buflen);
						s.write(new String(buf));
					}
				}
			} catch (IOException ex) {
				_LOG.error(ex);
				System.exit(-1);
			}

			System.exit(0);
		} else if (args[0].equals("-c")) { // run as client
			try {
				P2PChannelFactory f = P2PChannelFactory.newInstance(9001, null);
				int numReady;
				P2PChannel fromServer = null;
				SelectableSet selectables = new SelectableSet();
				InetAddress remoteAddress = InetAddress.getByName(args[1]);
				ServerContact sc = new ServerContact(remoteAddress, 9000, (QoS)null);
			
				// Write connect attempt to initiate connection
				// select() until writable.
				// Notice that there is no Selectable.Handler.
				selectables.clearAllSelectables(SignalType.ERROR);
				selectables.clearAllSelectables(SignalType.READ);
				selectables.clearAllSelectables(SignalType.WRITE);
				selectables.addSelectable(f, SignalType.WRITE, null);
				numReady = selectables.select(10000);
				for (Iterator i = selectables.iterator(SignalType.WRITE); i.hasNext(); ) {
					Selectable s = (Selectable)i.next();
					s.write(sc);
				}
			
				// Wait for the server to accept.
				selectables.clearAllSelectables(SignalType.ERROR);
				selectables.clearAllSelectables(SignalType.READ);
				selectables.clearAllSelectables(SignalType.WRITE);
				selectables.addSelectable(f, SignalType.READ, null);
				numReady = selectables.select(10000);
				for (Iterator i = selectables.iterator(SignalType.READ); i.hasNext(); ) {
					Selectable s = (Selectable)i.next();
					fromServer = (P2PChannel)s.read();
				}

				while(true) { 
					// Read until there is an error.  Then hang up.
					selectables.clearAllSelectables(SignalType.ERROR);
					selectables.clearAllSelectables(SignalType.READ);
					selectables.clearAllSelectables(SignalType.WRITE);
					selectables.addSelectable(fromServer, SignalType.READ, null);
					numReady = selectables.select(10000);
					for (Iterator i = selectables.iterator(SignalType.READ); i.hasNext(); ) {
						Selectable s = (Selectable)i.next();
						String buf = (String)s.read();
						_LOG.warn("Read a String of length " + buf.length());
					}
				}
			} catch (IOException ex) {
				_LOG.error(ex);
				System.exit(-1);
			}
		} else {
			System.out.println("Usage: P2PChannelFactory -s");
			System.out.println("Usage: P2PChannelFactory -c serverhostname");
		}

	}
}

