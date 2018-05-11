// File: Client.java

package mil.navy.nrl.cmf.sousa.idol.user;

import java.awt.Color;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import mil.navy.nrl.cmf.annotation.*;
import mil.navy.nrl.cmf.sousa.*;
import mil.navy.nrl.cmf.sousa.directory.ConsumerViewInterpreter;
import mil.navy.nrl.cmf.sousa.directory.DirectoryFields;
import mil.navy.nrl.cmf.sousa.idol.IdolInitializer;
import mil.navy.nrl.cmf.sousa.idol.util.*;
import mil.navy.nrl.cmf.sousa.spatiotemporal.*;
import mil.navy.nrl.cmf.sousa.util.StackTrace;
import mil.navy.nrl.cmf.sousa.util.Strings;
import mil.navy.nrl.cmf.stk.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
   ClientInitializer initializes an IDOL client Entity.  This client
   is a user agent, a browser, with a command console, a 
   {@link GUI} and a {@link ZUI}.
*/
public final class ClientInitializer
    extends ConsoleBrowserInitializer
{
	protected static final Logger _LOG = Logger.getLogger(ClientInitializer.class);

	// TODO: Get the time zone from the environment or from a
	// Property.
	/**
	   The time zone.
	   <P>
	   <EM>BUG- It's always UTC.  It should be the local TZ or it should
	   come from a Properties.</EM>
	 */
    private static final TimeZone _TZ = TimeZone.getTimeZone("UTC");

	/**
	   Identifies comments in console input.
	   <P>
	   A comment is any line in which the first non-whitespace
	   character is a '#'.  <code>handleCommand</code> uses <code>_commentPattern</code>.
	*/
    private static final Pattern _commentPattern = Pattern.compile("^\\s*#");


    // ****************************************************************

    /**
       Client_ControlLogic manages the console, {@link GUI}, and {@link ZUI} for a
       browser Entity.  The browser has a zoomable user interface
       (ZUI), a control panel (GUI), and a text command
       console.  Type 'help' in the console to see a summary of all of
       the console commands.
    */
    public static class Client_ControlLogic 
		extends IdolInitializer.Console_ControlLogic
		implements Client, LocalCommandObject, RemoteCommandObject,
			   SetPosition, SetTime
    {
		/**
		   the current location of Client
		*/
		private Vector3d _position = new Vector3d();

		/**
		   the current query radius of the Client
		*/
		private Vector3d _width = new Vector3d(1.0, 1.0, 10000.0);

		/**
		   the current heading, pitch, and roll of the Client

		   BUG: There is no way to set _hpr using the CLI
		*/
		private Vector3d _hpr = new Vector3d();

		/**
		   lower bound of Client's temporal query
		*/
		private Calendar _timeLower = Calendar.getInstance(_TZ);

		/**
		   upper bound of Client's temporal query.
		*/
		private Calendar _timeUpper = Calendar.getInstance(_TZ);

		/**
		   the Client's new location as set by the {@link ZUI}, the {@link GUI}, or
		   the console.
		   <P>

		   In this implementation, <code>_newPosition</code> acts as a queue of
		   depth 1.  It holds the position given to {@link #update(Vector3d, Calendar)}
		   until {@link #handle(Clock.Alarm)} processes it.
		 */
		private Vector3d _newPosition = null;

		/**
		   the Client's new query radius as set by the {@link ZUI}, the {@link GUI}, or
		   the console.
		   <P>

		   In this implementation, <code>_newWidth</code> acts as a queue of
		   depth 1.  It holds the width given to {@link #setWidth(Vector3d)} or 
		   {@link #setWidth(double,double,double)}
		   until {@link #handle(Clock.Alarm)} processes it.
		 */
		private Vector3d _newWidth = null;

		/**
		   the Client's new heading, pitch, and roll as set by the
		   {@link ZUI}, the {@link GUI}, or the console.  <P>

		   In this implementation, <code>_newHPR</code> acts as a queue of
		   depth 1.  It holds the hpr given to 
		   {@link #setPosition(Vector3d, Vector3d, Vector3d)}
		   until {@link #handle(Clock.Alarm)} processes it.
		 */
		private Vector3d _newHPR = null;

		/**
		   The Client's new lower bound of temporal query as set by the
		   ZUI, GUI, or the console.

		   In this implementation, <code>_newTimeLower</code> acts as a
		   queue of depth 1.  It holds the time given to {@link #pushTime(int)},
		   {@link #setTime(Calendar, Calendar)}, or {@link #update(Vector3d, Calendar)}
		   until {@link #handle(Clock.Alarm)} processes it.
		 */
		private Calendar _newTimeLower = null;


		/**
		   The Client's new upper bound of temporal query as set by the
		   {@link ZUI}, {@link GUI}, or the console.

		   In this implementation, <code>_newTimeUpper</code> acts as a
		   queue of depth 1.  It holds the time given to {@link #pushTime(int)},
		   {@link #setTime(Calendar, Calendar)}, or {@link #update(Vector3d, Calendar)}
		   until handle(Clock.Alarm) processes it.
		*/
		private Calendar _newTimeUpper = null;

		/**
		   The name of the {@link
		   mil.navy.nrl.cmf.sousa.spatiotemporal.QueryResultHandle}
		   field that contains the name of a city, coverage, model, or point
		*/
		private final String _mapnameField = "mapname";

		/**
		   The Zoomable User Interface, or ZUI
		*/
		private ZUI _zui = null;

		/**
		   The control panel 
		*/
		private GUI _gui = null;

		/**
		   Text-only output.  This might become the console I/O
		   handler one day.
		 */
		private CLIRenderable _cli;

		/**
		   Prevents concurrent modification of {@link #_position},
		   {@link #_newPosition}, {@link #_width}, {@link #_newWidth},
		   {@link #_hpr}, {@link #_newHPR},
		   {@link #_timeUpper}, {@link #_newTimeUpper}, 
		   {@link #_timeLower}, {@link #_newTimeLower}, 
		   {@link #_timeOfLastUpdate}, and {@link #_elapsedTime}.
		*/
		private final Object _updateLock = new Object();

		/**
		   the time when {@link #handle(Clock.Alarm)} last updated
		   the upper bound and lower bound time fields of the Entity
		   State.  {@link #updateIntervalPassed} limits the rate of
		   time updates to one per {@link #_updateInterval} ms.
		*/
		private long _timeOfLastUpdate = 0L;

		/**
		   the number of milliseconds since
		   {@link #_timeOfLastUpdate}.  It's calculated by
		   {@link #updateIntervalPassed}.
		 */
		private int _elapsedTime = 0;

		/**
		   the minimum number of milliseconds between notifications to
		   the ZUI.  If <code>_updateInterval</code> is less than
		   zero, the ZUI will not be notified.
		*/
		private int _updateInterval = 1000;

		/**
		   the heartbeat of this <code>Client_ControlLogic<code>.  It
		   expires every {@link #_updateInterval}.  On expiry, this
		   <code>Client_ControlLogic</code> sends its clients time,
		   location, and search width updates.

		   @see #handle(Clock.Alarm)
		 */
		private Clock.Alarm _alarm = null;

		/**
		   maps {@link mil.navy.nrl.cmf.sousa.Receptor} into 
		   {@link mil.navy.nrl.cmf.sousa.FieldListener}.
		   <P>
		   For each <code>Receptor</code> there is a <code>FieldListener</code> 
		   that monitors
		   changes in the <code>Receptor</code>'s State.
		   <P>
		   {@link #receptorStateChangeIndication(Receptor)} consults
		   <code>_receptorStateListeners</code> to obtain the State
		   changes.
		   <P>
		   {@link #receptorReadyIndication(ClientSideFSM)} uses 
		   {@link #addListener(Receptor)} to insert a
		   new FieldListener into 
		   <code>_receptorStateListeners</code>.
		   <P>
		   {@link #receptorNotReadyIndication(ClientSideFSM)} uses 
		   {@link #removeListener(Receptor)} to
		   remove a <code>FieldListener</code> from <code>_receptorStateListeners</code>.
		*/
		private final HashMap _receptorStateListeners = new HashMap();

		/**
		   Keys are ServerContact.  Values are Receptors.  _receptors
		   enables Client_ControlLogic to implement the
		   RemoteCommandObject interface.
		 */
		private final HashMap _receptors = new HashMap();

		private final CommandQueue _commands;
		private final Thread _commandsThread;

		/*
		  maps Receptor into ServerSI.
		  Caches the proxies of each Annotation Server.

		  DAVID: This could be generalized to contain proxies for
		  every <Receptor, Class> pair.

		  _annotationProxies exists because I can't create a proxy
		  from a Receptor.  Instead, I have to create them from
		  ClientSideFSMs.  I don't keep ClientSideFSMs around.  I do
		  keep Receptors around.
		*/
		private final HashMap _annotationProxies = new HashMap();

		/**
		   The RFC2045 MIME Content-Types and their Renderer Classes.
		   <P>
		   The keys are Strings and the values are Classes.  Each
		   Content-Type has a corresponding kind of Renderer that
		   knows how to place it in the scene graph.
		*/
		private final HashMap _pluginRenderers = new HashMap();

		// String-->LinkedList of Renderable
		// Keys are RFC2045 MIME ContentType.
		private final Map _renderables = new HashMap();


		// Address-->Map:QoS-->List of String
		private final Map _pendingFetches = 
			new TreeMap(new ServerContactWithContentTypeComparator());

		// Contact info for the directory service, if there is one.
		// It's obtained from Properties by the constructor.
		// The fetch process begins in setEntity().
		private final String _directoryAddressString;
		private final String _directoryPortString;

		private final static class ServerContactWithContentTypeComparator 
			implements Comparator
		{
			// Use ServerContact.compareTo().
			// If the two Objects are equal, then compare the ContentTypes.
			// Special case when a ContentType is null:
			//   0. both null --> 0
			//   1. Left null, right non-null --> -1
			//   2. Left non-null, right null --> 1

			public int compare(Object o1, Object o2)
			{
				ServerContact s1 = (ServerContact)o1;
				ServerContact s2 = (ServerContact)o2;
				int answer = s1.compareTo(s2);
			
				if (0 == answer) {
					String contentType1 = s1.getQoS().getContentType();
					String contentType2 = s2.getQoS().getContentType();

					if (null != contentType1) {
						// Let String.compareTo() deal with null ==
						// contentType2.
						answer = contentType1.compareTo(contentType2);
					} else if (null != contentType2) {
						// null is always less than non-null
						answer = -1;
					} // else both are null, so o1 and o2 are equal.
				}

				return answer;
			}

			// For equals(), use Object equals().  Good enough.
		}

		// Constructors

		/**
		   Class constructor that uses the value of the property
		   <code>idol.initializer.zui</code> to determine if there is
		   a ZUI and <code>idol.initializer.gui</code>to determine if
		   there is a GUI.  For each, a value of "on" means there is
		   one.  Any other value means there is not one.

		   <P>
		   See the {@link GUI} and {@link ZUI} documentation to
		   learn how they initialize themselves from the properties.

		   @param p the Properties of this ClientInitializer.Client_ControlLogic
		*/
		Client_ControlLogic(Properties p)
		{
			super(p);

			LinkedList routes = new LinkedList();

			// DAVID: Don't modify routes after you call
			// _commandsThread.start().  At best, the
			// additions will be ignored.  At worst, there
			// will be a ConcurrentModificationException.
			_commands = new CommandQueue(routes, this);
			routes.add(this);

			// Directory contact info. If there is a directory
			// service, fetch a Receptor from it in setEntity().
			_directoryAddressString = p.getProperty("idol.directory.address");
			_directoryPortString = p.getProperty("idol.directory.contactport");
	
			try {
				if ("on".equals(p.getProperty("idol.initializer.zui", "off"))) {
					_zui = new ZUI(p, _commands);

					Set types = _zui.getContentTypes();
					for (Iterator i=types.iterator(); i.hasNext(); ) {
						String contentType = (String)i.next();
						LinkedList ll = new LinkedList();
						ll.add(_zui);

						ClientInitializer._LOG.debug(new Strings(new Object[] 
							{"Renderable for ", contentType, " is ", _zui}));

						_renderables.put(contentType, ll);
					}

					routes.add(_zui);
					_zui.start();
				}
			}
			catch (IOException ex) {
				ClientInitializer._LOG.error(new Strings(new Object[] 
					{"Error constructing ZUI: ", ex}));
				ex.printStackTrace(System.err);
			}

			if ("on".equals(p.getProperty("idol.initializer.gui", "off"))) {
			    _gui = new GUI(p, _commands);

				Map guiRenderables = _gui.getRenderables(); 

				for (Iterator i = guiRenderables.entrySet().iterator();
					 i.hasNext(); ) {
					Map.Entry entry = (Map.Entry)i.next();
					String contentType = (String)entry.getKey();
					List renderables = (List)_renderables.get(contentType);
					List ll = (List)entry.getValue();

					if (null == renderables) {
						renderables = new LinkedList();
						_renderables.put(contentType, renderables);
					}

					renderables.addAll(ll);

					ClientInitializer._LOG.debug(new Strings(new Object[] 
						{"Renderable for ", contentType, " is ", ll}));
				}

				routes.add(_gui);
				_gui.start();
			}

			if ("on".equals(p.getProperty(IdolInitializer.Console_ControlLogic.IDOL_CONSOLE_PROP, "off"))) {
			    _cli = new CLIRenderable(p);

				Set types = _cli.getContentTypes();
				for (Iterator i = types.iterator(); i.hasNext(); ) {
						String contentType = (String)i.next();
						List ll = (List)_renderables.get(contentType);

						if (null == ll) {
							ll = new LinkedList();
							_renderables.put(contentType, ll);
						}

						ll.add(_cli);

						ClientInitializer._LOG.debug(new Strings(new Object[] 
							{"Renderable for ", contentType, " is ", _cli}));
				}
			}

			// DAVID: Don't modify routes after you call
			// _commandsThread.start().  At best, the
			// additions will be ignored.  At worst, there
			// will be a ConcurrentModificationException.
			_commandsThread = new Thread(_commands, "Browser Command Queue");
			_commandsThread.start();

			// Tell everyone where and when I am.
			SetPositionAndTimeCommand setPosTime = 
				new SetPositionAndTimeCommand(_position, _width, _hpr,
											  _timeLower, _timeUpper);
			setPosTime.setSource(this);

			_commands.put(setPosTime);
		}

		// mil.navy.nrl.cmf.sousa.idol.ControlLogic


		protected void setEntity(Entity e) {
			int directoryPort = -1;
			boolean haveDirectory = ((null != _directoryAddressString) && 
									 (null != _directoryPortString));
			
			super.setEntity(e);

			// If I have a non-null directoryAddressString and a non-null
			// directoryPortString and directoryPortString represents a
			// valid integer, I'll schedule a fetch of a
			// ClientViewInterpreter from the Directory server.  
			//
			if (haveDirectory) {
				try {
					directoryPort = Integer.parseInt(_directoryPortString);

					// The Directory Server must provide the
					// ConsumerViewInterpreter interface.  Use the default
					// QoS constructor to get a shared session.  For now,
					// sharing is good.
					//
					// TODO: Revisit sharing when the Directory server is
					// spatiotemporally aware.  If it gives answers based
					// on my location and time, then I shouldn't ask for a
					// shared session.

					scheduleFetch(_directoryAddressString, directoryPort, 0,
								  "mil.navy.nrl.cmf.sousa.directory.ConsumerViewInterpreter",
								  "x-idol/x-directory");

					ClientInitializer._LOG.warn(new Strings(new Object[] 
						{"Using Directory ", _directoryAddressString, ":", 
						 new Integer(directoryPort)}));
				} 
				catch (NumberFormatException ex) {
					ClientInitializer._LOG.error(new Strings(new Object[] 
						{"Error initializing directory port", ex}));
				}
				catch (SecurityException ex) {
					ClientInitializer._LOG.error(new Strings(new Object[] 
						{"Error initializing directory address", ex}));
				}
			} else {
				ClientInitializer._LOG.warn("Continuing without Directory.");
			}
		}


		/**
		   In addition to the actions of the base class method:
		   <UL>
		   <LI>Adds a listener to the State of each new <code>ClientSideFSM</code>.
		   <LI>Starts {@link #_alarm}, the heartbeat.
		   </UL>

		   @param fsm the new client
		   @see #handle(Clock.Alarm)
		 */
		public final void receptorReadyIndication(ClientSideFSM fsm) 
		{
			Receptor r = fsm.getReceptor();
			addListener(r);

			// This is the best place to initialize _alarm.  It can't
			// be done in the ctor because there isn't an Entity at
			// that time.  It could be done by the creator of the
			// Initializer but that would require fattening the
			// ControlLogic interface-- public void setClock(); or 
			// public void entityIsReady(); or some such.  Ick.
			//
			if (null == _alarm) {
				Clock c = getEntity().getClock();
				_alarm = c.setAlarm(_updateInterval /* period in milliseconds */, 
									true /* recurring or not */, 
									"client heartbeat" /* user data for AlarmHandler.handle() */,
									this /* the Clock.AlarmHandler */);
			}

			QoS qos = r.getQoS();

			// TODO: Don't take the Receptor's word for it.  Verify
			// that the Receptor contains the Renderer we asked for in
			// the FetchRequest.  If it doesn't, correct it.  If there
			// is no Renderer, then make one.

			// Introduce the Renderer, r, to the Renderable, _zui.
			//
			// The Receptor might come with an applet.
			// r.getRenderer() returning non-null is an indication of
			// that.
			//
			// r.getRenderer returning null indicates that the
			// Receptor needs a plug-in.  newRenderer() creates one.
			Renderer renderer = r.getRenderer();
			String requestedContentType = 
				removePendingFetch(fsm.getServerContact());
			String suppliedContentType = qos.getContentType();

			ClientInitializer._LOG.debug(new Strings(new Object[] 
				{this, ": receptorReadyIndication(): Server: ",
				 fsm.getServerContact(),
				 " Requested Content-Type: ", requestedContentType} ));

			
			/*
			 * Enforce the rules of safe plugins and safe applets.
			 */
			if (null == requestedContentType) {
				/* Asked for Content-Type null.  Got anything.
				 *
				 * There had better be nothing in the Receptor.  This
				 * is a very specific case of "Asked for X and Got X",
				 * below.
				 */

				r.setRenderer(null);
			} else {
				if (requestedContentType.equals(suppliedContentType)) {
					/* 
					 * Asked for Content-Type X.  Got X.  
					 *
					 *    Throw away the Renderer if the Content-Type we
					 *    asked for has an entry in _pluginRenderers; make
					 *    a plugin Renderer.  Otherwise use the Renderer
					 *    that came with the Receptor.
					 */
					Class myRendererClassByPolicy = (Class)
						_pluginRenderers.get(requestedContentType);

					if (null != myRendererClassByPolicy) {
						QoS qosCopy = new QoS(qos.getSession(), qos, 
											  requestedContentType);

						ClientInitializer._LOG.debug(new Strings(new Object[]
							{"Using plugin Renderer for ",
							 requestedContentType,
							 " instead of the one supplied by the Receptor"}));

						renderer = newRenderer(qosCopy);
						r.setRenderer(renderer);
					}
				} else {
					/*
					 * Asked for Content-Type X.  Got Y.
					 *
					 *    Throw away the Renderer in the Receptor.
					 *    Use the Renderer in _pluginRenderers.
					 */
					QoS qosCopy = new QoS(qos.getSession(), qos, 
										  requestedContentType);

					ClientInitializer._LOG.debug(new Strings(new Object[]
						{"Content-Type supplied by Server (",
						 suppliedContentType, ") does not match one requested (",
						 requestedContentType,
						 ").  Making the right kind of Renderer."}));

					renderer = newRenderer(qosCopy);
					r.setRenderer(renderer);
				}
			}

			if (null != renderer) {
				List renderables = 
					(List)_renderables.get(requestedContentType);

				ClientInitializer._LOG.debug(new Strings(new Object[] 
					{this, ": receptorReadyIndication(): Renderable(s) for ",
					 requestedContentType, " is(are) ", renderables} ));

				renderer.setRenderables(renderables);
			}

			
			_receptors.put(fsm.getServerContact(), r);

			// Do my own book keeping and whatnot before letting super
			// have a turn.  This ensures that I'm ready for super to
			// call receptorStateChangeIndication().
			super.receptorReadyIndication(fsm);
		}

		/**
		   In addition to the actions of the base class method,
		   <code>receptorNotReadyIndication</code> stops listening to
		   the State of the <code>ClientSideFSM</code>.
		   <P>
		   <EM>BUG - Doesn't remove contents of Directory server's
		   Directory field from the gui.</EM>

		   @param fsm the new client
		 */
		public final void receptorNotReadyIndication(ClientSideFSM fsm) 
		{
			Receptor r = fsm.getReceptor();
			removeListener(r);

			Renderer renderer = r.getRenderer();
			if (null != renderer)
				renderer.setRenderables(new LinkedList());

			_receptors.remove(fsm.getServerContact());

			// Since receptorNotReadyIndication() is the inverse of
			// receptorReadyIndication(), I clean up my local
			// knowledge of fsm before giving super a turn.
			super.receptorNotReadyIndication(fsm);

			// TODO: Remove contents of Directory server's Directory field from
			// _gui.
		}

		// Client_ControlLogic

		/**
		   Inserts a new {@link mil.navy.nrl.cmf.sousa.FieldListener}
		   into the <code>_receptorStateListeners</code> Map.  The
		   <code>FieldListener</code> listens to all <code>Field</code>s in 
		   <code>r</code>'s State.

		   @param r the <code>Receptor</code> to whose state the
		   <code>FieldListener</code> listens
		*/
		private void addListener(Receptor r) 
		{
			State s = r.getState();
			FieldListener listener = new FieldListener(s);

			s.attachFieldListener(s.getFieldNames(), listener);
			_receptorStateListeners.put(r, listener);
		}

		/**
		   Removes a {@link mil.navy.nrl.cmf.sousa.FieldListener}
		   from the <code>_receptorStateListeners</code> Map.

		   @param r the <code>Receptor</code> to whose state the
		   <code>FieldListener</code> listens
		*/
		private void removeListener(Receptor r) 
		{
			_receptorStateListeners.remove(r);
		}

		/**
		   Actions upon the expiration of {@link #_alarm}, the
		   heartbeat.  Commits the changed position, search width, and
		   time boundaries to the Entity's State.
		 */
		public void handle(Clock.Alarm alarm)
		{
			if (alarm == _alarm) {
				boolean positionChanged = false;
				boolean timeChanged = false;
				boolean updateNow = updateIntervalPassed();
				State as = getEntity().getState();

				synchronized (_updateLock) {

					//
					// Be smart about reporting the position!  Don't report
					// the same position.  
					//
					// ZUI always reports the position, even if it hasn't
					// changed.  If Client_ControlLogic reports it without
					// care, it will cause unnecessary traffic on the network
					// and unnecessary work at the servers.
					//
					if ((null != _newPosition) && 
						!_position.equals(_newPosition)) {
						// DAVID: Dardo! You must not change this.
						// The only way to report changes with
						// meaningful old & new values is to create a
						// new instance for each new value.  If you
						// merely change the current value, then the
						// change will not be reported with
						// old.equals(new).

						_position = _newPosition;
						positionChanged |= true;

						try {
							if (ClientInitializer._LOG.isEnabledFor(Level.INFO)) {
									ClientInitializer._LOG.info(new Strings(new Object[] 
										{this, ": handle(): new position ", 
										 _position, " time= ",
										 java.util.Calendar.getInstance().getTime()
										} ));
								}

							as.setField(QueryClientFields.POSITION_FIELDNAME, 
										_position);
						} catch (NoSuchFieldException ex) {
							ClientInitializer._LOG.error(ex);
						}
					}

					_newPosition = null;

					//
					// Smart width reporting for the same reason as smart
					// position reporting.
					//
					if ((null != _newWidth) && !_width.equals(_newWidth)) {
						// DAVID: Dardo! You must not change this.
						// The only way to report changes with
						// meaningful old & new values is to create a
						// new instance for each new value.  If you
						// merely change the current value, then the
						// change will not be reported with
						// old.equals(new).
						_width = _newWidth;
						positionChanged |= true;
			
						try {
							as.setField(QueryClientFields.WIDTH_FIELDNAME, _width);

							ClientInitializer._LOG.info(new Strings(new Object[] 
								{this, ": handle(): new width ",
								 _width
								} ));
						} catch (NoSuchFieldException ex) {
							ClientInitializer._LOG.error(ex);
						}
					}

					_newWidth = null;

					//
					// Smart heading, pitch, and roll reporting for
					// the same reason as smart position reporting.
					//
					if ((null != _newHPR) && !_hpr.equals(_newHPR)) {
						// DAVID: Dardo! You must not change this.
						// The only way to report changes with
						// meaningful old & new values is to create a
						// new instance for each new value.  If you
						// merely change the current value, then the
						// change will not be reported with
						// old.equals(new).

						// Notice that handle() doesn't change any
						// field in as.  That's because there is no
						// authoritative state field for
						// heading-pitch-roll. 
						_hpr = _newHPR;
						// positionChanged |= true;
			
						ClientInitializer._LOG.info(new Strings(new Object[] 
							{this, ": handle(): new HPR ",
							 _hpr
							} ));
					}

					_newHPR = null;

					//
					// Smart time bound reporting for the same reason as
					// smart position reporting.
					//

					// If there is a ZUI, then it drives the time.  If
					// there is no ZUI and time isn't advanced
					// manually, then the only opportunity to advance
					// time is here.
					//
					// Advance the time by the elapsed time.  This
					// keeps _timeLower and _timeUpper in the correct
					// places relative to now.  For example, if the
					// _timeLower were set to a time in the past,
					// advancing it by the elapsed time keeps it there
					// while advancing the clock.
					//

// BUG: If there is no ZUI but there is a GUI, ControLogic never tells
// the GUI the time.
  					if ((null == _zui) && (null == _newTimeLower) && 
  						(null == _newTimeUpper) && updateNow) {
  						pushTime(_elapsedTime);
 					}

					//
					// _newTimeLower and _newTimeUpper will be
					// non-null if the ZUI set the time, if the user
					// manually set the time, or if pushTime() was
					// called in the block above.
					//
					if ((null != _newTimeLower) && 
						!_timeLower.equals(_newTimeLower)) {
						// DAVID: Dardo! You must not change this.
						// The only way to report changes with
						// meaningful old & new values is to create a
						// new instance for each new value.  If you
						// merely change the current value, then the
						// change will not be reported with
						// old.equals(new).
						_timeLower = _newTimeLower;
						timeChanged |= true;

						try {
							as.setField(QueryClientFields.TIMELOWERBOUND_FIELDNAME, _timeLower);
						} catch (NoSuchFieldException ex) {
							ClientInitializer._LOG.error(ex);
						}
					}

					_newTimeLower = null;

					if ((null != _newTimeUpper) && 
						!_timeUpper.equals(_newTimeUpper)) {
						// DAVID: Dardo! You must not change this.
						// The only way to report changes with
						// meaningful old & new values is to create a
						// new instance for each new value.  If you
						// merely change the current value, then the
						// change will not be reported with
						// old.equals(new).
						_timeUpper = _newTimeUpper;
						timeChanged |= true;

						try {
							as.setField(QueryClientFields.TIMEUPPERBOUND_FIELDNAME, _timeUpper);
						} catch (NoSuchFieldException ex) {
							ClientInitializer._LOG.error(ex);
						}
					}

					_newTimeUpper = null;
				}


 				if (null != _zui) {
 					if (updateNow) {
 						synchronized (_zui) {
 							_zui.notify();
 						}
 					}
				}
			}
		}

		/**
		   Determines if enough time has passed to warrant committing
		   changes to the authoritative State.
		   <P>
		   This implementation sets {@link #_timeOfLastUpdate} and {@link #_elapsedTime}.

		   @return true if the update interval has elapsed; returns false otherwise.
		*/
		private boolean updateIntervalPassed() 
		{
			boolean answer = false;
	
			synchronized (_updateLock) {
				long now = System.currentTimeMillis();

				if (_timeOfLastUpdate == 0L) {
					_timeOfLastUpdate = now;
				}

				if (_updateInterval >= 0) {
					_elapsedTime = (int)(now - _timeOfLastUpdate);
					if (_elapsedTime > _updateInterval) {
						answer = true;
						_timeOfLastUpdate = now;
					}
				}
			}

			return answer;
		}

		/**
		   Advance the time by <code>interval</code> milliseconds.
		   Send the time to all listeners.  pushTime is called by
		   {@link #handle(Clock.Alarm)} only.

		   @param interval the number of milliseconds to advance time
		*/
		private void pushTime(int interval)
		{
			synchronized (_updateLock) {
				// DAVID: Dardo! You must not change this.  The only
				// way to report changes with meaningful old & new
				// values is to create a new instance for each new
				// value.  If you merely change the current value,
				// then the change will not be reported with
				// old.equals(new).
				_newTimeLower = Calendar.getInstance(_TZ);
				_newTimeLower.setTime(_timeLower.getTime());
				_newTimeLower.add(Calendar.MILLISECOND, interval);

				_newTimeUpper = Calendar.getInstance(_TZ);
				_newTimeUpper.setTime(_timeUpper.getTime());
				_newTimeUpper.add(Calendar.MILLISECOND, interval);

				if (false) {
				    // We expect only the GUI as a client
				    // of time.  The only reason that any
				    // code in this ControlLogic calls
				    // pushTime is because there is no ZUI
				    // to periodically set the time.
				    SetTimeCommand guiTimeCommand = new SetTimeCommand(_newTimeLower, _newTimeUpper);
				    guiTimeCommand.setSource(this);

				    _commands.put(guiTimeCommand);
				}
			}
		}

		// sousa.idol.user.LocalCommandObject

		// DAVID: So many update() methods!  
		// <EM>TODO: Find the redundant ones and remove them.</EM>

		/**
		   Record the new <code>position</code> and <code>time</code>.
		   These changes do not take effect until processed by 
		   {@link #handle(Clock.Alarm)}.

		   @param position the new location
		   @param time the new time
		*/
		public void update(Vector3d position, Calendar time)
		{
			synchronized (_updateLock) {
				// DAVID: Dardo! You must not change this.  The only
				// way to report changes with meaningful old & new
				// values is to create a new instance for each new
				// value.  If you merely change the current value,
				// then the change will not be reported with
				// old.equals(new).
				// long timeWidth = _timeUpper.getTimeInMillis() -  // TODO
				// _timeLower.getTimeInMillis(); // TODO

				_newPosition = new Vector3d(position);

				_newTimeUpper = Calendar.getInstance(_TZ);
				_newTimeUpper.setTimeInMillis(time.getTimeInMillis());
				// + timeWidth); // TODO
			}
		}

		// sousa.idol.user.SetTime

		/**
		   Record the new <code>time</code>.
		   This change does not take effect until processed by 
		   {@link #handle(Clock.Alarm)}.

		   @param time the new time
		*/
		private void _setTime(Calendar timeLower, Calendar timeUpper)
		{
			synchronized (_updateLock) {
				// DAVID: Dardo! You must not change this.  The only
				// way to report changes with meaningful old & new
				// values is to create a new instance for each new
				// value.  If you merely change the current value,
				// then the change will not be reported with
				// old.equals(new).

				_newTimeLower = timeLower;
				_newTimeUpper = timeUpper;
			}
		}

		/**
		   Record the new <code>time</code>.
		   This change does not take effect until processed by 
		   {@link #handle(Clock.Alarm)}.

		   @param time the new time
		*/
		public void setTime(Calendar timeLower, Calendar timeUpper)
		{
// 			if (null != _zui) {
// 				try {
// 					_zui.settime((double)timeUpper.getTimeInMillis());
// 				} catch (IOException e) {
// 					ClientInitializer._LOG.error(new Strings(new Object[] 
// 						{this, " caught exception ", e, ":",
// 						 StackTrace.formatStackTrace(e)}));
// 				}
//			} else {
				_setTime(timeLower, timeUpper);
//			}
		}


		// sousa.idol.user.SetPosition

		/**
		   Set the position to <code>position</code>, search width to <code>width</code>,
		   and heading, pitch, and roll to <code>hpr</code>.
		   This change does not take effect until processed by 
		   {@link #handle(Clock.Alarm)}.

		   @param position the new position in <longitude, latitude, elevation>
		   @param width the new search radius in <deg. lon., deg. lag., meters elev.>
		   @param hpr the new heading, pitch, and roll, all in degrees.
		*/
		private void _setPosition(Vector3d position, Vector3d width, Vector3d hpr)
		{
			synchronized (_updateLock) {
				// DAVID: Dardo! You must not change this.  The only
				// way to report changes with meaningful old & new
				// values is to create a new instance for each new
				// value.  If you merely change the current value,
				// then the change will not be reported with
				// old.equals(new).
				_newPosition = position;

				// DAVID: It's probably benign to set _newWidth or
				// _newHPR to null here when width or hpr are null.
				_newWidth = width;
				_newHPR = hpr;
			}
		}

		private void _setWidth(Vector3d width)
		{
			synchronized (_updateLock) {
				_newWidth = width;
			}
		}

		/**
		   Set the position to <code>position</code>, search width to <code>width</code>,
		   and heading, pitch, and roll to <code>hpr</code>.
		   This change does not take effect until processed by 
		   {@link #handle(Clock.Alarm)}.

		   @param position the new position in <longitude, latitude, elevation>
		   @param width the new search radius in <deg. lon., deg. lag., meters elev.>
		   @param hpr the new heading, pitch, and roll, all in degrees.
		*/
		public void setPosition(Vector3d position, Vector3d width, Vector3d hpr)
		{
			ClientInitializer._LOG.debug(new Strings(new Object[] 
				{this, ": setPosition( position ", position, 
				 " width ", width, " hpr ", hpr, " )"}));

			_setPosition(position, width, hpr);
		}

		/**
		   Change the rate at which the ZUI is notified to
		   <code>newInterval</code> milliseconds.
		   The ZUI will be notified every
		   updateInterval as long as the updateInterval is at least
		   zero.  Negative updateIntervals turn off ZUI notification.
		   This change takes effect immediately.

		   @param newInterval the new update interval
		*/
		public void setUpdateInterval(int newInterval) 
		{
			synchronized (_updateLock) {
				_updateInterval = newInterval;
			}
		}

		// sousa.idol.user.RemoteCommandObject
		public Receptor getReceptor(ServerContact s) {
			return (Receptor)_receptors.get(s);
		}

		// sousa.idol.Console$CommandHandler

		/**
		   Implements comments and the connect, currenttime,
		   gotolatlon, gotolatlonelev, log4j, quit, settime, update,
		   wait, and width commands.  Delegates all others to the base
		   class.  A comment is any line with '#' as the first
		   non-whitespace character.  The commands, their arguments,
		   and their meanings are

		   <UL>
		   <LI># - comment

		   <LI>connect &lt;serverName&gt; &lt;port&gt; &lt;session&gt;
		   &lt;viewInterpreterClassName&gt; - request a receptor from a
		   server given its name and contact port, a session number,
		   and the fully qualified name of the ViewInterpreter

		   <LI>currenttime - prints the current time in milliseconds
		   since midnight, January 1, 1970.

		   <LI>gotolatlon &lt;latitude&gt; &lt;longitude&gt; - Change the
		   position of the view without changing the elevation.

		   <LI>gotolatlonelev &lt;latitude&gt; &lt;longitude&gt; &lt;elevation&gt; -
		   Change the position of the viewer.

		   <LI>log4j &lt;fully qualified class name&gt;
		   DEBUG|INFO|WARN|ERROR|FATAL - Change the LOG4J logging
		   level of a class.

		   <LI>quit - Stop and exit.

		   <LI>settime [&lt;time in milliseconds since the epoch&gt;] -
		   Without an argument, set the clock to the current time.
		   With an argument, set the clock to some number of
		   milliseconds since midnight, January 1, 1970.

		   <LI>update &lt;milliseconds&gt; - Change the update rate. A rate
		   less than zero turns off updates.

		   <LI>wait &lt;milliseconds&gt; - Don't do anything for a while.

		   <LI>width &lt;latitude&gt; &lt;longitude&gt; &lt;elevation&gt; - Set the
		   radius of the spatial search.

		   </UL>

		 */
		public final void
			handleCommand(String command)
		{
			// A comment is any line in which the first non-whitespace
			// character is a '#'.  Ignore comments.
			Matcher m = _commentPattern.matcher(command);
	
			if (! m.lookingAt()) {
				String[] args = command.split("\\s");

				if ("quit".equals(args[0])) {
					shutdown();
				} else if ("log4j".equals(args[0])) {
					// log4j <fully qualified class name> <log level>
					//
					// Set the level of messages printed by
					// <fully qualified class name> to <log level>
					//
					// <log level> is a String that corresponds to one of the
					// values of log4j's Level class.

					String className = args[1];
					String levelName = args[2];

					try {
						Class classToLog = Class.forName(className);
						Logger logger = Logger.getLogger(classToLog);
						Level levelToLog = Level.toLevel(levelName);
						logger.setLevel(levelToLog);
			
					} catch (ClassNotFoundException ex) {
						ClientInitializer._LOG.error(new Strings(new Object[] 
							{ex, " ", className}));
					}
				} else if ("wait".equals(args[0])) {
					// wait <milliseconds>
					//
					// Don't do anything for <milliseconds>
					
					long interval = Long.parseLong(args[1]);
					try {
						Thread.sleep(interval);
					} catch (InterruptedException ex) {
						// Shouldn't happen!
						ClientInitializer._LOG.error(new Strings(new Object[] 
							{"Caught exception ", ex, ":",
							 StackTrace.formatStackTrace(ex)}));
					}
				} else if ("gotolatlon".equals(args[0])) {
					// gotolatlon <lat> <lon>
					//
					// Change location to (<lat>, <lon>).
					// Except that we store location as (<lon>, <lat>)
					// because we're used to Cartesian coordinates (<x>, <y>).
					//
					// Don't change elevation.

					double lat = Double.parseDouble(args[1]);
					double lon = Double.parseDouble(args[2]);

					setPosition(new Vector3d(lon, lat, _position.z),
								_width, _hpr);
				} else if ("gotolatlonelev".equals(args[0])) {
					// gotolatlonelev <lat> <lon> <elev>
					//
					// Change location to (<lat>, <lon>).
					// Except that we store location as (<lon>, <lat>)
					// because we're used to Cartesian coordinates (<x>, <y>).
					//
					// Change elevation to <elev> meters.

					double lat = Double.parseDouble(args[1]);
					double lon = Double.parseDouble(args[2]);
					double elev = Double.parseDouble(args[3]);

					setPosition(new Vector3d(lon, lat, elev), _width, _hpr);
				} else if ("settime".equals(args[0])) {
					// settime <milliseconds since the epoch>
					//
					// Set the upper bound of the temporal search
					// window to <milliseconds since the epoch> With
					// no argument, set the time to now.

					long time = 0L;
		
					if (args.length > 1) {
						time = Long.parseLong(args[1]);
					} else {
						time = System.currentTimeMillis();
					}
					Calendar cal = Calendar.getInstance(_TZ);
					cal.setTimeInMillis(time);
					setTime(_timeLower, cal);
				} else if ("currenttime".equals(args[0])) {
					// What time is it now?

					System.out.println(System.currentTimeMillis());
			
				} else if ("width".equals(args[0])) {
					// width <lat> <lon> <elev>
					double x = Double.parseDouble(args[2]); // lon
					double y = Double.parseDouble(args[1]); // lat
					double z = Double.parseDouble(args[3]); // elev

					setPosition(_position, new Vector3d(x, y, z), _hpr);
				} else if ("connect".equals(args[0])) {
					// connect <serverName> <port> <session> <viewInterpClassName> [<contentType>]
					String serverName = args[1];
					int port = Integer.parseInt(args[2]);
					int session = Integer.parseInt(args[3]);
					String viewInterpClassName = args[4];
					String contentType = null;
					if (args.length > 5) contentType = args[5];

					scheduleFetch(serverName, port, session, viewInterpClassName,
								  contentType);

				} else if ("update".equals(args[0])) {
					int newInterval = Integer.parseInt(args[1]);

					setUpdateInterval(newInterval);
				} else super.handleCommand(command);
			} else super.handleCommand(command);
		}

		/**
		   Prints helpful text for the console commands that
		   <code>Client_ControlLogic</code> adds to the commands
		   offered by the base class.
		   <P>
		   <EM>BUG: This implemenation ignores <code>command</code>.</EM>

		   @param command ignored by this implementation
		 */
		protected void help(String command) {
			super.help(command);

			System.out.println("Client commands:");			
			System.out.println("# comment");
			System.out.println("connect <serverName> <port> <session> <viewInterpClassName> [<contentType>]");
			System.out.println("currenttime");
			System.out.println("gotolatlon <lat> <lon>");
			System.out.println("gotolatlonelev <lat> <lon> <elev>");
			System.out.println("log4j <fully qualified class name> DEBUG|INFO|WARN|ERROR|FATAL");
			System.out.println("quit");
			System.out.println("settime [<time in milliseconds since the epoch>]");
			System.out.println("update <milliseconds> (-1 turns off updates)");
			System.out.println("wait <milliseconds>");
			System.out.println("width <lat> <lon> <elev>");
		}

		// sousa.idol.user.ZUI$QueryHandler

		/**
		   Interprets a query from the ZUI as a change in position and time.

		   @param tstamp the time at which the ZUI issued the query
		   @param position the new position
		   @param time the new time
		 */
		public final void
			handleQuery(long tstamp, Vector3d position, Calendar time)
		{
			update(position, time);
		}

		// Client_ControlLogic

		/**
		   Remove the {@link ZUI} and the {@link GUI} from the display
		   and stop the Entity.

		   <P>
		   <EM>BUG: Doesn't make the JVM exit.</EM>
		 */
		public void shutdown() 
		{
			ClientInitializer._LOG.error("Somebody called shutdown()!");
// 			if (null != _zui) {
// 				try {
// 					_zui.shutdown();
// 					ClientInitializer._LOG.error("ZUI shut down");
// 				} catch (IOException e) {
// 					ClientInitializer._LOG.error(new Strings(new Object[]
// 						{this, " caught exception ", e, ":",
// 						 StackTrace.formatStackTrace(e)}));
// 				}
// 			}

// 			if (null != _gui) {
// 				_gui.shutdown();
// 				ClientInitializer._LOG.error("GUI shut down");
// 			}

			quit("Client_ControlLogic");


			synchronized (this) {
				notify();
				Entity e = getEntity();
				if (null != e) e.stop();
				ClientInitializer._LOG.error("Entity stop()");
			}

			ClientInitializer._LOG.error("Client_ControlLogic shutdown");
		}

		public void scheduleFetch(ServerContact s, int session) {
			ClientInitializer._LOG.debug(new Strings(new Object[] 
				{this, ": scheduleFetch(", s, ", ", new Integer(session)}));
 
			QoS fqos = new QoS(session, 
							   // TODO: Use the QoS in s.
							   Collections.singleton(QueryViewInterpreter.class), 
							   // TODO: If there is more than one
							   // ContentType, choose the right one.
							   s.getQoS().getContentType());
 
			ClientInitializer._LOG.debug(new Strings(new Object[] 
				{this, ": scheduleFetch(): fetching qos ", fqos}));
 
			// DAVID: Consider making a ctor ServerContact(ServerContact, QoS)
			try {
				ServerContact newContact = 
					new ServerContact(s.getHost(), s.getPort(), fqos);
 
				addPendingFetch(newContact);
 
				getEntity().scheduleConnectTo(newContact);
			} catch (UnknownHostException ex) {
				ClientInitializer._LOG.error(ex);
			}
		}

		/**
		   Ask the Entity to fetch a Receptor from <code>server</code>.

		   @param server the name or address of the server
		   @param port the server's contact port
		   @param session the session number
		   @param viewInterpreterClassName the fully qualified name of
		   the class of the {@link mil.navy.nrl.cmf.sousa.ViewInterpreter} 
		   to install in the server
		   @param contentType MIME Content-Type for rendering
		*/
		void
			scheduleFetch(String server, int port, int session, 
						  String viewInterpreterClassName,
						  String contentType) 
		{
			// Forward QoS (as requested by client)
			try {
				Class viewInterpreterClass = 
					Class.forName(viewInterpreterClassName);
				QoS fqos = new QoS(session, 
								   Collections.singleton(viewInterpreterClass),
								   contentType);

				// Convert String server into InetAddress serverInet
				InetAddress serverInet = InetAddress.getByName(server);
				ServerContact serverAddress = 
					new ServerContact(serverInet, port, fqos);

				addPendingFetch(serverAddress);
				//
				// DAVID: If there is a GUI, it should schedule the fetch.
				//
				getEntity().scheduleConnectTo(serverAddress);
			} catch (ClassNotFoundException ex) {
				ClientInitializer._LOG.error(new Strings(new Object[] 
					{"Unable to find class: ", viewInterpreterClassName}));
			} catch (UnknownHostException ex) {
				ClientInitializer._LOG.error(ex);
			}
		}

		// Utility
		private final Renderer newRenderer(QoS q) {
			Renderer answer = null;
			String contentType = q.getContentType();

			if (null != contentType) {
				Class c = (Class)_pluginRenderers.get(contentType);

				ClientInitializer._LOG.debug(new Strings(new Object[] 
					{"newRenderer(): Qos: ", q,
					 " Renderer: ", c}));
				if (null != c) {
					try {
						answer = (Renderer)c.newInstance();
					} catch (IllegalAccessException ex) {
						ClientInitializer._LOG.warn(new Strings(new Object[] 
							{"newRenderer(): error instantiating ", 
							 c, " : ",
							 ex}));
					} catch (InstantiationException  ex) {
						ClientInitializer._LOG.warn(new Strings(new Object[] 
							{"newRenderer(): error instantiating ", 
							 c, " : ",
							 ex}));
					} catch (ExceptionInInitializerError ex) {
						ClientInitializer._LOG.warn(new Strings(new Object[] 
							{"newRenderer(): error instantiating ", 
							 c, " : ",
							 ex}));
					} catch (SecurityException ex) {
						ClientInitializer._LOG.warn(new Strings(new Object[] 
							{"newRenderer(): error instantiating ", 
							 c, " : ",
							 ex}));
					}
				}
			}

			return answer;
		}

		/**
		   I want a Renderer for some ContentType.  I might not ask
		   for the ContentType in the ServerContact.  I'll put null
		   there instead when I want to use plugin Renderer instead of
		   an applet Renderer.

		  @param address All contact info for the server
		 */
		private void addPendingFetch(ServerContact address) {
			QoS qos = address.getQoS();
			String contentType = qos.getContentType();

			ClientInitializer._LOG.debug(new Strings(new Object[]
				{"Fetching from ", address}));

			_pendingFetches.put(address, contentType);

			// When there is an entry in _pluginRenderers for the
			// Content-Type, remove the Content-Type from the QoS so
			// the server doesn't send back a Renderer.
			if (null != _pluginRenderers.get(contentType)) {
				qos.setContentType(null);
				ClientInitializer._LOG.debug(
				 "Setting Content-Type to null because of Plugin Policy");
			}

			// Remember the Content-Type we asked for.  Consult
			// _pendingFetches when the Receptor arrives to insert the
			// right kind of Renderer.

		}
		   
		/**
		   I requested a Renderer for some ContentType.  The server
		   sent me a Receptor for some QoS with a possibly different
		   ContentType.  Return the ContentType that I asked for.
		   There is no way to tell the difference between no entry for
		   the ServerContact and a null Renderer for Receptors from
		   the ServerContact.
		 */
		private String removePendingFetch(ServerContact address) {
			String answer = (String)_pendingFetches.get(address);
			_pendingFetches.remove(address);

			return answer;
		}

		public void handleMessage(String source, /*@ non_null */ String message) {
			ClientInitializer._LOG.error("SOMEBODY called handleMessage(" + source + ", " + message + ")");
		}

    }; // Client_ControlLogic

    // ****************************************************************


    /**
       ClientInitializer
    */
	// Constructors

	/**
	   Class constructor that uses a Property to determine the initial
	   values of the Entity's communications parameters and State fields.
	*/
    public ClientInitializer(Properties p)
		throws EntityInitializer.InitializationException
	{
		super(p);
	}


	// IdolInitializer

	/**
	   Constructs a {@link Client_ControlLogic} using elements of
	   <code>p</code>.

	   @param p the <code>Properties</code> of the <code>ControlLogic</code>
	   @return a <code>ControlLogic</code>
	 */
	public final ControlLogic
		initialize_makeControlLogic(Properties p)
	{
		ControlLogic cl = new Client_ControlLogic(p);
		return cl;
	}


	// DAVID: This code is nearly identical to
	// idol.service.ServerInitializer.initialize_custom().
    /**
	   Schedule the fetching of a Receptor from a directory server if
	   one is specified in <code>p</code>.  The property
	   <code>idol.directory.address</code> contains the address of the
	   directory server.  The property
	   <code>idol.directory.contactport</code> contains the directory
	   server contact port.  If both are non-null and together they
	   represent a valid host, then <code>initialize_custom</code> requests a
	   {@link mil.navy.nrl.cmf.sousa.directory.ConsumerViewInterpreter}
	   from that server.

	   @param p the special <code>Properties</code> of this ClientInitializer
    */
    protected void initialize_custom(Properties p) 
		throws EntityInitializer.InitializationException {

		this.addQoSClass(SpatiotemporalViewInterpreter.class);

    }
}; // ClientInitializer
