package mil.navy.nrl.cmf.annotation;

import mil.navy.nrl.cmf.sousa.*;
import java.util.*;
import mil.navy.nrl.cmf.sousa.spatiotemporal.SpatiotemporalViewInterpreter;

public class ClientVI extends SpatiotemporalViewInterpreter {

    public ClientVI(State s) {
	super(s);
	System.out.println("ANNOT: Made ClientVI");
    }
};

