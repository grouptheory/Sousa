package mil.navy.nrl.cmf.annotation;

import mil.navy.nrl.cmf.sousa.*;
import java.util.Calendar;
import mil.navy.nrl.cmf.sousa.spatiotemporal.*;

public interface ServerSI {
    public Object annotate(String note, 
			   Vector3d position, 
			   Calendar lower, Calendar upper);
};

 
