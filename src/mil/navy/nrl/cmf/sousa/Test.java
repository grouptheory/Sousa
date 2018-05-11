package mil.navy.nrl.cmf.sousa;

import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;

/**
 * Testing and examples of two kinds of Entity: client and
 * server.
 *
 * An Entity maintains collections other Entities in two ways. It
 * keeps track of the Entities that it can see in its Focus.  The
 * Focus is a collection of Receptors.  A Receptor is a local cache of
 * a remote Entity's state along with methods that may be invoked on
 * the remote Entity.
 *
 * An Entity keeps track of the Entities that can see it in its
 * Nimbus.  The Nimbus is a collection of Projectors.  A Projector
 * sends snapshots of the Entity's state to one or more Receptors held
 * by remote Entities.  The state projected by the Projector is the
 * Entity's state modulated by the ViewInterpreters requested by a
 * remote Entity.
 *
 *
 * An Entity has a contact port.  It listens on that port at all
 * network interfaces.
 *
 * An Entity has a ControlLogic.  It provides the Entity with a
 * specialized way to initiate and react to events.  The events
 * include the arrival and departure of a client, the arrival and
 * departure of a connection to a server, and generic I/O events.
 *
 * An Entity may have an authoritative state.  This is where it keeps
 * the information that it may wish to share with its clients.
 * Clients attach ViewInterpreters to the authoritative state.  The
 * ViewInterpreters adapt the values of the state Fields into state
 * Fields that the clients can understand.
 *
 * An Entity may have a multicast address and base port.  Only server
 * Entities must have them.  They are used to send state change
 * notifications to clients using a reliable multicast protocol.  The
 * first Projector that can be shared by clients will use the address
 * mcastAddress:basePort.  The next will use mcastAddress:basePort+1.
 * Each unique shareable Projector will get its own address and port.
 * When a Projector's last client disconnects, the port will be
 * reclaimed by the Entity for reuse.
 *
 * A server Entity may provide a number of functions to its clients via
 * RMI.  An external Object provides those functions and the functions
 * are determined by the interfaces that the Object implements.
 *
 * A client Entity may request a Receptor that supports a QoS from a
 * server Entity.  The Receptor will contain a State that has the
 * Fields of each ViewInterpreter Class contained by the QoS.  The
 * Receptor will offer the RMI functions of the server.  The RMI
 * functions available are those of the interfaces Classes contained
 * in the QoS and those of the interfaces implemented by the
 * ViewInterpreters that also implement the tagging interface
 * CommandLogic.
 *
 */
public final class Test {
	
    private final static int NUM_CONNECTIONS = 1;

	/**
	   This is an example of an interface for a command logic.
	   All methods must return Object.
	   <P>
	   A java.lang.reflect.Proxy, which may be obtained from a
	   Receptor, provides the apparent implementation of this
	   interface.  It is backed by a
	   java.lang.reflect.InvocationHandler, which is the Receptor,
	   whose invoke() dispatches calls of the interface's methods to a
	   remote server.  The InvocationHandler returns a
	   SelectableFutureResult.
	   <P>
	   When the server returns an answer, the InvocationHandler sets
	   the real value of the SelectableFutureResult.  The type of that
	   value is Number.
	*/
	public interface Incrementor {
		// Returns i + 1;
		//
		// By convention, the actual return type is Number.
		public Object increment(Number i);
	}

	/**
	   This is an example of a command logic.  It's an Object that
	   implements one or more interfaces.  All of its methods are
	   available to clients via RMI.
	*/
	public static class IntegerIncrementor implements Incrementor {
		// Returns i + 1;
		public Object increment(Number i) {
			int iVal = i.intValue();
			return new Integer(iVal + 1);
		}
	}

	/**
	   This is an example of a ViewInterpreter.  It can adapt the
	   server state for the client.  If the ViewInterpreter implements
	   the ComandLogic interface, then all of the methods of all of
	   the interfaces the ViewInterpreter implements are available to
	   clients via RMI.
	   <P>
	   This example adapts server state by appending "_BAR" to the
	   value of each of the server's Fields.  The name of a field is
	   its name in the server state concatentated with "_Bar".  The
	   interpret() method performs the adaptation.
	   <P>
	   The framework also adapts the names of the fields.  The name of
	   each field in the projected by the ViewInterpreter is the
	   concatenation of the class name of the ViewInterpreter with an
	   underscore and the name of the field created by the
	   ViewInterpreter.
	   <P>	
	   In this example, the class name is
	   "sousa.Test$TestViewInterpreter".  One of the Fields projected
	   by the ViewInterpreter is "Foo_Bar".  Thus, the name of that
	   Field at the client is
	   "sousa.Test$TestViewInterpreter_Foo_Bar".
	*/
	public static class TestViewInterpreter extends ViewInterpreter {
		public TestViewInterpreter(State s) {
			super(s);
			HashSet fields = new HashSet();
			fields.add("Foo");
			s.attachFieldListener(fields, this);
		}
		
		public boolean isDirty(State parameters){
			return true;
		}

		// interpret() adapts the server state into client state.  It
		// may return null, indicating no changes to the client state.
		// This implementation ignores parameters.
		protected State.ChangeMessage interpret(List fieldChanges, 
												State parameters) {
			// Map the Field.ChangeMessages from the Entity State
			// into Field.ChangeMessages for the Projector State.
			State.ChangeMessage answer = new State.ChangeMessage();

			// Append "_BAR" to each of the Entity's Field values
			if (null != fieldChanges) {
				for (Iterator i= fieldChanges.iterator(); i.hasNext(); ) {
					Field.ChangeMessage entityFCM = (Field.ChangeMessage)i.next();
					String barValue = entityFCM._value.toString() + "_BAR";
					Field.ChangeMessage projectorFCM = 
						new Field.ChangeMessage(entityFCM._fname + "_Bar", barValue);

					System.out.println("TestViewInterpreter.interpret(): " +
									   projectorFCM._fname + "=" +
									   projectorFCM._value);

					answer.addFieldChangeMessage(projectorFCM);
				}
			}

			return answer;
		}
	}

	/**
	   An example of ControlLogic for a client
	 */
	public static class ClientControlLogic extends ControlLogic {
		public ClientControlLogic() {
		}

		public void projectorReadyIndication(ServerSideFSM fsm) {
			System.out.print("ERROR! " );
			super.projectorReadyIndication(fsm);
		}

		public void projectorNotReadyIndication(ServerSideFSM fsm) {
			System.out.print("ERROR! " );
			super.projectorNotReadyIndication(fsm);
		}

		/**
		   When a Receptor is ready, create a dynamic proxy for all of
		   its interfaces.  Invoke a method of the proxy.  In this
		   case, the Receptor has only one interface, Incrementor.
		   <P>
		   ControlLogic.handle() will be called when the
		   SelectableFutureResult returned by the proxy has a value or
		   an Exception.
		*/
		public void receptorReadyIndication(ClientSideFSM fsm) {
			System.out.print("ClientControlLogic says: ");
			super.receptorReadyIndication(fsm);

			Receptor r = fsm.getReceptor();
			// Prints the initial state of the Receptor.
			r.print();

			// This is the way to obtain a Proxy that implements the
			// Incrementor interface.
			Incrementor view = (Incrementor)fsm.genProxy(Incrementor.class);

			Integer i = new Integer(32767);
			//
			// This is the way to invoke an Incrementor method of the
			// proxy.
			//
			// answer is really of type SelectableFutureResult.
			// Receptor.invoke(), the code backing the Proxy, gives
			// the Entity a reference to answer, so we need not keep
			// one ourselves.  The Entity's handle(Selectable,
			// SignalType) method will be informed when answer has
			// either a value or an Exception.  Entity.handle()
			// delegates to the ControlLogic's handle(Selectable,
			// SignalType) to process the result.
			//
			// ClientControlLogic uses ControlLogic.handle(), which
			// simply prints the value of the result or Exception.
			Object answer = view.increment(i);
		}

		/**
		   The server disconnected.
		*/
		public void receptorNotReadyIndication(ClientSideFSM fsm) {
			super.receptorNotReadyIndication(fsm);
		}
	}

	/**
	   An example of ControlLogic for a server
	 */
	public static class ServerControlLogic extends ControlLogic 
		implements Clock.AlarmHandler {

		private Clock.Alarm _alarm = null;
		private int _clients = 0;

		public ServerControlLogic() {
		}
		
		/**
		   Notification from the Nimbus that a client is ready.
		   Begin changing the state when the first client arrives.
		*/
		public void projectorReadyIndication(ServerSideFSM fsm) {
			super.projectorReadyIndication(fsm);
			// start timer
			_clients ++;

			if (1 == _clients) {
				Clock c = getEntity().getClock();
				_alarm = c.setAlarm(1000 /* period in milliseconds */, 
									true /* recurring or not */, 
									null /* user data for AlarmHandler.handle() */,
									this /* the Clock.AlarmHandler */);
			}
		}

		/**
		   Notification from the Nimbus that a client has departed.
		   Stop changing the state whenever there are no clients.
		*/
		public void projectorNotReadyIndication(ServerSideFSM fsm) {
			super.projectorNotReadyIndication(fsm);
			_clients --;

			// It's unlikely that _clients will be less than zero.
			if ((_clients <= 0) && (null != _alarm)) {
				// stop timer
				_alarm.disable();
			}
		}

		/**
		   This server will never be a client, so it doesn't care
		   about Receptors.
		*/
		public void receptorReadyIndication(ClientSideFSM fsm) {
			System.out.print("ERROR! " );
			super.receptorReadyIndication(fsm);
		}

		public void receptorNotReadyIndication(ClientSideFSM fsm) {
			System.out.print("ERROR! " );
			super.receptorNotReadyIndication(fsm);
		}

		//// Clock.AlarmHandler

		/**
		   An Alarm expired.  Increment the value of the Foo
		   field by one.  Reschedule the Alarm.
		*/
		public void handle(Clock.Alarm m) {
			if (m == _alarm) {
				State authoritativeState = getEntity().getState();
				try {
					Long oldValue = (Long)authoritativeState.getField("Foo");
					Long newValue = new Long(oldValue.longValue() + 1);
					authoritativeState.setField("Foo", newValue);
					System.out.println("ServerControlLogic: sets field Foo="+newValue);

					// The Alarm was created to be recurring.  Calling
					// enable() gives the scheduler permission to
					// reschedule the Alarm.
					m.enable();
				} catch (NoSuchFieldException ex) {
					System.out.println("handle(" + m + "): " + ex);
				}
			} else {
				System.out.println("ServerControlLogic.handle(" +
								   m + "): unknown Clock.Alarm");
			}
		}
	}



    public static void main(String[] args) {
		
		try {
			if (args.length < 1) {
				usage();
			}
			
			if (args[0].equals("s")) {
				int contactPort = Integer.parseInt(args[1]);
				String mcastAddress = args[2];
				// The first Projector that requires multicast will
				// write to mcastAddress:mcastBasePort.  The second
				// will write to mcastAddress:mcastBasePort + 1.  Each
				// new Projector that requires multicast will use the
				// next port.  When such a Projector's last client
				// disconnects, the Projector returns the multicast
				// port to the Entity for reuse.
				int mcastBasePort = Integer.parseInt(args[3]);

				// The server State has one field, "Foo", which
				// contains a Long.
				State authoritativeState = new State();
				authoritativeState.addField("Foo", new Long(1L));

				// The server as a single command logic.  Each method
				// of each interface it implements is available to
				// clients via RMI.
				IntegerIncrementor commandLogic = new IntegerIncrementor();
				LinkedList commandLogicList = new LinkedList();
				commandLogicList.add(commandLogic);

				// See also, the ServerControlLogic, above.
				Entity e = new Entity(contactPort, 
									  mcastAddress, mcastBasePort,
									  authoritativeState, 
									  new ServerControlLogic(),
									  null, commandLogicList, null);
			}
			else if (args[0].equals("c")) {
				int contactPort = Integer.parseInt(args[1]);
				InetAddress remoteAddress = InetAddress.getByName(args[2]);
				int remoteport = Integer.parseInt(args[3]);
				// The client has no State and no multicast address.
				// It's only a client.  It can't act as a server.  All
				// it has is a contact port and a ClientControlLogic.
				Entity e = new Entity(contactPort, 
									  null /* no mcast address */,
									  -1 /* no mcast port */, 
									  null /* no state */, 
									  new ClientControlLogic(),
									  null, null, null);

				// The QoS ctor takes an integer that identifies the
				// "session" at the server.  By convention, negative
				// session numbers indicate unsharable sessions; that
				// is, sessions that have their own Projectors with
				// precisely one client.  Such a Projector
				// communicates with its Receptor over the
				// point-to-point control channel.
				//
				// Non-negative session numbers indicate sharable
				// sessions; that is, sessions whose Projectors may
				// have more than one client.  Such a Projector sends
				// State.ChangeMessages to its clients over a multicat
				// data channel.  It receives RMI requests from and
				// sends RMI replies to a client over a point-to-point
				// control channel.  There is one such control channel
				// per client.
				QoS qos = new QoS(0);

				// The server must create a Projector that has a
				// TestViewInterpreter.
				qos.add(TestViewInterpreter.class);

				// The server must return a Receptor that implmements
				// the Incrementor interface.
				qos.add(Incrementor.class);

				// The Entity will fetch a Receptor from the server
				// when it gets time.

				try {
					e.scheduleConnectTo(new ServerContact(remoteAddress, 
														  remoteport, qos));
					/*
					  e.scheduleConnectTo(new ServerContact(remoteAddress, 
					  remoteport, null));
					*/
				}
				catch (UnknownHostException ex) {
					System.out.println("No ServerContact! " + ex);
				}
			}
			else {
				usage();
			}
			Thread.sleep(10000);
		}
		catch (Exception ex) {
			System.out.println("Exception: "+ex);
			ex.printStackTrace();
		}	
    }
	
    private static void usage() {
		System.out.println("Usage: Test s <localport> <mcast address> <initial mcast port>");
		System.out.println("       Test c <localport> <server host name> <remoteport>");
		System.out.println();
		System.out.println("Set these Loggers to DEBUG in log4j.properties:");
		System.out.println("log4j.category.mil.navy.nrl.cmf.sousa.Executor to observe RMI on the server");
		System.out.println("log4j.category.mil.navy.nrl.cmf.sousa.ControlLogic to observe the results of RMI");
		System.out.println("log4j.category.mil.navy.nrl.cmf.sousa.Receptor to observe Receptor State changes");

		System.exit(-1);
    }
}


