package mil.navy.nrl.cmf.sousa.idol.user;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Properties;

import mil.navy.nrl.cmf.sousa.*;
import mil.navy.nrl.cmf.sousa.idol.IdolInitializer;
import mil.navy.nrl.cmf.sousa.spatiotemporal.*;
import mil.navy.nrl.cmf.sousa.util.Strings;

import org.apache.log4j.Logger;

/**
 * ConsoleBrowserInitializer initializes a console-oriented browser
 * Entity.
 */
public class ConsoleBrowserInitializer extends IdolInitializer
{
    private static final Logger _LOG = 
		Logger.getLogger(ConsoleBrowserInitializer.class);
    
    public ConsoleBrowserInitializer(Properties p)
		throws EntityInitializer.InitializationException
    {
		super(p);
    }
    
    // IdolInitializer
    
    /**
       PURPOSE: add any additional fields custom to this particular
       initializer and perform any other custom initializations.
    */
    protected void initialize_custom(Properties p) 
		throws EntityInitializer.InitializationException {
    }

    // PURPOSE: make the control logic.
    protected ControlLogic initialize_makeControlLogic(Properties p)
		throws EntityInitializer.InitializationException {
		return new IdolInitializer.Console_ControlLogic(p);
    }
}
