package mil.navy.nrl.cmf.annotation;

import mil.navy.nrl.cmf.sousa.*;
import mil.navy.nrl.cmf.sousa.idol.service.raster.*;
import mil.navy.nrl.cmf.sousa.spatiotemporal.*;
import java.util.*;
import java.net.*;
import java.sql.*;

public class Server {

	private static AnnotationQueryable _queryable;
	private static AnnotationInsertable _insertable;

    public static void main(String [] args) {

	int contactPort = Integer.parseInt(args[0]);
	String mcastAddress = args[1];
	InetAddress remote = null;
	int remotePort = -1;
	int mcastBasePort = Integer.parseInt(args[2]);

	try {
	    _queryable = new AnnotationQueryable("jdbc:postgresql://localhost/idol");
	    System.out.println("AnnotationQueryable initialized");
	} catch (SQLException ex) {
	    String error = "AnnotationQueryable failed to initialize";
	    
	    System.out.println("Exception:"+ex);
	}

	try {
	    _insertable = new AnnotationInsertable("jdbc:postgresql://localhost/idol");
	    System.out.println("AnnotationInsertable initialized");
	} catch (SQLException ex) {
	    String error = "AnnotationInsertable failed to initialize";
	    
	    System.out.println("Exception:"+ex);
	}

	State authState = new State();
	authState.addField(QueryFields.QUERYABLE_FIELDNAME, _queryable);
	authState.addField(ServerVI.INSERTABLE_FIELDNAME, _insertable);
	authState.addField(QueryFields.DESCRIPTION_FIELDNAME, "Annotation");

	// CommandLogic cmd = new ServerCmd();
	LinkedList cmdList = new LinkedList();
	// cmdList.add(cmd);
	
	ControlLogic ctrl = new ServerLogic();
	
	LinkedList viList = new LinkedList();
	viList.add(ServerVI.class);

	HashMap contentTypes = new HashMap();

	contentTypes.put("x-idol/x-annotation", null);

	try {
	    Entity e = new Entity(contactPort,
							  mcastAddress,
							  mcastBasePort,
							  authState,
							  ctrl,
							  viList,
							  cmdList,
							  contentTypes);
	}
	catch (Exception ex) {
	    System.out.println("Unknown error: "+ex);
	}
    }
};

