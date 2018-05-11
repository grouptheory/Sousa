// File: SelectableFutureResult.java
package mil.navy.nrl.cmf.sousa;

import java.rmi.server.UID;

/**
 * The result of an ARMI call, which gets filled in when the result is
 * available.
 */
public class SelectableFutureResult extends Selectable 
{
    UID _uid = new UID();
    
    /**
     * Make a new SelectableFutureResult
    */
    public SelectableFutureResult() 
    {
    }
    
    /**
     * Set the return value when it arrived via the response message.
     * @param newValue the return value
    */
    public synchronized void set(Object newValue) {
	put_inQ(newValue);
	broadcastNotification(SignalType.READ);
    }
    
    /**
     * is the SelectableFutureResult closed?
     *
     * @return true or false depending on whether an answer has been obtained.
    */
    boolean isClosed() {
	return false;
    }
    
    /**
     * consider this SFR closed.
    */
    void close() {
    }
    
    /**
     * get the UID associated with this SFR.
     * @return the UID
    */
    public UID getUID() {
	return _uid;
    }
}
