package mil.navy.nrl.cmf.annotation;

import mil.navy.nrl.cmf.sousa.*;
import mil.navy.nrl.cmf.sousa.spatiotemporal.*;
import java.util.*;
import java.net.*;
	
public class ServerLogic extends ControlLogic {

    /**
       Fetches a View of the client that supports {@link
       mil.navy.nrl.cmf.sousa.spatiotemporal.SpatiotemporalViewInterpreter}.
    */
    public void projectorReadyIndication(ServerSideFSM fsm) {
		super.projectorReadyIndication(fsm);
		System.out.println("Projector ready.");
	
		ServerContact clientContact = fsm.getClientContact();

		for (Iterator it = clientContact.getQoS().iterator(); it.hasNext();) {
			Class specificClass = (Class)it.next();
			System.out.println("class: "+specificClass);
			if (SpatiotemporalViewInterpreter.class.isAssignableFrom(specificClass)) {
				QoS q = new QoS(0);
				//				q.add(ClientVI.class);
				q.add(SpatiotemporalViewInterpreter.class);
				try {
					System.out.println("Scheduling reverse fetch");
					ServerContact c = new ServerContact(clientContact.getHost(),
														clientContact.getPort(),
														q);
					getEntity().scheduleConnectTo(c);
					System.out.println("done scheduling");
				} catch (UnknownHostException ex) {
					System.out.println("Error "+ex);
				}
				// only one reverse fetch is needed
				break;
			}
		}
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
	}
    /**
     */
    public void receptorNotReadyIndication(ClientSideFSM fsm) {
		super.receptorNotReadyIndication(fsm);
		System.out.println("Receptor NOT ready.");
	}

};


