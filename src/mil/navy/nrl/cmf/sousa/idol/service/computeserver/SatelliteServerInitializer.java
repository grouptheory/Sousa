package mil.navy.nrl.cmf.sousa.idol.service.computeserver;

import java.io.Serializable;
import java.net.UnknownHostException;
import java.util.Properties;
import mil.navy.nrl.cmf.sousa.*;
import mil.navy.nrl.cmf.sousa.idol.service.ServerInitializer;
import mil.navy.nrl.cmf.sousa.spatiotemporal.*;
import mil.navy.nrl.cmf.sousa.util.StackTrace;
import mil.navy.nrl.cmf.sousa.util.Strings;
import org.apache.log4j.Logger;

/**
   SatelliteServerInitializer initializes an IDOL server that offers
   the locations of satellites in orbit.
   <P>

   <EM>All of these implementations are similar:
   <UL>
   <LI>{@link
   mil.navy.nrl.cmf.sousa.idol.service.city.CityServerInitializer}
   <LI>{@link
   mil.navy.nrl.cmf.sousa.idol.service.model.ModelServerInitializer}
   <LI>{@link
   mil.navy.nrl.cmf.sousa.idol.service.raster.RasterServerInitializer}
   <LI>{@link
   mil.navy.nrl.cmf.sousa.idol.service.computeserver.SatelliteServerInitializer}
   <LI>{@link
   mil.navy.nrl.cmf.sousa.idol.service.trail.TrailServerInitializer}
   </UL>
   <P>

   <EM> They differ in the description they give to the directory server
   and in the {@link mil.navy.nrl.cmf.sousa.spatiotemporal.Queryable}
   they use to perform spatiotemporal searches.
   <P>

   <EM>Consider refactoring these classes.</EM>
*/
public class SatelliteServerInitializer
	extends ServerInitializer
{
	static final Logger _LOG = 
		Logger.getLogger(SatelliteServerInitializer.class);
    
	/**
	   <CODE>SatelliteServer_ControlLogic</CODE> extends the base class by
	   automatically fetching a
	   <CODE>SpatiotemporalViewInterpreter</CODE> from each new
	   client.
	 */
    public static class  SatelliteServer_ControlLogic
		extends ServerInitializer.Server_ControlLogic {
	
		public SatelliteServer_ControlLogic(Properties p) 
			throws EntityInitializer.InitializationException
		{
			super(p);
		}
	
		/**
		   Fetches a View of the client that supports {@link
		   mil.navy.nrl.cmf.sousa.spatiotemporal.SpatiotemporalViewInterpreter}.
		*/
		public void projectorReadyIndication(ServerSideFSM fsm) {
			super.projectorReadyIndication(fsm);
			SatelliteServerInitializer._LOG.warn("Scheduling connection");
			
			ServerContact clientContact = fsm.getClientContact();

			if (clientContact.getQoS().contains(SpatiotemporalViewInterpreter.class)) {
				QoS q = new QoS(0);
				q.add(SpatiotemporalViewInterpreter.class);
				try {
					ServerContact c = new ServerContact(clientContact.getHost(),
														clientContact.getPort(),
														q);
					getEntity().scheduleConnectTo(c);
				} catch (UnknownHostException ex) {
					SatelliteServerInitializer._LOG.error(ex);
				}
			}
		}
    }

	public SatelliteServerInitializer(Properties p)
		throws EntityInitializer.InitializationException 
    {
		super(p);
    }

    /**
	   Adds the {@link
	   mil.navy.nrl.cmf.sousa.spatiotemporal.Queryable} (in {@link
	   mil.navy.nrl.cmf.sousa.spatiotemporal.QueryFields#QUERYABLE_FIELDNAME})
	   and the description (in {@link
	   mil.navy.nrl.cmf.sousa.spatiotemporal.QueryFields#DESCRIPTION_FIELDNAME})
	   to the State and {@link
	   mil.navy.nrl.cmf.sousa.spatiotemporal.QueryViewInterpreter} to
	   the {@link mil.navy.nrl.cmf.sousa.QoS}.
    */
	protected void initialize_custom(Properties p)
		throws EntityInitializer.InitializationException
	{
		super.initialize_custom(p);

		//** AuthoritativeState ************************************
		SatelliteDB satellites = new SatelliteDB(p.getProperty("idol.satellite.name"),
												 p.getProperty("idol.satellite.db.url"),
												 p.getProperty("idol.satellite.db.class"),
												 p.getProperty("idol.satellite.query"));

		try {
			satellites.initialize();
			_LOG.warn("CalcDB intialized");
		} catch (java.util.prefs.BackingStoreException ex) {
			String error = "SatelliteDB failed to initialize";

			_LOG.error(new Strings(new Object[]{
				this, " caught exception ", ex, ":",
				StackTrace.formatStackTrace(ex)}));
			throw new InitializationException(error, ex);
		}

		State s = this.getState();
		s.addField(QueryFields.QUERYABLE_FIELDNAME, satellites);
		s.addField(QueryFields.DESCRIPTION_FIELDNAME, 
				   "Satellites");
		this.addQoSClass(QueryViewInterpreter.class);
	}

	/**
	   Creates a SatelliteServer_ControLogic.
	 */
    protected ControlLogic initialize_makeControlLogic(Properties p)
		throws EntityInitializer.InitializationException {
		return new SatelliteServer_ControlLogic(p);
    }
}
