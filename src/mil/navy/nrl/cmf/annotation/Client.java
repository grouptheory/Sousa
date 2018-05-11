package mil.navy.nrl.cmf.annotation;

import mil.navy.nrl.cmf.sousa.*;
import mil.navy.nrl.cmf.sousa.spatiotemporal.*;
import java.util.*;
import java.net.*;

public class Client {

    public static String LAT_FNAME = "LAT";
    public static String LON_FNAME = "LON";
    public static String TIME_FNAME = "TIME";

    public static void main(String [] args) {

	int contactPort = Integer.parseInt(args[0]);
	String mcastAddress = args[1];
	int mcastBasePort = Integer.parseInt(args[2]);
	InetAddress remoteAddress = null;
	try {
	    remoteAddress = InetAddress.getByName(args[3]);
	}
	catch (UnknownHostException ex) {
	    System.out.println("Unknown host: "+args[3]);
	    System.exit(-1);
	}
	int remotePort = Integer.parseInt(args[4]);

	State authState = new State();
	authState.addField(QueryClientFields.POSITION_FIELDNAME, new Vector3d(0,0,0));
	authState.addField(QueryClientFields.WIDTH_FIELDNAME, new Vector3d(1.0,1.0,1.0));
	authState.addField(QueryClientFields.TIMELOWERBOUND_FIELDNAME, new GregorianCalendar());
	Calendar upper = new GregorianCalendar();
	upper.add(Calendar.MINUTE,1);
	authState.addField(QueryClientFields.TIMEUPPERBOUND_FIELDNAME, upper); 
	authState.addField(QueryClientFields.FIELDS_FIELDNAME, new String(""));

	CommandLogic cmd = new ClientCmd();
	LinkedList cmdList = new LinkedList();
	
	ControlLogic ctrl = new ClientLogic();
	
	LinkedList viList = new LinkedList();
	//	viList.add(ClientVI.class);
	viList.add(SpatiotemporalViewInterpreter.class);
	try {
	    Entity e = new Entity(contactPort,
							  mcastAddress,
							  mcastBasePort,
							  authState,
							  ctrl,
							  viList,
							  cmdList,
							  // TODO: Content-Type Map
							  null);

	    QoS qos = new QoS();
	    qos.add(ServerVI.class);
	    try {
		ServerContact sc = new ServerContact(remoteAddress, remotePort, qos);
		e.scheduleConnectTo(sc);
	    }
	    catch (UnknownHostException ex) {
		System.out.println("Unable to make contact with the AnnotationServer at "+remoteAddress+":"+remotePort);
	    }
	}
	catch (Exception ex) {
	    System.out.println("Unknown error: "+ex);
	}
    }
};

