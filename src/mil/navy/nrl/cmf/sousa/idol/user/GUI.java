package mil.navy.nrl.cmf.sousa.idol.user;

import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.io.IOException;
import java.security.AccessController;
import java.security.Principal;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Properties;
import java.util.TimeZone;
import javax.security.auth.Subject;
import javax.swing.*;
import mil.navy.nrl.cmf.sousa.Renderable;
import mil.navy.nrl.cmf.sousa.ServerContact;
import mil.navy.nrl.cmf.sousa.spatiotemporal.Vector3d;
import mil.navy.nrl.cmf.sousa.util.Strings;
import org.apache.log4j.*;

/**
   GUI
 */
final class GUI implements LocalCommandObject
{
	public static final String PANEL_PROP_PREFIX = "idol.initializer.gui.panel";
	public static final String PANEL_SIZE_PROP = PANEL_PROP_PREFIX + ".size";
	public static final String PANEL_ENTRY_PREFIX = PANEL_PROP_PREFIX + ".entry.";
	public static final String PANEL_NAME_SUFFIX = ".name";
	public static final String PANEL_CLASS_SUFFIX = ".class";

	/**
	   _LOG
	 */
	private static final Logger _LOG = Logger.getLogger(GUI.class);

	/**
	   _frame
	 */
	/*@ non_null */ private final JFrame _frame;


	private final HashSet _setPositions = new HashSet();
	private final HashSet _setTimes = new HashSet();

	 /*@ non_null */ private final JLabel _principal;

	 private final JTabbedPane _tabs = new JTabbedPane();

	 private final CommandQueue _commandQueue;

 // Constructors

 /**
	GUI(Properties, User)
	@methodtype ctor
	@param p .
	@param u .
  */
 GUI(/*@ non_null */ Properties p, /*@ non_null */ CommandQueue q )
 {
	 this._commandQueue = q;

	 int display = Integer.parseInt(p.getProperty("idol.user.gui.display", "0"));

	 this._frame = new JFrame("IDOL",
		 GraphicsEnvironment.getLocalGraphicsEnvironment()
			 .getScreenDevices()[display]
			 .getDefaultConfiguration());

	 JPanel cp = (JPanel)_frame.getContentPane();

	 // Also populates _setTimes and _setPositions.
	 loadTabs(p);

	 JPanel principalp = new JPanel();
	 String pname = "nobody";
	 Subject subject = Subject.getSubject(AccessController.getContext());
	 if (null != subject) {

		 pname = ((Principal)subject.getPrincipals().iterator().next()).getName();
	 }

	 this._principal = new JLabel(pname);	
	 principalp.add(this._principal);

	 cp.add(_tabs, BorderLayout.CENTER);
	 cp.add(principalp, BorderLayout.SOUTH);

	 _frame.addWindowListener(new WindowAdapter()
	     {
		 public final void windowClosing(WindowEvent event)
		 {
		     addCommand(new ShutdownCommand());
		 }
	     });
 }

	/*
	  Creates the JPanel tabs from Properties.
	  Populates _setTimes and _setPositions Sets.
	 */
	 private void loadTabs(Properties p) {
		 String s = p.getProperty(PANEL_SIZE_PROP, "0");

		 try {
			 Class[] ctorArgs = 
				 new Class[] {this.getClass()};

			 int size = Integer.parseInt(s);

			 for (int i=0; i < size; i++) {
				 String prefix = PANEL_ENTRY_PREFIX + i;
				 String name = p.getProperty(prefix + PANEL_NAME_SUFFIX, "Panel " + i);
				 String panelClassName = p.getProperty(prefix + PANEL_CLASS_SUFFIX);

				 if (null != panelClassName) {
					 _LOG.debug(new Strings(new Object[] 
						 {prefix, PANEL_NAME_SUFFIX, "=", name}));

					 _LOG.debug(new Strings(new Object[] 
						 {prefix, PANEL_CLASS_SUFFIX, "=", panelClassName}));

					 try {
						 Class cls = Class.forName(panelClassName); 
						 Constructor ctor = cls.getConstructor(ctorArgs);
						 JPanel panel = (JPanel)ctor.newInstance(new Object[]{this});
						 _tabs.addTab(name, panel);
					 
						 if (Renderable.class.isAssignableFrom(cls)) {
							 _LOG.debug(new Strings(new Object[] 
								 {cls, " is Renderable for ", 
								  ((Renderable)panel).getContentTypes()}));
						 } else {
							 _LOG.debug(new Strings(new Object[] 
								 {cls, " is not a Renderable"}));
						 }

						 if (panel instanceof SetPosition) {
							 _setPositions.add(panel);
						 }

						 if (panel instanceof SetTime) {
							 _setTimes.add(panel);
						 }

					 } catch (ClassNotFoundException ex) {// Class.forName()
						 _LOG.error("Initializing GUI panels: " + prefix + " " + ex);
					 } catch (NoSuchMethodException ex) {
						 _LOG.error("Initializing GUI panels: " + prefix + " " + ex);
					 } catch (IllegalAccessException ex) {
						 _LOG.error("Initializing GUI panels: " + prefix + " " + ex);
					 } catch (IllegalArgumentException ex) {
						 _LOG.error("Initializing GUI panels: " + prefix + " " + ex);
					 } catch (InstantiationException ex) {
						 _LOG.error("Initializing GUI panels: " + prefix + " " + ex);
					 } catch (InvocationTargetException ex) {
						 _LOG.error("Initializing GUI panels: " + prefix + " " + ex);
					 } catch (ExceptionInInitializerError ex) {
						 _LOG.error("Initializing GUI panels: " + prefix + " " + ex);
					 }
				 } else {
					 _LOG.error("Initializing GUI panels: " +
								prefix + " must have a " +
								PANEL_CLASS_SUFFIX);

				 }
			 }
		 } catch (NumberFormatException ex) {
			 _LOG.error("Initializing GUI panels: " +
						PANEL_SIZE_PROP + 
						" must be an integer, not \"" + s + "\"");
		 }
	 }

	/**
	   Returns a Map from RFC2045 Content-Type String to List of
	   Renderables into which the Content-Type can be drawn by a
	   Renderer.  The List must not be null.
	 */
	 public Map getRenderables() {
		 Map answer = new HashMap();

		 // Find the Renderable Components in _tabs.
		 for (int i=0; i < _tabs.getTabCount(); i++) {
			 Component c = _tabs.getComponentAt(i);

			 if (Renderable.class.isAssignableFrom(c.getClass())) {
				 // Add each of the Content-Types for this Renderable.
				 Set types = ((Renderable)c).getContentTypes();

				 for (Iterator j=types.iterator(); j.hasNext(); ) {
					 String contentType = (String)j.next();

					 List renderables = (List)answer.get(contentType);
					 if (null == renderables) {
						 renderables = new LinkedList();
						 answer.put(contentType, renderables);
					 }

					 renderables.add(c);
				}
			}
		}
		
		return answer;
	}
// ****************************************************************

// mil.navy.nrl.cmf.idol.user.GUI

/**
   start()
   @methodtype command
 */
final void
start()
{
	_frame.pack();
	_frame.show();
}

public final void addCommand(Command c) {
    if (RoutableCommand.class.isAssignableFrom(c.getClass())) {
	((RoutableCommand)c).setSource(this);
    }

    _commandQueue.put(c);
}

// mil.navy.nrl.cmf.sousa.idol.user.LocalCommandObject
public void scheduleFetch(ServerContact s, int session) {};

public void setUpdateInterval(int newInterval) {};

/**
   shutdown()
   @methodtype command
 */
public final void
shutdown()
{
	_frame.hide();
}

// mil.navy.nrl.cmf.sousa.idol.user.setPosition
/**
   updatePosition(Vector3d)
   @methodtype set
   @param position .
 */
public final void setPosition(/*@ non_null */ Vector3d position, 
							  /*@ non_null */Vector3d width,
							  /*@ non_null */Vector3d hpr)
{
	for (Iterator i = _setPositions.iterator(); i.hasNext(); ) {
		SetPosition s = (SetPosition)i.next();
		_LOG.debug(new Strings(new Object[]
			{this, " sending setPosition(", position, ", ", width, ", ", hpr, ") to ", s}));
		s.setPosition(position, width, hpr);
	}
}

// mil.navy.nrl.cmf.sousa.idol.user.setTime
/**
   updateTime(Calendar)
   @methodtype set
   @param time .
 */
public final void setTime(/*@ non_null */ Calendar timeLowerBound, 
						  /*@ non_null */ Calendar timeUpperBound)
{
	for (Iterator i = _setTimes.iterator(); i.hasNext(); ) {
		SetTime s = (SetTime)i.next();
		s.setTime(timeLowerBound, timeUpperBound);
	}
}

}; // GUI
