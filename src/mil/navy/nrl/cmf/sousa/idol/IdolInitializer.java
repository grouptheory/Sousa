package mil.navy.nrl.cmf.sousa.idol;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.LinkedList;
import java.util.Properties;

import mil.navy.nrl.cmf.sousa.Entity;
import mil.navy.nrl.cmf.sousa.State;
import mil.navy.nrl.cmf.sousa.ControlLogic;
import mil.navy.nrl.cmf.sousa.CommandLogic;
import mil.navy.nrl.cmf.sousa.EntityInitializer;
import mil.navy.nrl.cmf.sousa.QoS;
import mil.navy.nrl.cmf.sousa.Renderer;
import mil.navy.nrl.cmf.sousa.ServerContact;
import mil.navy.nrl.cmf.sousa.util.ObjectFactory;
import mil.navy.nrl.cmf.sousa.util.StackTrace;
import mil.navy.nrl.cmf.sousa.util.Strings;

import org.apache.log4j.Logger;

/**
   IdolInitializer configures an agent's state and provides a simple
   ControlLogic that permits the setting and inspecting of the
   Entity's state on the console.
*/
public abstract class IdolInitializer
    implements EntityInitializer
{
    private static final Logger _LOG = 
		Logger.getLogger(IdolInitializer.class);
    
    // note that all data members are private and final
    
    // the defaults
	/**
	   The default contact port
	 */
    public static final int DEFAULT_CONTACTPORT = 4300;

	/**
	   The default multicast base port
	 */
    public static final int DEFAULT_MCASTBASEPORT = -1;
    
    // the addresses
	/**
	   The Entity's contact port

	   @see #getContactPort()
	 */
    private final int _contactPort;

	/**
	   The lower bound of the Entity's multicast ports

	   @see #getMcastBasePort()
	 */
    private final int _mcastBasePort;

	/**
	   The Entity's multicast address

	   @see #getMcastAddress()
	 */
    private final String _mcastAddress;

	/**
	   The name of the property that contains the Entity's multicast
	   address
	 */
	public static final String IDOL_MCASTADDR_PROP = "idol.entity.mcastAddress";

	/**
	   The name of the property that contains the lower bound of the
	   Entity's multicast port.  The value is an integer.
	 */
    public static final String IDOL_MCASTBASEPORT_PROP = "idol.entity.mcastBasePort";

	/**
	   The name of the property that contains the Entity's contact
	   port.  The value is an integer.
	 */
    public static final String IDOL_CONTACTPORT_PROP = "idol.entity.contactport";


    // state and logic
	/**
	   The Entity's </code>State</code> as initialized by the
	   <code>IdolInitializer</code> constructor

	   @see #getState()
	 */
    private final State _state;

	/**
	   The Entity's <code>ControlLogic</code> as initialized by
	   <code>initialize_makeControlLogic</code>

	   @see #getControlLogic()
	 */
    private final ControlLogic _controlLogic;

    // qos classes
	/**
	   All of the <code>Class</code> objects in the Entity's quality
	   of service

	   @see #addQoSClass(Class)
	   @see #getQoSClasses()
	 */
    private final List _qosclasses = new LinkedList();

	/**
	   All of the Entity's <code>CommandLogics</code>

	   @see #addCommandLogic(CommandLogic)
	   @see #getCommandLogics()
	*/
    private final List _commandlogics = new LinkedList();

	/**
	  All of the servers to contact when the Entity is live.

	  @see #scheduleConnectTo(ServerContact)
	  @see #scheduleInitialFetches(Entity)
	*/
    private final LinkedList _serversToContact = new LinkedList();
    
	/**
	   Maps Content-Type String to the Class of Renderer that draws it
	   on a Renderable.
	 */
	private final HashMap _contentTypes = new HashMap();

    // field autogen constants and rules
    //
    // A comma separated list of class names.  The Entity State
    // contains some of the Fields that are defined in these classes.
    // All static String fields with names that end in the value of
    // IDOL_INITIALIZER_FIELDNAME_SUFFIX are included to the
    // Entity State.
    //
    // The value of each Field comes from a Properties object.  For
    // each class name C from IDOL_INITIALIZER_ATTR_PROP, each field F
    // in C that appears in the Entity State has the value of
    // the Property idol.C.F.
    //
    // For example, suppose the Property named by
    // IDOL_INITIALIZER_ATTR_PROP has the value
    // "mil.navy.nrl.cmf.sousa.idol.Foo" and the Class
    // mil.navy.nrl.cmf.sousa.idol.Foo has fields named
    // thing1_FIELDNAME and thing2_FIELDNAME.
    //
    // If the value of thing1_FIELDNAME is "Thing1" and the value of
    // thing2_FIELDNAME is "Thing2", then the Entity State will
    // have fields Thing1 and Thing2.
    //
    // The value of Thing1 will be the value of the Property
    // idol.mil.navy.nrl.cmf.sousa.idol.Foo.Thing1.
    //
    // The value of Thing2 will be the value of the Property
    // idol.mil.navy.nrl.cmf.sousa.idol.Foo.Thing2.
    //

	/**
	   The name of the property that contains classes that supply the
	   names of the fields of the Entity's State.  The value is a
	   comma separated list of fully qualified class names.

	   @see #IdolInitializer(Properties)
	 */
    public static final String IDOL_INITIALIZER_ATTR_PROP = "idol.initializer.attributes";

	/**
	   Magic suffix for field names that contain the names of the
	   Entity's State fields.

	   @see #IdolInitializer(Properties)
	   @see #initialize_getFieldNames(String)
	 */
    public static final String IDOL_INITIALIZER_FIELDNAME_SUFFIX = "FIELDNAME";


	public static final String IDOL_INITIALIZER_CONTENTTYPE_PROP = "idol.initializer.contentType";

    // Base ControlLogic with Console Support
    
    /**
	  Basic ControlLogic with command console support.  The command
	  console, if enabled, understands four commands: help, set,
	  print, quit.  The comand console's range of commands can be
	  expanded by overriding <code>handleCommand(String)</code>.
     */
    public static class Console_ControlLogic 
		extends ControlLogic
		implements Console.CommandHandler
    {
		// DAVID: there is a bug in javadoc for 1.4.2.  It won't recognize valid @see
		// tags for inner class constructors.  The @see, below, should be
		//
		// @see IdolInitializer.Console_ControlLogic#Console_ControlLogic(Properties)
		//
		// javadoc prints a warning about the @see, below, but it generates the
		// correct HTML in spite of itself.
		//
		//		warning - Tag @see: missing #: "IdolInitializer.Console_ControlLogic(Properties)"
		/**
		   The name of the property that determines if there is a Console.
		   Acceptable values are "on" and "off". The default value of the
		   property is "off".

		   @see #IdolInitializer.Console_ControlLogic(Properties)
		*/
		public static final String IDOL_CONSOLE_PROP = "idol.initializer.console";


		/**
		   The command console.
		 */
		final Console _console;

		/**
		   Class constructor that uses the value of the property
		   <code>idol.initializer.console</code> 
		   to
		   determine if this <code>Console_ControlLogic</code> has a command
		   console or not.  If <code>idol.initializer.console</code>
		   has the value <code>on</code>, then
		   this <code>Console_ControlLogic</code> has a command console.

		   @param p the <code>Properties</code> of this <code>Console_ControlLogic</code>.
		*/
		public Console_ControlLogic(Properties p) {

			String consoleOnOff = p.getProperty(IDOL_CONSOLE_PROP, "off");
			if (consoleOnOff.equals("on")) {
				_console = new Console(p, this);
				_console.start();
			}
			else {
				_console = null;
			}
		}
	
		/**
		   Implements the help, set, print, and quit commands.
		   The commands, their arguments, and their meanings are
		   <UL>
		   <LI>help - Print the help message.
		   <LI>set &lt;field name&gt; &lt;field value&gt; - Assign the value to the field of the Entity's State.
		   <LI>print &lt;field name&gt; - Print the value of the field on the console.
		   <LI>quit - Force the Entity to terminate.
		   </UL>
		   <P>
		   handleCommand ignores all other inputs.
		   <P>
		   Override handleCommand to extend the range of command
		   strings it understands.

		   @param command a string to interpret as a command
		*/
		public void handleCommand(/*@ non_null */ String command) {
			command = command.toLowerCase();
			if (command.startsWith("help")) {
				command = command.replaceFirst("help","").trim();
				help(command);
			} else if (command.startsWith("set")) {
				command = command.replaceFirst("set","").trim();
				set(command);
			}
			else if (command.startsWith("print")) {
				command = command.replaceFirst("print","").trim();
				print(command);
			}
			else if (command.startsWith("quit")) {
				command = command.replaceFirst("quit","").trim();
				quit(command);
			}
		}
	
		/**
		   Prints helpful text that describes how to control the
		   Entity.
		   <P>
		   <EM>BUG: This implementation ignores <code>command</code>.  It
		   should not.</EM>

		   @param command the command for which to print help.  If
		   command is null, then print the text for all known
		   commands.
		 */
		protected void help(String command) {
			System.out.println("General commands:");
			System.out.println("set <field name> <value>");
			System.out.println("get <field name>");
			System.out.println("print");
			System.out.println("quit");

		}

		/**
		   Change the value of one of the fields in the Entity's
		   State.  The new value of the field must be a Double.

		   @param command the name of the field, separated by from its
		   value by white space
		 */
		private void set(String command) {
			Set fns = this.getEntity().getState().getFieldNames();
			for (Iterator it=fns.iterator(); it.hasNext();) {
				String fn = (String)it.next();
				if (command.startsWith(fn)) {
					command = command.replaceFirst(fn,"").trim();
					set(fn, command);
				}
			}
		}
	
		/**
		   Change the value of one of the fields in the Entity's
		   State.  The new value must be a Double.  Prints an error
		   message if there is no such field.

		   @param fieldname the name of the field
		   @param command the new value of the field
		 */
		private void set(String fieldname, String command) {
			State as = this.getEntity().getState();
			Double val = new Double(Double.parseDouble(command));
			try {
				as.setField(fieldname, val);
			} catch (NoSuchFieldException ex) {
				_LOG.error(ex);
			}
		}
	
		/**
		   Returns the value of a field in the Entity's State.

		   @param fieldname the name of the field

		   @throws NoSuchFieldException if there is no field that matches fieldname
		 */
		private Object get(String fieldname)
			throws NoSuchFieldException {
			State as = this.getEntity().getState();
			return as.getField(fieldname);
		}
	
		/**
		   Prints the value of a field of the Entity's State on the console.
		   Prints an error message if there is no such field.

		   @param command the name of the field
		 */
		private void print(String command) {
			Set fns = this.getEntity().getState().getFieldNames();
			for (Iterator it=fns.iterator(); it.hasNext();) {
				String fn = (String)it.next();
				try {
					System.out.println(fn+"="+get(fn));
				} catch (NoSuchFieldException ex) {
					_LOG.error(ex);
				}
			}					
		}
	
		/**
		   Stops the command console, if there is one, then stops the Entity.
		   <P>
		   BUG - This implementation ignores command.

		   @param command hints for quit
		 */
		protected void quit(String command) {
			if (_console != null) {
				_console.stopRunning();
				_LOG.error("Console shut down");
			}
			this.stopRunning();
		}
    }
    
    // Constructors
    
    /**
	   Class constructor that uses a Property to determine the initial
	   values of the Entity's communications parameters and State fields.
	   <P>
	   The communications parameters are
	   <UL>
	   <LI>a multicast address, a string whose value comes from the property
	   <code>idol.entity.mcastAddress</code>
	   <LI>a multicast port, an integer whose value comes
	   from the property <code>idol.entity.mcastBasePort</code>
	   <LI>a contact port,  an integer whose value comes
	   from the property <code>idol.entity.contactport</code>
	   </UL>
	   <P>
	   The State of the Entity is initialized automatically by
	   IdolInitializer.  Derived classes may provide additional ways
	   to initialize the State.
	   <P>
	   The process of automatic State initialization has a few rules.
	   The rules may appear to be complicated but in practice they are
	   simple and easy to follow.
	   <P>
	   First, <code>IdolInitializer</code> obtains the names of the classes that
	   contain the names of the fields from the property
	   <code>idol.initializer.attributes</code>, a 
	   comma separated list of fully qualified class names.
	   <P>
	   Second, <code>IdolInitializer</code> obtains the names of the fields by
	   reflection on each of the classes in
	   <code>idol.initializer.attributes</code>.  The value of each
	   <code>public static final String</code> field whose name ends in "FIELDNAME"
	   becomes the name of a State field.
	   <P>
	   Third, <code>IdolInitializer</code> creates an <code>ObjectFactory</code> for each field.
	   It constructs the name of a property that contains the name of
	   the concrete class of the <code>ObjectFactory</code> from the name of the
	   class, C, that provides the field, F, according to the pattern
	   C.F.factory.  For example, the class
	   <code>idol.mil.navy.nrl.cmf.sousa.spatiotemporal.QueryClientFields</code>
	   provides the field <code>Position</code>, so the name of the
	   <code>ObjectFactory</code> class that will construct the
	   <code>Position</code> field in the State is the value of the
	   property
	   <code>idol.mil.navy.nrl.cmf.sousa.spatiotemporal.QueryClientFields.Position.factory</code>.
	   <P>
	   Fourth, <code>IdolInitializer</code> uses each field's <code>ObjectFactory</code> to
	   instantiate the value of the field from p given a property name
	   prefix of the form C.F, as above.  The prefix is the beginning
	   of the names of properties of p that the <code>ObjectFactory</code> may use
	   to instantiate the field.  Each <code>ObjectFactory</code> has its own
	   requirements for naming the values it uses to instantiate its
	   class of object.
	   <P>
	   Fifth, <code>IdolInitializer</code> creates a Map from RFC2045
	   MIME Content-Type to <CODE>Class</CODE> of <CODE>Renderer</CODE>.  See
	   {@link #initialize_makeControlLogic(Properties)} for the
	   properties that define this Map.
	   <P>
	   Here is an example of a properties file.  It is part of the
	   properties file for one of the sample client Entities.
	   <PRE>
	   idol.initializer.attributes=mil.navy.nrl.cmf.sousa.spatiotemporal.QueryClientFields
	   idol.mil.navy.nrl.cmf.sousa.spatiotemporal.QueryClientFields.Position.factory=mil.navy.nrl.cmf.sousa.spatiotemporal.Vector3dFactory
	   idol.mil.navy.nrl.cmf.sousa.spatiotemporal.QueryClientFields.Position.x=0
	   idol.mil.navy.nrl.cmf.sousa.spatiotemporal.QueryClientFields.Position.y=0
	   idol.mil.navy.nrl.cmf.sousa.spatiotemporal.QueryClientFields.Position.z=0
	   </PRE>
	   <P>
	   <code>idol.initializer.attributes</code> names a single class,
	   <code>mil.navy.nrl.cmf.sousa.spatiotemporal.QueryClientFields</code>,
	   that provides the names of the fields of the Entity's state.
	   <P>
	   The <code>Position</code> field is initialized by an <code>ObjectFactory</code> 
	   of class <code>mil.navy.nrl.cmf.sousa.spatiotemporal.Vector3dFactory</code>.
	   <P>
	   The properties
	   <code>idol.mil.navy.nrl.cmf.sousa.spatiotemporal.QueryClientFields.Position.x</code>,
	   <code>idol.mil.navy.nrl.cmf.sousa.spatiotemporal.QueryClientFields.Position.y</code>,
	   and
	   <code>idol.mil.navy.nrl.cmf.sousa.spatiotemporal.QueryClientFields.Position.z</code>
	   are potential inputs to the <code>Vector3dFactory</code>.
	   <P>
	   Here is an excerpt from a version of
	   <code>mil.navy.nrl.cmf.sousa.spatiotemporal.QueryClientFields</code>.
	   According to the automatic field initialization convention described above,
	   <code>POSITION_FIELDNAME</code>, a public static final String field,
	   contains the name of a field in the Entity's State.  The name
	   of that field is "Position".
	   <PRE>
	   package mil.navy.nrl.cmf.sousa.spatiotemporal;

	   public final class QueryClientFields 
	   {
	     public static final String POSITION_FIELDNAME = "Position";
	   ...
	   };
	   </PRE>
	   <P>
	   Initialization of the <code>ControlLogic</code> is determined by the derived classes.

	   @param p the Properties of the Entity
	   @see IdolInitializer.Console_ControlLogic
	   @see mil.navy.nrl.cmf.sousa.util.ObjectFactory
    */
    public IdolInitializer(Properties p)
		throws EntityInitializer.InitializationException
    {
		//** Comms ************************************
		_mcastAddress = p.getProperty(IDOL_MCASTADDR_PROP);
		String portString = p.getProperty(IDOL_CONTACTPORT_PROP);
		String mcastBasePortString = p.getProperty(IDOL_MCASTBASEPORT_PROP);
		try {
			if (portString != null) {
				try {
					_contactPort = Integer.parseInt(portString);
				}
				catch (NumberFormatException ex) {
					throw new EntityInitializer.InitializationException("Error initializing contact port", ex);
				}
			}
			else {
				_contactPort = DEFAULT_CONTACTPORT;
			}
	    
			if (mcastBasePortString != null) {
				try {
					_mcastBasePort = Integer.parseInt(mcastBasePortString);
				}
				catch (NumberFormatException ex) {
					throw new EntityInitializer.InitializationException("Error initializing mcast base port", ex);
				}
			}
			else {
				_mcastBasePort = DEFAULT_MCASTBASEPORT;
			}
		}
		catch (Exception ex) {
			_LOG.error(new Strings(new Object[] {
				"Caught exception ", ex, ":",
				StackTrace.formatStackTrace(ex)}));
			throw new EntityInitializer.InitializationException("Error initializing something", ex);
		}
		//** Comms ************************************

	
		//** Entity State ************************************
		_state = new State();
		String classNames = p.getProperty(IDOL_INITIALIZER_ATTR_PROP);
		if (null != classNames) {
			Set fnames = null;
			boolean done = false;

			do {
				String className = classNames;
				int index = classNames.indexOf(",");
				if (index != -1) {
					className = classNames.substring(0,index);
					classNames = classNames.substring(index+1);
				} else {
					done = true;
				}

				fnames = initialize_getFieldNames(className);
				initialize_loadDefaultFieldValues(className, fnames, p);
			}
			while (!done);
		}
		_LOG.warn("Entity State initialized");
		//** Entity State ************************************
	

		//** ControlLogic ************************************
		_controlLogic = initialize_makeControlLogic(p);
		_LOG.warn("ControlLogic initialized");
		//** ControlLogic ************************************

		//** Content-Type Map ************************************
		initialize_ContentTypes(p);

		// all custom initialization happens here
		initialize_custom(p);
    }
    
    // IdolInitializer methods
    
	/**
	   Collects the names of the automatically initialized State
	   fields.  <code>initialize_getFieldNames</code> searches the class named by
	   <code>className</code> for <code>public static final String</code> fields whose names end
	   in "FIELDNAME".  The values of these fields will be the names
	   of the State fields.

	   @param className the fully qualified name of a class
	   @return a set of strings that are the names of the fields
	 */
    private final Set initialize_getFieldNames(String className) {
		Set fns = new HashSet();
		try {
			Class cls = Class.forName(className);
			java.lang.reflect.Field[] fs = cls.getDeclaredFields();
			for (int i = 0; i<fs.length; i++) {
				java.lang.reflect.Field f = fs[i];
				int modifiers = f.getModifiers();
				//
				// f must be a public static final String.
				//
				// E.g. public static final String FOO_FIELDNAME = "foo";
				// 
				if (Modifier.isFinal(modifiers) &&
					Modifier.isStatic(modifiers) &&
					Modifier.isPublic(modifiers) &&
					f.getType().equals(String.class)) {
					String fname = f.getName();
					if (fname.endsWith(IDOL_INITIALIZER_FIELDNAME_SUFFIX)) {
						// The reflection code above discovers a field
						// named FOO_FIELDNAME in some class.  The value
						// of FOO_FIELDNAME is the name of the field in
						// the Entity State.
						try {
							String fieldname = (String)f.get(null);

							_LOG.debug(new Strings(new Object[]
								{"IdolInitializer.initialize_getFieldNames(", 
								 className, "): adding ", fname, "=", 
								 fieldname}));

							fns.add(fieldname);
						} catch (IllegalAccessException ex) {
							// Ignore f.
						} catch (IllegalArgumentException ex) {
							// Ignore f.
						} catch (NullPointerException ex) {
							// Ignore f.
						} catch (ExceptionInInitializerError ex) {
							// Ignore f.
						} catch (ClassCastException ex) {
							// f is not a String.
							_LOG.fatal("Expected a public static final String instead of " +
									   f);
						}
					}
				}
			}
		}
		catch (ClassNotFoundException ex) {
			_LOG.error(new Strings(new Object[] 
				{"ClassNotFoundException (", IDOL_INITIALIZER_ATTR_PROP, "=", 
				 className, ")"}));
		}
		return fns;
    }
    
	/**
	   Initializes the fields of _state given the name of an
	   <code>ObjectFactory</code>, a set of field names, and a
	   Properties that may contain initialization hints.

	   @param className the name of the <code>ObjectFactory</code> class
	   @param fns the set of field names (Strings)
	   @param p the properties for initializing the fields
	*/
    private final void initialize_loadDefaultFieldValues(String className, Set fns, Properties p) {
		for (Iterator it=fns.iterator(); it.hasNext();) {
			String fn = (String)it.next();
			String prefix = "idol."+className+"."+fn;
			String factoryClassName = p.getProperty(prefix + ".factory");

			_LOG.info(new Strings(new Object[] 
				{"IdolInitializer.initialize_loadDefaultFieldValues(",
				 className, "): factory ", factoryClassName}));

			try {
				// Create the factory.
				// This could fail.
				// DAVID: What happens if the factory property is absent?
				Class cls = Class.forName(factoryClassName); 
				Constructor factoryCtor = cls.getConstructor(new Class[0]);
				ObjectFactory factory = (ObjectFactory)factoryCtor.newInstance(new Object[0]);

				// Instantiate the field using the factory.
				// This could fail.
				//
				// DAVID: What happens if f.create() doesn't return a
				// Serializable?  ClassCastException?
				_state.addField(fn, (Serializable)factory.create(prefix, p));

			} catch (ClassNotFoundException ex) {// Class.forName()
				_LOG.error(ex);
			} catch (NoSuchMethodException ex) {
				_LOG.error(ex);
			} catch (IllegalAccessException ex) {
				_LOG.error(ex);
			} catch (IllegalArgumentException ex) {
				_LOG.error(ex);
			} catch (InstantiationException ex) {
				_LOG.error(ex);
			} catch (InvocationTargetException ex) {
				_LOG.error(ex);
			} catch (ExceptionInInitializerError ex) {
				_LOG.error(ex);
			}
		}
	}
    
	/**
	   Constructs a ControlLogic using elements of <code>p</code>.

	   @param p the <code>Properties</code> of the <code>ControlLogic</code>
	   @return a <code>ControlLogic</code>
	 */
    protected abstract ControlLogic initialize_makeControlLogic(Properties p)
		throws EntityInitializer.InitializationException;

	/**
	   Constructs a Map from String to Class that implements Renderer.
	   <P>
	   BUGS:
	   <UL>
	   <LI>This Map could be constructed by an ObjectFactory.</LI>
	   </UL>
	   <P>
	   Expects Properties to contain elements prefixed with
	   IDOL_INITIALIZER_CONTENTTYPE_PROP.
	   <P>
	   There must be a ".size" Property.  Size is an integer.  It is
	   the number of entries in the Map.
	   <P>
	   There must be as many ".element.#.type" Properties as the value
	   of the ".size" property.  The value of this Property is a MIME
	   Content-Type string of the form <CODE>type/subtype</CODE> per
	   RFC2045.
	   <P>
	   Any ".element.#.type" Property may have a corresponding
	   ".element.#.renderer" Property.  The value of this Property is
	   the name of a Class that implements Renderer.  The Renderer of
	   any ".element.#.type" Property without a corresponding
	   ".element.#.renderer" Property is null.
	   <P>
	   In the sample below, there are three entries for the Map.
	   The Renderer for <code>x-idol/x-city</code> is <code>mil.navy.nrl.cmf.sousa.idol.user.CityRenderer</code>.
	   The Renderer for <code>x-idol/x-point</code> is <code>mil.navy.nrl.cmf.sousa.idol.user.PointRenderer</code>.
	   The Renderer for <code>x-idol/x-NoRenderer</code> is <code>null</code>.
	   <P>
	   <CODE>
	   idol.initializer.contentType.size=3
	   idol.initializer.contentType.element.0.type=x-idol/x-city
	   idol.initializer.contentType.element.0.renderer=mil.navy.nrl.cmf.sousa.idol.user.CityRenderer
	   idol.initializer.contentType.element.1.type=x-idol/x-point
	   idol.initializer.contentType.element.1.renderer=mil.navy.nrl.cmf.sousa.idol.user.PointRenderer
	   idol.initializer.contentType.element.2.type=x-idol/x-NoRenderer
	   </CODE>
	   <P>

	   @throws IllegalArgumentException when there is no known Class
	   of renderer or when there is no size Property.  -1 in the
	   Exception text indicates the "no size" error.
	*/
	private void initialize_ContentTypes(Properties p)
		throws IllegalArgumentException 
	{
		HashMap types = new HashMap();
		int i = -1;

		try {
			Class rendererClass = Class.forName("mil.navy.nrl.cmf.sousa.Renderer");
			int size = Integer.parseInt(p.getProperty(IDOL_INITIALIZER_CONTENTTYPE_PROP+".size", "0"));

			_LOG.debug(new Strings(new Object[]
				{"IdolInitializer.initialize_ContentTypes(): There are ", 
				 new Integer(size), " content types"}));

			for (i=0; i < size; i++) {
				String index = ".element."+i;
				String type = p.getProperty(IDOL_INITIALIZER_CONTENTTYPE_PROP+index+".type");
				String renderer = p.getProperty(IDOL_INITIALIZER_CONTENTTYPE_PROP+index+".renderer");
				Class cls = null;

				_LOG.debug(new Strings(new Object[]
					{"IdolInitializer.initialize_ContentTypes(): ", 
					 new Integer(i), " ", type, "=", renderer}));

				if (null != type) {
					if (null != renderer) {
						cls = Class.forName(renderer);
						if (! rendererClass.isAssignableFrom(cls)) {
							throw new
								IllegalArgumentException("Instantiating " +
														 IDOL_INITIALIZER_CONTENTTYPE_PROP + 
														 " " + i + ":" +
														 renderer + 
														 " does not implement mil.navy.nrl.cmf.sousa.Renderer");
						}
					}

					_contentTypes.put(type, cls);
				}
			}
		} catch (IllegalArgumentException ex) {
			throw new IllegalArgumentException("Instantiating " + IDOL_INITIALIZER_CONTENTTYPE_PROP + " " + i + ":" + ex);
		} catch (ExceptionInInitializerError ex) {
			throw new IllegalArgumentException("Instantiating " + IDOL_INITIALIZER_CONTENTTYPE_PROP + " " + i + ":" + ex);
		} catch (ClassNotFoundException ex) {
			throw new IllegalArgumentException("Instantiating " + IDOL_INITIALIZER_CONTENTTYPE_PROP + " " + i + ":" + ex);
		}
	}

	/**
	   Add any additional fields custom to this particular initializer
	   and perform any other custom initializations.

	   @param p the <code>Properties</code> of the initializer
	*/
    protected abstract void initialize_custom(Properties p) 
		throws EntityInitializer.InitializationException;

    
	/**
	   Adds <code>s</code> to a list of servers that are to to be
	   contacted immediately after constructing the Entity.

	   @param s the server to contact
	*/
    protected final void scheduleConnectTo(ServerContact s) {
		_serversToContact.add(s);
    }

	/**
	   Adds a <code>QoS</code> class to the list of <code>QoS</code> classes.
	   
	   @param c a quality of service <code>Class</code>
	*/
    protected final void addQoSClass(Class c) {
		_qosclasses.add(c);
    }

	/**
	   Adds a <code>CommandLogic</code> to the list of <code>CommandLogic</code>s.
	   
	   @param cl the additional CommandLogic
	*/
    protected final void addCommandLogic(CommandLogic cl) {
		_commandlogics.add( cl );
    }

	/**
	   Adds a mapping between RFC2045 MIME <code>contentType</code>
	   and <code>renderer Class</code> to the Content-Type Map.

	   @param contentType RFC2045 MIME String representing some kind of content
	   @param renderer Class of an Object that can render the
	   contentType.  Must implement the Renderer interface.
	*/
	protected final void addContentType(String contentType, Class renderer) {
		_contentTypes.put(contentType, renderer);
	}

	/**
	   Adds a bunch of mappings to the Content-Type Map.

	   @param types a Map from String to Class, where the keys are
	   RFC2045 MIME Content-Types and the Classes implement the
	   Renderer interface.
	*/
	protected final void addContentTypes(Map types) {
		_contentTypes.putAll(types);
	}

    // EntityInitializer

    // note that all getter methods are final
    
    public final int getContactPort() {
		return _contactPort;
    }
    
    public final String getMcastAddress() {
		return _mcastAddress;
    }
    
    public final int getMcastBasePort() {
		return _mcastBasePort;
    }

    public final State getState() {
		return _state;
    }
    
    public final ControlLogic getControlLogic() {
		return _controlLogic;
    }

    public final List getQoSClasses() {
		return _qosclasses;
    }
    
    public final List getCommandLogics() {
		return _commandlogics;
    }

	public final Map getContentTypes() {
		return _contentTypes;
	}

    public final void scheduleInitialFetches(Entity e) {
		for (Iterator it = _serversToContact.iterator(); it.hasNext();) {
			ServerContact sc = (ServerContact)it.next();
			e.scheduleConnectTo(sc);
		}
    }
}; // IdolInitializer
