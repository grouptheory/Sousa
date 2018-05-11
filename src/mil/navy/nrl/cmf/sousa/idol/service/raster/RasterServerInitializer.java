package mil.navy.nrl.cmf.sousa.idol.service.raster;

import java.io.Serializable;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Properties;

import mil.navy.nrl.cmf.sousa.*;
import mil.navy.nrl.cmf.sousa.idol.IdolInitializer;
import mil.navy.nrl.cmf.sousa.idol.service.ServerInitializer;
import mil.navy.nrl.cmf.sousa.spatiotemporal.*;
import mil.navy.nrl.cmf.sousa.util.StackTrace;
import mil.navy.nrl.cmf.sousa.util.Strings;

import org.apache.log4j.Logger;

/**
   ModelServerInitializer initializes an IDOL server that offers
   raster images.
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
public class RasterServerInitializer
    extends ServerInitializer
{
    private static final Logger _LOG = 
		Logger.getLogger(RasterServerInitializer.class);
    
    protected RasterQueryable _rasters = null;
    
	/**
	   <CODE>RasterServer_ControlLogic</CODE> extends the base class by
	   automatically fetching a
	   <CODE>SpatiotemporalViewInterpreter</CODE> from each new
	   client.
	 */
    public static class  RasterServer_ControlLogic
		extends ServerInitializer.Server_ControlLogic {
	
		public RasterServer_ControlLogic(Properties p) 
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
			RasterServerInitializer._LOG.warn("Scheduling connection");
			
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
					RasterServerInitializer._LOG.error(ex);
				}
			}
		}
    }
    
    public RasterServerInitializer(Properties p) 
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
		throws EntityInitializer.InitializationException {
	
		super.initialize_custom(p);
	
		try {
			_rasters = new RasterQueryable(p.getProperty("idol.pgsql.db"),
										   p.getProperty("idol.pgsql.query"));
			_LOG.warn("RasterQueryable initialized");
		} catch (SQLException ex) {
			String error = "RasterQueryable failed to initialize";
	    
			_LOG.error(new Strings(new Object[]{
				this, " caught exception ", ex, ":",
				StackTrace.formatStackTrace(ex)}));
			throw new InitializationException(error, ex);
		}

		State s = this.getState();
		s.addField(QueryFields.QUERYABLE_FIELDNAME, _rasters);
		s.addField(QueryFields.DESCRIPTION_FIELDNAME, 
				   "Imagery");
		this.addQoSClass(QueryViewInterpreter.class);
	}

	/**
	   Creates a RasterServer_ControLogic.
	 */
    protected ControlLogic initialize_makeControlLogic(Properties p)
		throws EntityInitializer.InitializationException {
		return new RasterServer_ControlLogic(p);
    }
}
