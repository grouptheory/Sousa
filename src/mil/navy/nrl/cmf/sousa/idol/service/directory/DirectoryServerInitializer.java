package mil.navy.nrl.cmf.sousa.idol.service.directory;

import java.io.Serializable;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;
import mil.navy.nrl.cmf.sousa.*;
import mil.navy.nrl.cmf.sousa.idol.IdolInitializer;
import mil.navy.nrl.cmf.sousa.directory.*;
import mil.navy.nrl.cmf.sousa.idol.service.ServerInitializer;
import mil.navy.nrl.cmf.sousa.spatiotemporal.QueryFields;
import mil.navy.nrl.cmf.sousa.util.MapUtil;
import mil.navy.nrl.cmf.sousa.util.Strings;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;


/**
 * DirectoryServerInitializer constructs the ControlLogic for a
 * directory server Entity.  
 * <P>
 * A directory server has two kinds of clients: advertisers and
 * consumers.
 * <P>
 * An <EM>advertiser</EM> places an advertisement in the directory server.  An
 * advertisement is a tuple &lt;ServerContact, String, int&gt;.  The
 * int is the time-to-live in milliseconds.  After that many
 * milliseconds, the directory server will remove the tuple
 * &lt;ServerContact, String&gt; from the directory.
 * <P>
 * A <EM>consumer</EM> subscribes to all of the advertisements in a directory
 * server.
 * <P>
 * <EM>TODO: Consider making the directory server conform to the
 * expectations of the Queryable interface.  It need not implement
 * Queryable but it must send QueryResultHandles to its consumers.</EM>
 */
public class DirectoryServerInitializer
    extends ServerInitializer
{
	static final Logger _LOG = 
		Logger.getLogger(DirectoryServerInitializer.class);
    
    /**
      DirectoryServer_ControlLogic expects a client to be an advertiser or a
      consumer.

      <P>

      An advertiser supplies its ServerContact and description, a String,
      using 
	  {@link mil.navy.nrl.cmf.sousa.directory.Advertiser#advertise(ServerContact, String, Integer)}.

      <P>

      A consumer requests the <code>Field</code> named 
	  {@link mil.navy.nrl.cmf.sousa.directory.DirectoryFields#DIRECTORY_FIELDNAME}.
	  That <code>Field</code> is a map of from 
	  the {@link mil.navy.nrl.cmf.sousa.ServerContact} address of the 
	  advertising client to String.

      <P>
      
	  As advertments age, DirectoryServer_ControlLogic removes entries
      from the DIRECTORY_FIELD.

	  <P>

	  <EM>TODO: This implementation implements QoS by limiting the
	  rate at which it applies changes to the authoritiative State.
	  That makes it more complicated that it could be.  Consider
	  simplifying it by applying changes to authoritative State as
	  they occur.  Let the ViewInterpreters limit the rate of updates
	  to the clients.</EM>
    */
    public static class DirectoryServer_ControlLogic 
		extends ServerInitializer.Server_ControlLogic
		implements Advertiser, Clock.AlarmHandler
    {
		/**
		   When _advertiseAlarm expires, this Directory server applies
		   all of the MapTransactions that are contained in
		   {@link #_transactions} to the DIRECTORY field of the authoritative
		   State.  This will probably cause a Projector to send a
		   directory update to the consumers.
		*/
		private Clock.Alarm _advertiseContentsAlarm = null;

		/**
		   Apply changes to the directory no more frequently than this
		   number of milliseconds.  It's the period of
		   {@link #_advertiseContentsAlarm}.
		 */
		private int _advertiseContentsInterval = -1;

		/**
		   When _ttlAlarm expires, this Directory server begins to
		   remove the the oldest entry of the DIRECTORY field of the
		   authoritative State.  The transaction completes when
		   {@link #_advertiseContentsAlarm} expires.
		*/
		private Clock.Alarm _ttlAlarm = null;
	
		/**
		   Maps Long to List of {@link
		   mil.navy.nrl.cmf.sousa.ServerContact}.  The key is the time
		   at which the directory entries for the ServerContacts
		   expire.  Time is measured in milliseconds since the Epoch
		   (midnight, January 1, 1970).
		*/
		private SortedMap _expirations = new TreeMap();

		/**
		   Maps {@link mil.navy.nrl.cmf.sousa.ServerContact} to Long.
		   The key is the ServerContact of an advertiser.  The value
		   is the expiration time of the client's advertisement.  Time
		   is in milliseconds since the Epoch.
		*/
		private Map _advertiserExpiration = new HashMap();

		/**
		   _updateLock is the semaphore for collecting and applying
		   changes to authoritative State.
		*/
		private final Object _updateLock = new Object();
	
		/**
		   _transactions maintains the order of changes to be applied
		   to the advertisers field of the authoritative State.  Each
		   change is a pair &lt;ServerContact, String&gt;.  The
		   changes are applied to authoritative State and cleared
		   every time {@link #_advertiseContentsAlarm} expires.

		   <P>
		   Changes to _transactions are controlled by {@link #_updateLock}.
		*/
		private final MapTransactions _transactions = new MapTransactions();
	
		/**
		   DirectoryServer_ControlLogic accepts advertisements from
		   <EM>advertisers</EM> (service clients that wish to
		   advertise) and disseminates the advertisements to
		   <EM>consumers</EM> (clients that wish to know about those
		   advertisements).
		   <P>
		   It exposes the {@link #advertise(ServerContact, String, Integer)} 
		   method to advertisers.
		   <P>

		   The property
		   <CODE>idol.directory.advertiseContents.interval</CODE> is
		   the rate at which DirectoryServer_ControlLogic sends
		   directory updates to consumers.  It is measured in
		   milliseconds.  It must be a positive integer.

		   @param p the properties of this DirectoryServer_ControlLogic

		   @throws EntityInitializer.InitializationException when
		   <CODE>idol.directory.advertiseContents.interval</CODE> is
		   either missing or not an integer greater than zero.
		*/
		public DirectoryServer_ControlLogic(Properties p)
			throws EntityInitializer.InitializationException {
			super(p);
			String directoryAdvertiseContentsIntervalString = 
				p.getProperty("idol.directory.advertiseContents.interval");

			if (null != directoryAdvertiseContentsIntervalString) {
				try {
					_advertiseContentsInterval = Integer.parseInt(directoryAdvertiseContentsIntervalString);
					if (_advertiseContentsInterval <=0) {
						throw new EntityInitializer.InitializationException("idol.directory.advertiseContents.interval ("
																			+ _advertiseContentsInterval + 
																			") must be greater than 0");
					}
				} catch (NumberFormatException ex) {
					throw new EntityInitializer.InitializationException("Error initializing idol.directory.advertiseContents.interval", ex);
				}
			} else {
				throw new EntityInitializer.InitializationException("Missing idol.directory.advertiseContents.interval");
			}
		}
	
		// ControlLogic
	
		/**
		   Perform the actions of the super class, then 
		   start {@link #_advertiseContentsAlarm} if it hasn't already
		   been started.  

		   @param fsm the new connection to a client
		*/
		public void projectorReadyIndication(ServerSideFSM fsm) {
			super.projectorReadyIndication(fsm);
	    
			// Instantiate and schedule an Alarm if there isn't one
			// already.  This is the only way to initialize values
			// that depend on the Entity that owns this
			// DirectoryServer_ControlLogic.
			if (null == _advertiseContentsAlarm) {
				Entity e = getEntity();
				Clock c = e.getClock();
				_advertiseContentsAlarm = c.setAlarm(_advertiseContentsInterval /* period in milliseconds */, 
													 true /* recurring Clock.Alarm */, 
													 "advertiseContentsAlarm" /*null /* user data for AlarmHandler.handle() */,
													 this /* the Clock.AlarmHandler */);
			}
		}

		// Advertiser

		/**
		   Accept an advertisement from some client.  Ignore
		   advertisements with non-positive TTLs.  The advertisement
		   will be included in the next directory update.  It will
		   persist from the next directory update until the directory
		   update following the expiration of its TTL.

		   <P>
		   <EM>TODO: Consider throwing an Exception when <CODE>ttl</CODE> &le; 0.</EM>

		   @param contact the contact address of the client
		   @param description text that briefly describes the information
		   offered by the advertiser
		   @param ttl the length of the advertisement in milliseconds

		   @return <CODE>null</CODE>
		 */
		public Object advertise(ServerContact contact, String description, Integer ttl) {
			// DAVID: Consider throwing an Exception when ttl <= 0.

			// DAVID: It might be OK to do this without transactions.
			// Consider keeping a set of added and a set of removed.
			// Use the same logic as MapFieldValue's put() and
			// remove().
	    
			if ((null != ttl) && (ttl.intValue() >= 0)) {
				_transactions.add(contact, description, ttl);

				DirectoryServerInitializer._LOG.debug(new Strings(new Object[] 
					{"Advertising <", contact, ", ", description, "> ttl=", ttl, 
					 " #transactions=", new Integer(_transactions.size()) }));
			}

			return null;
		}
	
		//// Clock.AlarmHandler
	
		/**
		   Process the expiration of {@link #_advertiseContentsAlarm} and
		   {@link #_ttlAlarm}; delgate to the super class for all other alarms.
		   <P>

		   When <CODE>m</CODE> is
		   <CODE>_advertiseContentsAlarm</CODE>, apply all of the
		   transactions in {@link #_transactions} to the authoritative
		   State.  Reschedule <CODE>_ttlAlarm</CODE>.

		   <P>

		   When <CODE>m</CODE> is <CODE>_ttlAlarm</CODE>, place one
		   Remove transaction into <CODE>_transactions</CODE> for each
		   advertisment that has expired.

		   @param m an alarm that expired
		*/
		public void handle(Clock.Alarm m) {
			long now = System.currentTimeMillis();

			if (m == _advertiseContentsAlarm) {
				State authoritativeState = getEntity().getState();
		
				// DAVID: Should I make a copy of the DIRECTORY field?
				if (_transactions.size() > 0) {
					try {
						HashMap advertisers = 
							(HashMap)authoritativeState.getField(DirectoryFields.DIRECTORY_FIELDNAME);
			
						if (null != _ttlAlarm)
							_ttlAlarm.disable();

						for (Iterator i = _transactions.iteratorAll(); 
							 i.hasNext(); ) {
							MapTransactions.MapTransaction t = 
								(MapTransactions.MapTransaction)i.next();
							Serializable key = t.getKey();
							Serializable value = t.getValue();
			    
							if (t instanceof MapTransactions.Add) {
								//
								// Take away the old value, if any.
								//
								Object oldValue = advertisers.remove(key);
								if (null != oldValue) {
									Long oldTTL = (Long)_advertiserExpiration.remove(key);
									MapUtil.removeFromMapList(_expirations, oldTTL, key);
								}

								//
								// New value goes in here.
								//
								Long expiration = new Long(now + ((Integer)((MapTransactions.Add)t).getTime()).intValue());
								_advertiserExpiration.put(key, expiration);
								MapUtil.addToMapList(_expirations, expiration, key);
								advertisers.put(key, value);

								DirectoryServerInitializer._LOG.info(new Strings(new Object[] 
									{this, " handle(): added ", key, ":", 
									 value, " expires @", expiration } ));
				
							} else { // Assume it's MapTransactions.Remove
								//
								// Sometimes an update arrives just
								// before an entry expires.  Check the
								// expiration time before removing
								// anything.  If the expiration time
								// of the MapTransaction.Remove isn't
								// the same as the expiration time in
								// _advertiserExpiration, then don't
								// remove anything.  The time in a
								// MapTransaction.Remove will never be
								// greater than the time in
								// _advertiserExpiration.  It will
								// always be equal to or less than.
								//
								Long oldExpiration = (Long)_advertiserExpiration.get(key);
								Long expiration = (Long)((MapTransactions.Remove)t).getTime();

								if (0 == expiration.compareTo(oldExpiration)) {
									Object oldValue = advertisers.remove(key);
									MapUtil.removeFromMapList(_expirations, expiration, key);

									DirectoryServerInitializer._LOG.info(new Strings(new Object[]
										{this, " handle(): removed ", key} ));
								}
							}
						}
							
						authoritativeState.setField(DirectoryFields.DIRECTORY_FIELDNAME, advertisers);
						_transactions.clear();

						// Observe that this directory server doesn't
						// advertise when a new advertisement
						// arrives. Instead, it advertises at regular
						// intervals.  An advertisement persists until
						// the expiration of _advertiseAlarm following
						// the end of the TTL of the advertisement.
						scheduleTTLAlarm(now);

					} catch (NoSuchFieldException ex) {
						// DAVID: Can't happen!
						DirectoryServerInitializer._LOG.fatal(new Strings(new Object[] 
							{this, ": handle(): ", ex}));
					}
				}
			} else if (m == _ttlAlarm) {
				// Make a Remove transaction for each directory entry
				// that expires now.  Reschedule _ttlAlarm
				Long time = null;

				while ((_expirations.size() > 0) && 
					   ((time = (Long)_expirations.firstKey()).intValue() <= now)) {
					List contacts = (List)_expirations.get(time);
					
					for (Iterator i = contacts.iterator(); i.hasNext(); ) {
						ServerContact key = (ServerContact)i.next();
						_transactions.remove(key, time);

						DirectoryServerInitializer._LOG.info(new Strings(new Object[] 
							{this, " handle(): expired ", key, " @", time} ));
					}

					_expirations.remove(time);
				}

				scheduleTTLAlarm(now);
			} else super.handle(m);
		}
    
		/**
		   Schedule the non-recurring {@link #_ttlAlarm} for remainder
		   of the life of the next-expiring advertisement in the
		   directory.  Do nothing if there are no directory entries.
		*/
		private void scheduleTTLAlarm(long now) {
			if (_expirations.size() > 0) {
				Long ttl = (Long)_expirations.firstKey();

				if (null != _ttlAlarm)
					_ttlAlarm.disable();

				// Since _ttl is keyed on expiration time, we
				// already know when (i.e. watch time) the new
				// _ttlAlarm will expire.  We have to calculate
				// the interval between now and then because
				// Clock.Alarm deals with intervals of time, not
				// watch time.
				Entity e = getEntity();
				Clock c = e.getClock();

				// Hope that there is no loss of precision in the cast
				// from long to int, below. Since TTLs are ints and
				// the keys to _expirations are formed from TTLs + the time
				// now (long), the difference should fit in an int.
				_ttlAlarm = c.setAlarm((int)(ttl.longValue() - now), 
									   false, "ttlAlarm" /*null*/, this);
			}
		}
	}

	/**
	   AdvertiserBridge provides the {@link
	   mil.navy.nrl.cmf.sousa.directory.Advertiser} {@link
	   mil.navy.nrl.cmf.sousa.QoS} to clients.  It implements the
	   <CODE>Bridge</CODE> design pattern. It decouples the
	   <CODE>Advertiser</CODE> abstraction from the implementation of
	   <CODE>Advertiser</CODE>.
	   <P>

	   <B>Usage and implementation note:</B> Because of the way that
	   the SOUSA framework discovers <CODE>CommandLogics</CODE>, using
	   an instance of <CODE>AdvertiserBridge</CODE> instead of an
	   instance of <CODE>DirectoryServer_ControlLogic</CODE> as an
	   argument to {@link #addCommandLogic(CommandLogic)} permits the
	   hiding of the other interfaces that
	   <CODE>DirectoryServer_ControlLogic</CODE> implements from the
	   outside world.  That's a good thing.  We don't want the methods
	   of {@link mil.navy.nrl.cmf.sousa.Clock.AlarmHandler}, for
	   example, available via RMI.
	*/
	static class AdvertiserBridge
		implements Advertiser, CommandLogic {

		/**
		   The object that implements the Advertiser interface
		 */
		private final Advertiser _implementation;

		/**
		   Constructs an <CODE>AdvertiserBridge</CODE> given an <CODE>Advertiser</CODE>.
		 */
		AdvertiserBridge(Advertiser a) {
			_implementation = a;
		}

		/**
		   Delgates to the implementor of <CODE>Adveriser</CODE>.

		   @param contact the contact address of the client
		   @param description text that briefly describes the information
		   offered by the advertiser
		   @param ttl the length of the advertisement in milliseconds

		   @return whatever the implementor returns
		 */
		public Object advertise(ServerContact contact, String description, 
								Integer ttl)
		{
			return _implementation.advertise(contact, description, ttl);
		}

	}
    // DirectoryServerInitializer

	/**
	   Constructs a DirectoryServerInitializer from <CODE>p</CODE>.

	   @throws EntityInitializer.InitializationException if the
	   super class constructor throws it.
	 */
    public DirectoryServerInitializer(Properties p) 
		throws EntityInitializer.InitializationException
    {
		super(p);
    }

    // IdolInitializer
    
    /**
	   Adds {@link
	   mil.navy.nrl.cmf.sousa.spatiotemporal.QueryFields#DESCRIPTION_FIELDNAME}
	   and {@link
	   mil.navy.nrl.cmf.sousa.directory.DirectoryFields#DIRECTORY_FIELDNAME}
	   to authoritative State.

	   <P>

	   Adds {@link mil.navy.nrl.cmf.sousa.directory.ConsumerViewInterpreter} and 
	   {@link mil.navy.nrl.cmf.sousa.directory.Advertiser} to the 
	   {@link mil.navy.nrl.cmf.sousa.QoS} offerings.

	   <P>

	   Installs the <CODE>Advertiser</CODE> methods of the {@link
	   DirectoryServer_ControlLogic} as a {@link
	   mil.navy.nrl.cmf.sousa.CommandLogic}.

	   @param p the extra properties
    */
    protected void initialize_custom(Properties p) 
		throws EntityInitializer.InitializationException {
	
		super.initialize_custom(p);
	
		//** AuthoritativeState ************************************
		this.getState().addField(QueryFields.DESCRIPTION_FIELDNAME, "Entity Directory");
		this.getState().addField(DirectoryFields.DIRECTORY_FIELDNAME, new HashMap());
		//** AuthoritativeState ************************************

		//** QoS Classes ************************************
		this.addQoSClass(ConsumerViewInterpreter.class);
		this.addQoSClass(Advertiser.class);
		//** QoS Classes ************************************

		//** CommandLogics ************************************

		// Because the ControlLogic is wrapped in an AdvertiserBridge,
		// only the Advertiser methods of the ControlLogic are exposed
		// for RMI.
		this.addCommandLogic(new AdvertiserBridge((Advertiser)getControlLogic()));
		//** CommandLogics ************************************
    }

    // PURPOSE: make the control logic.
    protected ControlLogic initialize_makeControlLogic(Properties p)
		throws EntityInitializer.InitializationException {
		return new DirectoryServer_ControlLogic(p);
    }
}

