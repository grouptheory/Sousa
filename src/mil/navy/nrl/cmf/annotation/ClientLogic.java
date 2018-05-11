package mil.navy.nrl.cmf.annotation;

import mil.navy.nrl.cmf.sousa.*;
import mil.navy.nrl.cmf.sousa.spatiotemporal.*;
import mil.navy.nrl.cmf.sousa.idol.Console;
import java.util.*;
import java.io.Serializable;

public class ClientLogic extends ControlLogic 
    implements Console.CommandHandler
{
	private ServerSI _si;

    /**
    */
    public void projectorReadyIndication(ServerSideFSM fsm) {
		super.projectorReadyIndication(fsm);
		System.out.println("Projector ready.");
	}
    /**
    */
    public void projectorNotReadyIndication(ServerSideFSM fsm) {
		super.projectorNotReadyIndication(fsm);
		System.out.println("Projector NOT ready.");
	}
    /**
     */
    public void receptorReadyIndication(ClientSideFSM fsm) {
		super.receptorReadyIndication(fsm);
		System.out.println("Receptor ready.");

		_si = (ServerSI)fsm.genProxy(mil.navy.nrl.cmf.annotation.ServerSI.class);
	}
    /**
     */
    public void receptorNotReadyIndication(ClientSideFSM fsm) {
		super.receptorNotReadyIndication(fsm);
		System.out.println("Receptor NOT ready.");

		_si = null;
	}

    /**
       The command console.
    */
    final Console _console;
    
    /**
       Class constructor makes a Console.
    */
    public ClientLogic() {
	
	_console = new Console(this);
	_console.start();
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
	// command = command.toLowerCase();
	if (command.startsWith("help")) {
	    command = command.replaceFirst("help","").trim();
	    help(command);
	} else if (command.startsWith("set")) {
	    command = command.replaceFirst("set","").trim();
	    set(command);
	} else if (command.startsWith("get")) {
	    command = command.replaceFirst("get","").trim();
	    Object obj = get(command);
	    if (obj != null) {
			if (obj instanceof Calendar) obj = Calendar2String((Calendar)obj);
			System.out.println("Field value = "+obj);
	    }
	}
	else if (command.startsWith("print")) {
	    command = command.replaceFirst("print","").trim();
	    print(command);
	}
	else if (command.startsWith("add")) {
	    command = command.replaceFirst("add","").trim();
	    add(command);
	}
	else if (command.startsWith("quit")) {
	    command = command.replaceFirst("quit","").trim();
	    quit(command);
	}
    }
    
    /**
       Adds an annotation to the server at the client's present
       spatiotemporal coordinates.
       
       @param note The annotation for the client's current position.
    */
    protected void add(String note) {
	_si.annotate(note,
		     (Vector3d)get(QueryClientFields.POSITION_FIELDNAME),
		     (Calendar)get(QueryClientFields.TIMELOWERBOUND_FIELDNAME),
		     (Calendar)get(QueryClientFields.TIMEUPPERBOUND_FIELDNAME));
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
	boolean bad = true;
	for (Iterator it=fns.iterator(); it.hasNext();) {
	    String fn = (String)it.next();
	    if (command.startsWith(fn)) {
		command = command.replaceFirst(fn,"").trim();
		set(fn, command);
		bad = false;
	    }
	}
	if (bad)
		System.out.println("fieldname not found!");

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
	Serializable val=null;

	if (fieldname.equals(QueryClientFields.POSITION_FIELDNAME)) {
		command = command.substring(command.indexOf("<")).replaceFirst("\\<","");
		double x = Double.parseDouble( command.substring(0,command.indexOf(",")).trim() );
		command = command.substring(command.indexOf(",")).replaceFirst(",","");
		double y = Double.parseDouble(command.substring(0,command.indexOf(",")).trim() );
		command = command.substring(command.indexOf(",")).replaceFirst(",","");
		double z = Double.parseDouble(command.substring(0,command.indexOf(">")).replaceAll("\\>","").trim() );
		val = new Vector3d(x,y,z);
	}
	if (fieldname.equals(QueryClientFields.WIDTH_FIELDNAME)) {
		command = command.substring(command.indexOf("<")).replaceFirst("\\<","");
		double x = Double.parseDouble( command.substring(0,command.indexOf(",")).trim() );
		command = command.substring(command.indexOf(",")).replaceFirst(",","");
		double y = Double.parseDouble(command.substring(0,command.indexOf(",")).trim() );
		command = command.substring(command.indexOf(",")).replaceFirst(",","");
		double z = Double.parseDouble(command.substring(0,command.indexOf(">")).replaceAll("\\>","").trim() );
		val = new Vector3d(x,y,z);
	}
	if (fieldname.equals(QueryClientFields.TIMELOWERBOUND_FIELDNAME)) {
		command = command.substring(command.indexOf("<")).replaceFirst("\\<","");
		int y = Integer.parseInt( command.substring(0,command.indexOf(",")).trim() );
		command = command.substring(command.indexOf(",")).replaceFirst(",","");
		int m = Integer.parseInt(command.substring(0,command.indexOf(",")).trim() );
		command = command.substring(command.indexOf(",")).replaceFirst(",","");
		int d = Integer.parseInt(command.substring(0,command.indexOf(",")).trim() );
		command = command.substring(command.indexOf(",")).replaceFirst(",","");
		int hr = Integer.parseInt(command.substring(0,command.indexOf(",")).trim() );
		command = command.substring(command.indexOf(",")).replaceFirst(",","");
		int mn = Integer.parseInt(command.substring(0,command.indexOf(",")).trim() );
		command = command.substring(command.indexOf(",")).replaceFirst(",","");
		int sc = Integer.parseInt(command.substring(0,command.indexOf(">")).replaceAll("\\>","").trim() );
		val = new GregorianCalendar();
		((Calendar)val).set(y,m,d,hr,mn,sc);

		Calendar upper=(Calendar)get(QueryClientFields.TIMEUPPERBOUND_FIELDNAME);
		if (((Calendar)val).after(upper)) {
			val = (GregorianCalendar)upper.clone();
			((Calendar)val).add(Calendar.SECOND, -1);
		}
	}
	if (fieldname.equals(QueryClientFields.TIMEUPPERBOUND_FIELDNAME)) {
		command = command.substring(command.indexOf("<")).replaceFirst("\\<","");
		int y = Integer.parseInt( command.substring(0,command.indexOf(",")).trim() );
		command = command.substring(command.indexOf(",")).replaceFirst(",","");
		int m = Integer.parseInt(command.substring(0,command.indexOf(",")).trim() );
		command = command.substring(command.indexOf(",")).replaceFirst(",","");
		int d = Integer.parseInt(command.substring(0,command.indexOf(",")).trim() );
		command = command.substring(command.indexOf(",")).replaceFirst(",","");
		int hr = Integer.parseInt(command.substring(0,command.indexOf(",")).trim() );
		command = command.substring(command.indexOf(",")).replaceFirst(",","");
		int mn = Integer.parseInt(command.substring(0,command.indexOf(",")).trim() );
		command = command.substring(command.indexOf(",")).replaceFirst(",","");
		int sc = Integer.parseInt(command.substring(0,command.indexOf(">")).replaceAll("\\>","").trim() );
		val = new GregorianCalendar();
		((Calendar)val).set(y,m,d,hr,mn,sc);

		Calendar lower=(Calendar)get(QueryClientFields.TIMELOWERBOUND_FIELDNAME);
		if (((Calendar)val).before(lower)) {
			val = (GregorianCalendar)lower.clone();
			((Calendar)val).add(Calendar.SECOND, 1);
		}
	}
	if (fieldname.equals(QueryClientFields.FIELDS_FIELDNAME)) {
		val = command;
	}

	try {
	    as.setField(fieldname, val);

		System.out.print("set "+fieldname+" --> ");
		if (val instanceof Calendar) System.out.println(Calendar2String((Calendar)val));
		else System.out.println(val);

	} catch (NoSuchFieldException ex) {
	    System.out.println("No such field!");
	}
    }
    
    /**
       Returns the value of a field in the Entity's State.
       
       @param fieldname the name of the field
       
       @throws NoSuchFieldException if there is no field that matches fieldname
    */
    private Object get(String fieldname) {
	Object obj = null;
	State as = this.getEntity().getState();
	try {
	    obj = as.getField(fieldname);
	} catch (NoSuchFieldException ex) {
	    System.out.println("No such field: "+fieldname);
	}
	return obj;
    }
    
		private String Calendar2String(Calendar c) {
			String s = "";
			s += "<";
			s += ""+c.get(Calendar.YEAR)+", ";
			s += ""+c.get(Calendar.MONTH)+", ";
			s += ""+c.get(Calendar.DATE)+", ";
			s += ""+c.get(Calendar.HOUR)+", ";
			s += ""+c.get(Calendar.MINUTE)+", ";
			s += ""+c.get(Calendar.SECOND)+">";
			return s;
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
		Object obj = get(fn);
		if (obj instanceof Calendar) obj = Calendar2String((Calendar)obj);
	    System.out.println(fn+"="+obj);
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
	    System.out.println("Console shut down!");
	}
	this.stopRunning();
    }
};

