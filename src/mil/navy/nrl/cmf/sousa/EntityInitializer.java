// File: EntityInitializer.java

package mil.navy.nrl.cmf.sousa;

import java.util.Properties;
import java.util.List;
import java.util.Map;

/**
   <CODE>EntityInitializer</CODE> defines the parts that an Entity
   developer must code to initialize an Entity.  The typical way to
   use an EntityInitializer is shown in the code fragment below.

   <PRE>
		EntityInitializer initializer;
		...
		Entity e = new Entity(initializer.getContactPort(),
						      initializer.getMcastAddress(),
						      initializer.getMcastBasePort(),
						      initializer.getState(),
						      initializer.getControlLogic(),
						      initializer.getQoSClasses(),
						      initializer.getCommandLogics(),
							  initializer.getContentTypes());

		initializer.scheduleInitialFetches(e);
    </PRE>

	<EM>IMPLEMENTATION SUGGESTION:</EM> The constructor of a concrete
	class implementing this interface takes Properties as its only
	argument.  It throws an InitializationException in the event of
	failure.  The constructor parses the Properties to learn the
	initial values of an Entity's State; type of ControlLogic; initial
	servers from which to fetch Views; and other things.  The
	constructor throws
	<CODE>EntityInitializer.InitializationException</CODE> when there
	is some kind of initialization problem.
 */
public interface EntityInitializer 
{
	/**
	   An implementation of EntityInitializer may throw
	   InitializationException.  It is intended for use by a
	   constructor that reads the Entity's initial values from a
	   {@link java.util.Properties} object.
	 */
	static final class InitializationException 
		extends java.rmi.activation.ActivationException {

		public InitializationException(String s, Throwable t) {
			super(s, t);
		}

		public InitializationException(String s) {
			super(s);
		}
	}

	/**
	   Returns the <code>ControlLogic</code>

	   @return the <code>ControlLogic</code>
	 */
	public ControlLogic getControlLogic();

	/**
	   Returns the <code>State</code> populated with all fields and
	   their initial values.

	   @return the state of the Entity
	 */
	public State getState();

	/**
	   Returns the contact port.  The Entity listens at the contact
	   port on all interfaces.

	   @return the contact port
	 */
	public int getContactPort();

	/**
	   Returns the <code>String</code> representation of the multicast
	   address.  It can be null.  It can be a point-to-point address.  The
	   Entity uses multicast to transmit state changes to its clients.

	   @return the multicast address
	 */
	public String getMcastAddress();

	/**
	   The Entity uses a range of ports on its multicast address.
	   Using a range of ports permits the Entity to multicast
	   different qualities of service to its clients.  The port
	   numbers begin with the value returned by getMcastBasePort() and
	   increases thereafter with each new Projector.

	   @return the starting port number.
	 */
	public int getMcastBasePort();

	/**
	   Returns the <code>List</code> of <code>CommandLogic</code>.

	   @return the list of <code>CommandLogic</code>
	 */

        public List getCommandLogics();

	/**
	   Returns the <code>List</code> of <code>Class</code>, with each
	   member representing a piece of the quality of service offered
	   by the Entity.

	   @return the list of quality of service <code>Class</code> objects
	*/
	public List getQoSClasses();


	/**
	 Returns the <code>Map</code> from <code>String</code> RFC2045
	 MIME Content-Type to <code>Class</code> of the
	 <code>Renderer</code> that puts the Content-Type into a
	 <code>Renderable</code>.

	 @return the Content-Type--Renderer map.
	*/
	public Map getContentTypes();

    /**
       Tells the run-time to fetch <code>Receptors</code> for an
       Entity.
	   
	   @param e the Entity
    */
	public void scheduleInitialFetches(Entity e);
}
