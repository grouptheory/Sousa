package mil.navy.nrl.cmf.sousa.idol.service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Set;

import mil.navy.nrl.cmf.sousa.*;

import mil.navy.nrl.cmf.sousa.directory.Advertiser;
import mil.navy.nrl.cmf.sousa.idol.IdolInitializer;
import mil.navy.nrl.cmf.sousa.spatiotemporal.QueryFields;
import mil.navy.nrl.cmf.sousa.util.StackTrace;
import mil.navy.nrl.cmf.sousa.util.Strings;

import org.apache.log4j.Logger;

/**
   ServerInitializer initializes an IDOL server (or service) Entity.
*/
public abstract class ServerInitializer
    extends IdolInitializer
{
	static final Logger _LOG = 
		Logger.getLogger(ServerInitializer.class);

	/**
	   Server_ControlLogic is an
	   <CODE>IdolInitializer.Console_ControlLogic</CODE> with the
	   ability to advertise the service and to periodically update the
	   advertisement.
	 */
    public static class Server_ControlLogic 
		extends IdolInitializer.Console_ControlLogic
		implements Clock.AlarmHandler
	{
		/**
		 * A recurring alarm that initiates advertising with a
		 * directory service.  It expires after
		 * <CODE>_advertiseSelfInterval</CODE> milliseconds.
		 */
		private Clock.Alarm _advertiseSelfAlarm = null;

		/**
		 * Number of milliseconds between successive invocations of
		 * <CODE>advertise</CODE> on the directory server
		 * <CODE>Receptor</CODE>.  It's probably good if
		 * <CODE>_advertiseSelfInterval</CODE> is less than
		 * <CODE>_advertiseSelfTTL</CODE> to maintain the continuity
		 * of the advertisement.
		 */
		private int _advertiseSelfInterval = -1;

		/**
		 * Number of milliseconds for the directory server to
		 * advertise this server
		 */
		private Integer _advertiseSelfTTL = new Integer(-1);

		/**
		 * The SelectableFutureResults that the directory server's
		 * <CODE>advertise</CODE> method returned.  They are obtained
		 * in <CODE>receptorReadyIndication</CODE>.
		 * <CODE>handle(Selectable, SignalType)</CODE> tells us when
		 * one of the real values is available.
		 *
		 * @see #receptorReadyIndication(ClientSideFSM)
		 * @see #handle(Selectable, SignalType)
		 */
		private HashSet _advertiseSelfResult = new HashSet();

		/**
		 * The <CODE>ClientSideFSM</CODE> of the directory server from
		 * whom we obtained <CODE>_advertiseSelfResult</CODE>.  Its
		 * value is assigned in <CODE>receptorReadyIndication</CODE>.
		 * When <CODE>handle(Selectable, SignalType)</CODE> tells us
		 * that <CODE>_advertiseSelfResult</CODE> is readable, we
		 * deregister <CODE>_advertiseSelfFSM</CODE> and set it to
		 * <CODE>null</CODE>.
		 *
		 * @see #receptorReadyIndication(ClientSideFSM)
		 * @see #handle(Selectable, SignalType)
		 */
		private ClientSideFSM _advertiseSelfFSM = null;

		/**
		 * Concact info for the directory server.  The
		 * <CODE>QoS</CODE> contains only
		 * <CODE>Advertiser.class</CODE>.
		 */
		private ServerContact _directoryServer = null;

		// Constructors

		/**
		   Class constructor that uses the value of the property
		   <CODE>idol.directory.advertiseSelf.interval</CODE> to
		   determine how often to advertise and the value of the
		   property <CODE>idol.directory.advertiseSelf.TTL</CODE> to
		   determine how long each advertisement will be valid.
		   <P>
		   If either property is present in <CODE>p</CODE>, its value
		   must be an integer greater than zero.
		   <P>
		   <CODE>idol.directory.advertiseSelf.interval</CODE> should
		   be less than <CODE>idol.directory.advertiseSelf.TTL</CODE>
		   in order to be able to re-advertise before the current
		   advertisement expires.

		   @param p the <CODE>Properties</CODE> of this <CODE>Server_ControlLogic</CODE>

		   @throws EntityInitializer.InitializationException if either
		   of the two properties is in <CODE>p</CODE> but its value is
		   not an integer greater than zero
		*/
		public Server_ControlLogic(Properties p)
			throws EntityInitializer.InitializationException
		{
			super(p);

			String directoryAdvertiseSelfIntervalString = p.getProperty("idol.directory.advertiseSelf.interval");
			String directoryAdvertiseSelfTTLString = p.getProperty("idol.directory.advertiseSelf.TTL");

			if (null != directoryAdvertiseSelfIntervalString) {
				try {
					_advertiseSelfInterval = Integer.parseInt(directoryAdvertiseSelfIntervalString);
					if (_advertiseSelfInterval <=0) {
						throw new EntityInitializer.InitializationException("idol.directory.advertiseSelf.interval ("
																			+ _advertiseSelfInterval + 
																			") must be greater than 0");
					}
				} catch (NumberFormatException ex) {
					throw new EntityInitializer.InitializationException("Error initializing idol.directory.advertiseSelf.interval", ex);
				}
			}

			if (null != directoryAdvertiseSelfTTLString) {
				try {
					_advertiseSelfTTL = new Integer(Integer.parseInt(directoryAdvertiseSelfTTLString));

					if (_advertiseSelfTTL.intValue() <=0) {
						throw new EntityInitializer.InitializationException("idol.directory.advertiseSelf.TTL ("
																			+ _advertiseSelfTTL + 
																			") must be greater than 0");
					}
				} catch (NumberFormatException ex) {
					throw new EntityInitializer.InitializationException("Error initializing idol.directory.advertiseSelf.TTL ", ex);
				}
			}

			ServerInitializer._LOG.debug(new Strings(new Object[]
				{"Advertising interval=", new Integer(_advertiseSelfInterval), 
				 "ms TTL=", _advertiseSelfTTL, "ms"}));
		}

		/**
		   If <CODE>fsm</CODE> is for a connection to a Directory
		   server, advertise in it.  Otherwise, just do the base
		   class's actions.
		   <P>

		   In order for advertisement to work, the Entity's State must
		   include a String field whose name is defined by {@link
		   mil.navy.nrl.cmf.sousa.spatiotemporal.QueryFields#DESCRIPTION_FIELDNAME}.

		   @param fsm the new connection to a server
		*/
		public void receptorReadyIndication(ClientSideFSM fsm) {
			super.receptorReadyIndication(fsm);
	    
			ServerContact serverContact = fsm.getServerContact();
			QoS serverQoS = serverContact.getQoS();
	    
			//
			// TODO: Don't advertise if _advertiseSelfTTL == -1
			// or _advertiseSelfInterval == -1
			//
			// TODO: Consider how to advertise with more than one
			// directory server.  This code assumes only one directory
			// server.
			if (serverQoS.contains(Advertiser.class)) {

				try {
					String description = (String)
						getEntity().getState().getField(QueryFields.DESCRIPTION_FIELDNAME);

					_advertiseSelfFSM = fsm;

					Advertiser view = (Advertiser)fsm.genProxy(Advertiser.class);

					Set contentTypes = getEntity().getContentTypes();
					ServerContact contact = getEntity().getServerContact();
					QoS qos = contact.getQoS();

					// One advertisement per Content-Type.
					for (Iterator i=contentTypes.iterator(); i.hasNext(); ) {

						QoS q = new QoS(qos.getSession(), qos, (String)i.next());
						try {
							ServerContact c = new ServerContact(contact.getHost(),
																contact.getPort(),
																q);
							ServerInitializer._LOG.debug(new Strings(new Object[] 
								{"receptorReadyIndication(): advertising with ",
								 c}));

							_advertiseSelfResult.add((SelectableFutureResult)
								view.advertise(c, description, _advertiseSelfTTL));
						} catch (UnknownHostException ex) {
							_LOG.error(new Strings(new Object[] 
								{"receptorReadyIndication(): making advertisement ", 
								 ex}));
						}
					}

					// It would be nice if we could deregister the
					// ClientSideFSM now.  We can't because we haven't a
					// real answer from the directory server.
					// _advertiseResult is a SelectableFutureResult and we
					// have to wait until its value is filled in by the
					// directory server.
					//
					// What happens next?  Wait for _advertiseResult to become readable.
					// Then we can deregister the ClientSideFSM and
					// schedule the _advertiseSelfAlarm.

					//
					// Prepare for scheduling _advertiseSelfAlarm.
					// Don't share the session.  This saves the
					// directory server from setting up a multicast
					// call.
					//
					QoS directoryQoS = new QoS(-1);
					directoryQoS.add(Advertiser.class);

					try {
						_directoryServer = new ServerContact(serverContact.getHost(), 
															 serverContact.getPort(), 
															 directoryQoS);
					} catch (UnknownHostException ex) {
						ServerInitializer._LOG.error(new Strings(new Object[] 
							{"Can't construct ServerContact for directory server ",
							 serverContact, ": ", ex}));
					}
				} catch (NoSuchFieldException ex) {
					ServerInitializer._LOG.error(ex);
				}
			}
		}

		/**
		   Processes the result of RMI to the directory service.
		   Delegates all other selectables to the base class.
		   <P>
		   If <CODE>sel</CODE> is the
		   <CODE>SelectableFutureResult</CODE> for an attempt to
		   advertise with a directory service, then
		   <CODE>handle</CODE> disconnects from the server and
		   schedules itself to advertise again after the interval
		   given by the property
		   <CODE>idol.directory.advertiseSelf.interval</CODE>.

		   @param sel the Selectable that is readable, writable, or in error
		   @param st the operation that may be performed on <CODE>sel</CODE>
		 */
		public void handle(Selectable sel, SignalType st) {
			//
			// I'm not interested in anything other than
			// one of the members of_advertiseSelfResult.
			//
			if (_advertiseSelfResult.remove(sel)) {
				if (SignalType.READ == st) {
					Object obj = sel.read();
					_LOG.debug(new Strings(new Object[] 
						{"ServerInitializer.handle(): Read ", obj}));
					
					// Now that we've advertised, hang up!
					if (_advertiseSelfResult.isEmpty()) {
						_advertiseSelfFSM.deregister();
						_advertiseSelfFSM = null;

						// Start a recurring timer that tells me when to
						// re-advertise with my directory server.
						if (null == _advertiseSelfAlarm) {
							Entity e = getEntity();
							Clock c = e.getClock();
							_advertiseSelfAlarm = c.setAlarm(_advertiseSelfInterval,
															 true, null, this);
						}
					}

				} else if (SignalType.ERROR == st) {
					_LOG.error(new Strings(new Object[] {
											   sel, " threw exception ", sel.getError()}));
				} else {
					_LOG.error(new Strings(new Object[]
						{sel, " is ", st, ". WHY AM I INTERESTED IN THIS?"}));
				}
			} else super.handle(sel, st);
		}

		//// Clock.AlarmHandler

		/**
		   Re-advertises the Entity with the directory service.

		   @param m the alarm that expired
		 */
		public void handle(Clock.Alarm m) {
			if (m == _advertiseSelfAlarm) {
				getEntity().scheduleConnectTo(_directoryServer);

				ServerInitializer._LOG.debug(new Strings(new Object[] 
					{"handle(", m, "): fetching from directory server ", 
					 _directoryServer}));

				m.enable();
			} else {
				ServerInitializer._LOG.debug(new Strings(new Object[] 
					{"ServerControlLogic.handle(",
					 m, "): unknown Clock.Alarm"}));
			}
		}
	}

	/**
	   Class constructor that uses a Property to determine the initial
	   values of the Entity's communications parameters and State fields.

	   @param p the Properties of this ServerInitializer
	*/
     public ServerInitializer(Properties p)
		 throws EntityInitializer.InitializationException
	{
		super(p);
	}

    // EntityInitializer

    /**
	   Initiates the advertisement process with a directory server if
	   one is named in <CODE>p</CODE>.  The address of the directory
	   server is determined from the property
	   <CODE>idol.directory.address</CODE>.  If present, it must be a
	   reachable address.  The contact port of the directory server is
	   determined from <CODE>idol.directory.contactport</CODE>.  If
	   present, it must be a valid IP port number.  If either are
	   null, then <CODE>initialize_custom</CODE> does not attempt to
	   connect to the directory server.

	   <P>
	   To support advertisement, the ControlLogic must invoke the
       Directory Server's <CODE>advertise</CODE> method when its
       Receptor is ready.

	   <P>
	   Derived classes must add <CODE>QueryFields.DESCRIPTION_FIELDNAME</CODE> to
	   the authoritative State in order for advertisement to succeed.

	   @param p the <CODE>Properties</CODE> of this
	   <CODE>ServerInitializer</CODE>

	   @throws EntityInitializer.InitializationException if 
	   <UL>
	   <LI><CODE>idol.directory.address</CODE> is defined but does not represent a valid address
	   <LI>if a security manager exists and its checkConnect method doesn't allow calling <CODE>InetAddress.getByName</CODE>
	   <LI><CODE>idol.directory.contactport</CODE> is defined but does not represent a valid port number
	   </UL>

	   @see ServerInitializer.Server_ControlLogic#receptorReadyIndication(ClientSideFSM)
	   @see mil.navy.nrl.cmf.sousa.spatiotemporal.QueryFields#DESCRIPTION_FIELDNAME
    */
    protected void initialize_custom(Properties p) 
		throws EntityInitializer.InitializationException {

		String directoryAddressString = p.getProperty("idol.directory.address");
		String directoryPortString = p.getProperty("idol.directory.contactport");
		int directoryPort = -1;
		boolean haveDirectory = ((null != directoryAddressString) && 
								 (null != directoryPortString));


		// If I have a non-null directoryAddressString and a non-null
		// directoryPortString and directoryPortString represents a
		// valid integer, I'll schedule a fetch of a
		// ClientViewInterpreter from the Directory server.  
		//
		if (haveDirectory) {
			try {
				directoryPort = Integer.parseInt(directoryPortString);
				// The Directory Server must provide the
				// Advertiser interface.  When we receive a
				// Receptor from the Directory Server, we must
				// invoke 
				//
				// Advertiser.advertise(ourServerContact, ourDescription, ourTTL)
				//
				// to list ourselves with it.  Implement that in
				// ControlLogic.receptorReadyIndication().
				//
				// Don't share the session.  This saves the
				// directory server from setting up a multicast
				// call.
				QoS directoryQoS = new QoS(-1);
				directoryQoS.add(Advertiser.class);
		
				// Throws UnknownHostException, SecurityException.
				// Both are caught below.
				InetAddress directoryAddress = 
					InetAddress.getByName(directoryAddressString);
		
				ServerContact directoryServer = 
					new ServerContact(directoryAddress, 
									  directoryPort, directoryQoS);
		
				this.scheduleConnectTo(directoryServer);
		
				ServerInitializer._LOG.warn(new Strings(new Object[] {
															"Using Directory ", directoryServer}));
			} 
			catch (NumberFormatException ex) {
				throw new EntityInitializer.InitializationException("Error initializing directory port", ex);
			}
			catch (java.net.UnknownHostException ex) {
				throw new EntityInitializer.InitializationException("Error initializing directory address", ex);
			}
			catch (SecurityException ex) {
				throw new EntityInitializer.InitializationException("Error initializing directory address", ex);
			}
		} else {
			ServerInitializer._LOG.warn("Continuing without Directory.");
		}
    }

}; // ServerInitializer
