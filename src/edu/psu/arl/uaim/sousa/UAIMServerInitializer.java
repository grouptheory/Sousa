package edu.psu.arl.uaim.sousa;

import java.util.Properties;
import java.io.*;
import java.util.Vector;

import mil.navy.nrl.cmf.sousa.*;
import mil.navy.nrl.cmf.sousa.idol.service.ServerInitializer;
import mil.navy.nrl.cmf.sousa.spatiotemporal.*;
import mil.navy.nrl.cmf.sousa.util.Strings;

import org.apache.log4j.Logger;

/**
 * UAIMServerInitializer initializes an IDOL server that offers U-AIM alerts.
 * 
 */
public class UAIMServerInitializer extends ServerInitializer
{
    private static final Logger _LOG = Logger.getLogger(UAIMServerInitializer.class);
    
	//
	// TODO: Comment about the properties in the uaim.properties file.
	//

    /**
     * Default Constructor 
     * 
     * @param p
     * @throws EntityInitializer.InitializationException
     */
    public UAIMServerInitializer(Properties p) throws EntityInitializer.InitializationException {
        super(p);
    }
    
    /**
     * Initialize method that will initialize any fields that cannot be automatically initialized 
     * from the properties file in the constructor.
     * 
     * @param p
     * @throws EntityInitializer.InitializationException
	 */
    protected void initialize_custom(Properties p) throws EntityInitializer.InitializationException {
        super.initialize_custom(p);
	
		State s = this.getState();
        s.addField(QueryFields.DESCRIPTION_FIELDNAME,
				   "x-idol/x-uaim");
		// TODO: Add fields that contain the alerts
        this.addQoSClass(QueryViewInterpreter.class);
    }
    

    /**
     * Creates a UaimServer_ControLogic
     * 
     * @param p
     * @throws EntityInitializer.InitializationException
     */
    protected ControlLogic initialize_makeControlLogic(Properties p) 
    	throws EntityInitializer.InitializationException {
		return new UAIMServer_ControlLogic(p);
    }
    

    /** 
     * UAIMServer_ControlLogic extends the base class and handles the
     * processing of the alerts received from the UAIM system.
     */
    public static class UAIMServer_ControlLogic 
		extends ServerInitializer.Server_ControlLogic 
		implements Selectable.Handler {
        
        private P2PChannelFactory channelFactory = null;
        private Vector channels = new Vector();

	
    	/**
    	 * Main Constructor
    	 * 
    	 * @param p 
    	 * @throws EntityInitializer.InitializationException
    	 */
		public UAIMServer_ControlLogic(Properties p) throws EntityInitializer.InitializationException {
            super(p);

            try {
                int port = new Integer(p.getProperty("idol.uaim.port")).intValue();
                channelFactory = P2PChannelFactory.newInstance(port, null);
            } catch (Exception e) {
                _LOG.error("UAIMServer_ControlLogic exception e is " + e);
                e.printStackTrace();
				throw new EntityInt	//
	// TODO: Comment about the properties in the uaim.properties file.
	//

    /**
     * Default Constructor 
     * 
     * @param p
     * @throws EntityInitializer.InitializationException
     */
    public UAIMServerInitializer(Properties p) throws EntityInitializer.InitializationException {
        super(p);
    }
    
    /**
     * Initialize method that will initialize any fields that cannot be automatically initialized 
     * from the properties file in the constructor.
     * 
     * @param p
     * @throws EntityInitializer.InitializationException
	 */
    protected void initialize_custom(Properties p) throws EntityInitializer.InitializationException {
        super.initialize_custom(p);
	
		State s = this.getState();
        s.addField(QueryFields.DESCRIPTION_FIELDNAME,
				   "x-idol/x-uaim");
		// TODO: Add fields that contain the alerts
        this.addQoSClass(QueryViewInterpreter.class);
    }
    

    /**
     * Creates a UaimServer_ControLogic
     * 
     * @param p
     * @throws EntityInitializer.InitializationException
     */
    protected ControlLogic initialize_makeControlLogic(Properties p) 
    	throws EntityInitializer.InitializationException {
		return new UAIMServer_ControlLogic(p);
    }
    

    /** 
     * UAIMServer_ControlLogic extends the base class and handles the
     * processing of the alerts received from the UAIM system.
     */
    public static class UAIMServer_ControlLogic 
		extends ServerInitializer.Server_ControlLogic 
		implements Selectable.Handler {
        
        private P2PChannelFactory channelFactory = null;
        private Vector channels = new Vector();

	
    	/**
    	 * Main Constructor
    	 * 
    	 * @param p 
    	 * @throws EntityInitializer.InitializationException
    	 */
		public UAIMServer_ControlLogic(Properties p) throws EntityInitializer.InitializationException {
            super(p);

            try {
                int port = new Integer(p.getProperty("idol.uaim.port")).intValue();
                channelFactory = P2PChannelFactory.newInstance(port, null);
            } catch (Exception e) {
                _LOG.error("UAIMServer_ControlLogic exception e is " + e);
                e.printStackTrace();
				throw new EntityInt	//
	// TODO: Comment about the properties in the uaim.properties file.
	//

    /**
     * Default Constructor 
     * 
     * @param p
     * @throws EntityInitializer.InitializationException
     */
    public UAIMServerInitializer(Properties p) throws EntityInitializer.InitializationException {
        super(p);
    }
    
    /**
     * Initialize method that will initialize any fields that cannot be automatically initialized 
     * from the properties file in the constructor.
     * 
     * @param p
     * @throws EntityInitializer.InitializationException
	 */
    protected void initialize_custom(Properties p) throws EntityInitializer.InitializationException {
        super.initialize_custom(p);
	
		State s = this.getState();
        s.addField(QueryFields.DESCRIPTION_FIELDNAME,
				   "x-idol/x-uaim");
		// TODO: Add fields that contain the alerts
        this.addQoSClass(QueryViewInterpreter.class);
    }
    

    /**
     * Creates a UaimServer_ControLogic
     * 
     * @param p
     * @throws EntityInitializer.InitializationException
     */
    protected ControlLogic initialize_makeControlLogic(Properties p) 
    	throws EntityInitializer.InitializationException {
		return new UAIMServer_ControlLogic(p);
    }
    

    /** 
     * UAIMServer_ControlLogic extends the base class and handles the
     * processing of the alerts received from the UAIM system.
     */
    public static class UAIMServer_ControlLogic 
		extends ServerInitializer.Server_ControlLogic 
		implements Selectable.Handler {
        
        private P2PChannelFactory channelFactory = null;
        private Vector channels = new Vector();

	
    	/**
    	 * Main Constructor
    	 * 
    	 * @param p 
    	 * @throws EntityInitializer.InitializationException
    	 */
		public UAIMServer_ControlLogic(Properties p) throws EntityInitializer.InitializationException {
            super(p);

            try {
                int port = new Integer(p.getProperty("idol.uaim.port")).intValue();
                channelFactory = P2PChannelFactory.newInstance(port, null);
            } catch (Exception e) {
                _LOG.error("UAIMServer_ControlLogic exception e is " + e);
                e.printStackTrace();
				throw new EntityInitializer.InitializationException(e);
            }
		}
		
		/**
		 * handle method processes the I/O object
		 * 
		 * @param sel
		 * @param st
		 */
		public void handle(Selectable sel, SignalType st) {
			_LOG.debug("UAIMServer_ControlLogic handle method");
	
            if (SignalType.READ == st) {
				_LOG.debug("UAIMServer_ControlLogic READ method");
                if (sel == channelFactory) {
					// Ding! Dong!  U-AIM calling!
					_LOG.debug("UAIMServer_ControlLogic SELECTABLE is channel factory method");
					Selectable selectable = (Selectable) sel.read();
                    channels.addElement(sel);
                } else if (sel instanceof SelectableFutureResult) {
					_LOG.debug("UAIMServer_ControlLogic SELECTABLE is SelectableFutureResult method");
					// Must read the Object or else sel will remain in the READABLE state
					Object obj = sel.read();
                } else { 
					_LOG.debug("UAIMServer_ControlLogic selectable is something we want to read method");
                    // anything else is the stuff I want to invoke READ on...
                    _LOG.debug("UAIMServer_ControlLogic handle READ signal type");            
                    Object obj = sel.read();
                    if (obj != null) {
						_LOG.debug("UAIMServer_ControlLogic handle method and Data = " + obj);
                    } else {
            	        _LOG.debug("UAIMServer_ControlLogic handle method and Data is NULL");
                    }
                }
            } else if (SignalType.ERROR == st) {
                _LOG.debug("UAIMServer_ControlLogic handle ERROR signal type");    
                // remove it from the channels vector - this channel is no longer valid      
                channels.remove(sel); 
            } else if (SignalType.WRITE == st) {
                _LOG.debug("UAIMServer_ControlLogic handle WRITE signal type");            
            }
		}

        protected void buildSelectableSet(SelectableSet ss) {
            ss.addSelectable(channelFactory, SignalType.READ, this);
            ss.addSelectable(channelFactory, SignalType.ERROR, this);
        
            // loop selectables to be added and add them now...
            for (int i=0; i<channels.size(); i++) {
                Selectable sel = (Selectable) channels.elementAt(i);
                ss.addSelectable(sel, SignalType.READ, this);
                ss.addSelectable(sel, SignalType.ERROR, this);
            }
        }

    }
 

}
