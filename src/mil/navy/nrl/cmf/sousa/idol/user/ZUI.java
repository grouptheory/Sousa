package mil.navy.nrl.cmf.sousa.idol.user;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import mil.navy.nrl.cmf.sousa.Renderable;
import mil.navy.nrl.cmf.sousa.ServerContact;
import mil.navy.nrl.cmf.sousa.idol.util.StreamHandlerThread;
import mil.navy.nrl.cmf.sousa.spatiotemporal.Vector3d;
import mil.navy.nrl.cmf.sousa.util.StackTrace;
import mil.navy.nrl.cmf.sousa.util.Strings;
import mil.navy.nrl.cmf.stk.XYZd;
import org.apache.log4j.*;

/**
   ZUI
*/
final class ZUI
	extends Thread
    implements Renderable, LocalCommandObject
{
	/**
	   _LOG
	*/
	private static final Logger _LOG = Logger.getLogger(ZUI.class);

	/**
	   Part of a hack that assumes that the first image drawn on the
	   scene graph is the base earth image and ensures that the base
	   image is drawn as far from the viewer as possible.  This
	   eliminates flimmer.
	*/
	private boolean _nolayers = true;

	/**
	   Internal message separator
	*/
	static final String SPACE = " ";

	/**
	   _shuttingdown
	*/
	private boolean _shuttingdown = false;

	/*@ non_null */ private final ServerSocket _server;
	private final List _outputStreams = Collections.synchronizedList(new LinkedList());
	/*@ non_null */ private final CommandQueue _commandQueue;

	private class ZUIStreamHandler implements StreamHandlerThread.StreamHandler {
		/**
		   _parsebuf
		*/
		private final StringBuffer _parsebuf = new StringBuffer();	

		/**
		   _pos longitude, latitude, elevation
		*/
		private final Vector3d _pos = new Vector3d();

		/**
		   _width
		   
		   BUG: ZUI must obtain _width from elsewhere.
		*/
		private final Vector3d _width = null;

		/**
		   _hpr heading, pitch, roll
		*/
		private final Vector3d _hpr = new Vector3d();

		/**
		   _t
		*/
		private final Calendar _tmin = 
			Calendar.getInstance(TimeZone.getTimeZone("UTC"));

		private final Calendar _tmax = 
			Calendar.getInstance(TimeZone.getTimeZone("UTC"));

		/**
		   _df
		*/
		private final DateFormat _df = new SHMDateFormat(_tmax);

		// May be null.  If it's not null, handleInput() tells all the
		// other associated output streams about its input.
		private final OutputStream _ostream;

		public ZUIStreamHandler(OutputStream ostream) {
			_ostream = ostream;
		}
		// StreamHandlerThread$StreamHandler
		/**
		   @see StreamHandlerThread$StreamHandler#handleInput(String)
		 */
		public final void
		handleInput(String line)
		{
			ZUI._LOG.debug(line);

			// spaces delimit tokens in line.
			String[] qs = line.split(SPACE);

			// :navigator setlatlonelevhpr <latitude deg> <longitude deg> <elevation meters> <heading deg> <pitch deg> <roll deg>
			if (qs[0].equals(":navigator")) {
				if (qs[1].equals("setlatlonelevhpr")) {
					if (qs.length == 8) {
						try {

							// "Strings2Vector3d"
							_pos.set(Double.parseDouble(qs[3]), // longitude
									 Double.parseDouble(qs[2]), // latitude
									 Double.parseDouble(qs[4])); // elevation

							_hpr.set(Double.parseDouble(qs[5]), // heading
									 Double.parseDouble(qs[6]), // pitch
									 Double.parseDouble(qs[7])); // roll

							// Move my agent
							//
							// DAVID: _width never changes because there no
							// way for the other side to say so.  By leaving
							// _width at its initial value, null, we tell
							// the agent not to change its authoritative 
							// value of search width.
							addCommand(new SetPositionCommand(_pos, _width, _hpr));

							// Move the other clients
							setPosition(_ostream, _pos, _width, _hpr);

						} catch (NumberFormatException e) {
							ZUI._LOG.error(new Strings(new Object[] 
								{this, " caught exception ", e, ":",
								 StackTrace.formatStackTrace(e)}));
						}
					} else {
						ZUI._LOG.error(new Strings(new Object[] 
							{"Bad socket input.  Expecting 8 arguments, got \"", 
							 new Integer(qs.length),
							 "\" in \"", line, "\""}));
					}
				} else {
					ZUI._LOG.error(new Strings(new Object[] 
						{"Bad socket input.  Expecting \"setlatlonelevhpr\", got \"", 
						 qs[1], 
						 "\" in \"", line, "\""}));
				}
			} else {
				ZUI._LOG.error(new Strings(new Object[] 
					{"Bad socket input.  Expecting \":navigator\", got \"", qs[0], 
					 "\" in \"", line, "\""}));
			}
		}


		/**
		   @see StreamHandlerThread$StreamHandler#handleException()
		 */
		public final void
		handleException(IOException exception)
		{
			exception.printStackTrace(System.err);
		}

		/**
		   @see StreamHandlerThread$StreamHandler#handleEOF()
		 */
		public final void
		handleEOF()
		{
			ZUI._LOG.debug("EOF");
		}
	};

	// Constructors

	/**
	   ZUI(Properties, LocalCommandObject)
	   @methodtype ctor
	   @param p .
	   @param commandQueue .
	   @throws IOException .
	*/
	ZUI(/*@ non_null */ Properties p, 
		/*@ non_null */ CommandQueue commandQueue)
		throws IOException
	{
		super("ZUI");

		String zui_exec = p.getProperty("idol.user.zui.exec", "");
		String zui_dir = p.getProperty("idol.user.zui.dir", "");
		String zui_opts = p.getProperty("idol.user.zui.opts", "");

		/*
		this._process = Runtime.getRuntime().exec(zui_exec + " " + zui_opts, 
							  null, new File(zui_dir));
		*/

		this._commandQueue = commandQueue;

		/*
		new StreamHandlerThread("ZUI-INPUT", _process.getInputStream(),
								StreamHandlerThread.INPUTHANDLER).priority(Thread.MIN_PRIORITY).start();
		new StreamHandlerThread("ZUI-ERROR", _process.getErrorStream(),
								StreamHandlerThread.ERRORHANDLER).priority(Thread.MIN_PRIORITY).start();
		*/
		
		_server = new ServerSocket(9999);

		try {
			Thread.sleep(2000L);
		} catch (InterruptedException e) {
			_LOG.error(new Strings(new Object[] 
			    {this, " caught exception ", e, ":", 
			     StackTrace.formatStackTrace(e)}));
		}
	}

	// ****************************************************************
	// ****************************************************************

	/**
	   QueryHandler
	*/
	public interface
		QueryHandler
	{
		/**
		   handleQuery(long, Vector3d, Calendar)
		   @methodtype handler
		   @param tstamp .
		   @param position .
		   @param time .
		*/
		public void
			handleQuery(long tstamp, 
						/*@ non_null */ Vector3d position, 
						/*@ non_null */ Calendar time);
	}; // QueryHandler

	// ****************************************************************

	/**
	   MessageHandler
	*/
	public interface
		MessageHandler
	{
		/**
		   handleMessage(String, String)
		   @methodtype handler
		   @param source .
		   @param message .
		*/
		public void
			handleMessage(String source, /*@ non_null */ String message);
	}; // MessageHandler

	// ****************************************************************
	// ****************************************************************

	/**
	   SHMDateFormat
	*/
	static class SHMDateFormat
		extends SimpleDateFormat
	{
		// Constructors

		/**
		   SHMDateFormat(Calendar)
		   @methodtype ctor
		   @param cal .
		*/
		SHMDateFormat(/*@ non_null */ Calendar cal)
		{
			super("yyyyMMddHHmmssSSS");
			setCalendar(cal);
		}
	}; // SHMDateFormat

	// ****************************************************************
	// ****************************************************************

	// java.lang.Thread

	/**
	   @see Thread#run()
	*/
	public final void
		run()
	{
		long numSockets = -1;

		try {
			Socket newSocket = null;

			while (null != (newSocket = _server.accept())) {
				numSockets ++;				
				OutputStream s = newSocket.getOutputStream();
				new StreamHandlerThread("ZUI-INPUT-" + numSockets, 
										newSocket.getInputStream(),
										new ZUIStreamHandler(s)).priority(Thread.MIN_PRIORITY).start();
				synchronized (_outputStreams) {
					_outputStreams.add(s);
				}

				ZUI._LOG.debug(new Strings(new Object[]
						{this, " accepted connection # ", new Long(numSockets), 
						 ": ", newSocket}));
			}
		} catch (IOException e) {
			_LOG.error(new Strings(new Object[] 
				{this, " caught exception ", e, ":",
				 StackTrace.formatStackTrace(e)}));
		}

		addCommand(new ShutdownCommand());
	}

    // mil.navy.nrl.cmf.idol.user.SetPosition

	/**
	   This setPosition is a message from the agent: tell all clients
	   about the new position.
	 */
    public void setPosition(Vector3d position, Vector3d width, Vector3d hpr) {

		try {
			broadcast(OssimPlanetLanguage.setlatlonelevhpr(":navigator",
														   position.y, 
														   position.x,
														   position.z,
														   hpr.x, 
														   hpr.y, 
														   hpr.z));
			// Tell each of the ZUIStreamHandlers about the
			// new position, width, and hpr.

		} catch (IOException ex) {
			_LOG.error(new Strings(new Object[] 
				{"Error broadcasting lat-lon-elev-hpr:", ex}));
		}
	}

	/**
	   This setPosition is a message from one of the clients: tell the
	   other clients about the new position.
	 */
    public void setPosition(OutputStream sender, Vector3d position, Vector3d width, Vector3d hpr) {
		try {
			broadcast(sender, 
					  OssimPlanetLanguage.setlatlonelevhpr(":navigator",
														   position.y, 
														   position.x,
														   position.z,
														   hpr.x, 
														   hpr.y, 
														   hpr.z));
		} catch (IOException ex) {
			_LOG.error(new Strings(new Object[] 
				{"Error broadcasting lat-lon-elev-hpr:", ex}));
		}
    }

    // mil.navy.nrl.cmf.idol.user.SetTime

    public void setTime(Calendar timeLowerBound, Calendar timeUpperBound) {
		// Viewer's notion of time is seconds since the epoch.
		String message = ": settime " + (timeUpperBound.getTimeInMillis() / 
										 1000.0);
		try {
			broadcast(message);
			//		_tMin.setDate(timeLowerBound.getDate());
			//		_tMax.setDate(timeUpperBound.getDate());
		} catch (IOException ex) {
			_LOG.error(new Strings(new Object[] {message, ": ", ex}));
		}
    }

    // mil.navy.nrl.cmf.idol.user.LocalCommandObject
    public void scheduleFetch(ServerContact s, int session) {};

    public void setUpdateInterval(int newInterval) {};

    /**
       shutdown()
       @methodtype command
    */
    public final void shutdown()
    {
		String message = ":idolbridge quit";
		_shuttingdown = true;

		try {
			broadcast(message);
		} catch (IOException ex) {
			_LOG.error(new Strings(new Object[] {message, ": ", ex}));
		}
    }


    // mil.navy.nrl.cmf.idol.user.ZUI

    public final void addCommand(Command c) {
		if (RoutableCommand.class.isAssignableFrom(c.getClass())) {
			((RoutableCommand)c).setSource(this);
		}

		_LOG.debug(new Strings(new Object[]
			{"Adding command ", c}));

		_commandQueue.put(c);
    }

    /**
	   settimerate(double)
	   @methodtype command
	   @param timerate .
	   @throws IOException .
	*/
	final void
		settimerate(double timerate)
		throws IOException
	{
		broadcast(": settimerate " + timerate);
	}


	// mil.navy.nrl.cmf.sousa.Renderer
	final public void
		loaddynamicmodel(String layername, String name, String filename, 
						 double scale, XYZd position)
		throws IllegalArgumentException, IOException
	{
		broadcast(OssimPlanetLanguage.addPoint(":idolbridge",
											   OssimPlanetLanguage.layername(layername),
											   OssimPlanetLanguage.id(name),
											   OssimPlanetLanguage.position(position.getY(),
																			position.getX(), 
																			position.getZ(),
																			"relative"),
											   OssimPlanetLanguage.model(filename,
																		 /* orientation */ null, 
																		 OssimPlanetLanguage.scale(scale, 
																								   scale, 
																								   scale)),
											   null, null));
	}

	final public void
		loadfluxedmodel(String layername, String name, String filename, 
						double scale, double time, XYZd position, XYZd velocity)
		throws IOException
	{
		broadcast(OssimPlanetLanguage.addPoint(":idolbridge",
											   OssimPlanetLanguage.layername(layername),
											   OssimPlanetLanguage.id(name),
											   OssimPlanetLanguage.position(position.getY(),
																			position.getX(), 
																			position.getZ(),
																			"relative"),
											   OssimPlanetLanguage.model(filename,
																		 /* orientation */ null, 
																		 OssimPlanetLanguage.scale(scale, 
																								   scale, 
																								   scale)),
											   null, 
											   OssimPlanetLanguage.velocity(time,
																			velocity.getY(),
																			velocity.getX(),
																			velocity.getZ())
											   ));
	}

	final public void
		loadstatictext(String layername, String name, String text, Color color,
					   double scale, XYZd position)
		throws IOException
	{
		broadcast(OssimPlanetLanguage.addPoint(":idolbridge",
											   OssimPlanetLanguage.layername(layername),
											   OssimPlanetLanguage.id(name),
											   OssimPlanetLanguage.position(position.getY(),
																			position.getX(), 
																			position.getZ(),
																			"relative"),
											   OssimPlanetLanguage.text(text,
																		OssimPlanetLanguage.scale(scale, 
																								  scale, 
																								  scale),
																		OssimPlanetLanguage.color((double)(color.getRed()/255.0),
																								  (double)(color.getGreen()/255.0), 
																								  (double)(color.getBlue()/255.0),
																								  (double)(color.getAlpha()/255.0))),
											   null, null));
	}

	final public void
		updatedynamicobject(String layername, String name, XYZd position)
		throws IllegalArgumentException, IOException
	{
		broadcast(OssimPlanetLanguage.movePoint(":idolbridge",
												OssimPlanetLanguage.layername(layername),
												OssimPlanetLanguage.id(name),
												OssimPlanetLanguage.position(position.getY(),
																			 position.getX(), 
																			 position.getZ(),
																			 "relative"),
												null));
	}

	final public void
		updatefluxedobject(String layername, String name, double time, 
						   XYZd position, XYZd velocity)
		throws IllegalArgumentException, IOException
	{
		broadcast(OssimPlanetLanguage.movePoint(":idolbridge",
												OssimPlanetLanguage.layername(layername),
												OssimPlanetLanguage.id(name),
												OssimPlanetLanguage.position(position.getY(),
																			 position.getX(), 
																			 position.getZ(),
																			 "relative"),
												OssimPlanetLanguage.velocity(time,
																			 velocity.getY(),
																			 velocity.getX(),
																			 velocity.getZ())
												));
	}

	final public void
		loadpatch(String layername, String name, String filename, 
				  int displacement)
		throws IllegalArgumentException, IOException
	{
		broadcast(OssimPlanetLanguage.addImage(":idolbridge",
											   OssimPlanetLanguage.layername(layername),
											   OssimPlanetLanguage.id(name),
											   OssimPlanetLanguage.image(filename),
											   OssimPlanetLanguage.description(filename)));
	}

	final public void
		unloadSceneGraphObject(String layername, String name)
		throws IllegalArgumentException, IOException
	{
		broadcast(OssimPlanetLanguage.removeObject(":idolbridge",
												   OssimPlanetLanguage.layername(layername),
												   OssimPlanetLanguage.id(name)));
	}

	public void 
		loadObject(String layername, Object obj)
		throws IOException {}

	public void 
		unloadObject(String layername, Object obj)
		throws IOException {}


	/**
	   deleteLayer(String, String)
	   @methodtype command
	   @param layername .
	   @throws IOException .
	*/
	final public void
		deleteLayer(String layername)
		throws IOException
	{
		broadcast(OssimPlanetLanguage.removeLayer(":idolbridge", 
												  OssimPlanetLanguage.layername(layername)));
	}

	final public Set getContentTypes() {
		Set answer = new HashSet();
		answer.add("x-idol/x-city");
		answer.add("x-idol/x-coverage");
		answer.add("x-idol/x-model");
		answer.add("x-idol/x-point");

		return answer;
	}
	// Utility

	/**
	   broadcast(String)

	   Send the message to all OutputStreams.

	   @methodtype command
	   @param message .
	   @throws IOException .
	*/
	private void
		broadcast(String message)
		throws IOException
	{
		// DAVID: This and broadcast(OutputStream, String) are candidates for
		// refactoring.

		_LOG.debug(new Strings(new Object[] 
		    {"broadcast(\"", message, "\")"}));

		synchronized (_outputStreams) {
			for (Iterator i = _outputStreams.iterator(); i.hasNext(); ) {
				OutputStream s = (OutputStream)i.next();
				try {
					_LOG.debug(new Strings(new Object[] 
						{"broadcast(", s, ", \"", message, "\")"}));

					sendToStream(s, message);
				} catch (IOException ex) {
					_LOG.error(new Strings(new Object[] {message, ": ", ex}));
					_LOG.error(new Strings(new Object[] 
						{"Removing connection to ", s }));
					i.remove();
				}
			}
		}
	}

	/**
	   broadcast(OutputStream, String)
	   
	   Send the message to all OutputStreams but the sender.

	   @methodtype command
	   @param sender the source of the message
	   @param message .
	   @throws IOException .
	*/
	private void
		broadcast(OutputStream sender, String message)
		throws IOException
	{
		// DAVID: This and broadcast(String) are candidates for
		// refactoring.
		_LOG.debug(new Strings(new Object[] 
		    {"broadcast(", sender, ", \"", message, "\")"}));

		synchronized (_outputStreams) {
			for (Iterator i = _outputStreams.iterator(); i.hasNext(); ) {
				OutputStream s = (OutputStream)i.next();
				if (! s.equals(sender)) {
					try {
						sendToStream(s, message);
					} catch (IOException ex) {
						_LOG.error(new Strings(new Object[] {message, ": ", ex}));
						i.remove();
					}
				}
			}
		}
	}

	static private final byte[] _TERMINATOR = new byte[] {(byte)10};

	private final void sendToStream(OutputStream s, String message) 
		throws IOException
	{
		s.write((message).getBytes());
		s.write(ZUI._TERMINATOR);
	}

}; // ZUI
