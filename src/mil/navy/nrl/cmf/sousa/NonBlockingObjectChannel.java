package mil.navy.nrl.cmf.sousa;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

//import org.apache.log4j.*;

/**
 * A Selectable object which is backed by an NIO channel.
 */
abstract class NonBlockingObjectChannel extends Selectable {
	// sensitive to nothing
	static final int OP_NONE = 0;

    // the selector for low-level Java NIO.
    protected Selector _selector = null;

    /* 
     * PURPOSE: Construct a new NonBlockingObjectChannel
     */
    protected NonBlockingObjectChannel() {
    }

    /* 
     * PURPOSE: Register this Channel with a Selector.
     * PRECONDITION: selector!=null.
     * POSTCONDITION: This Channel is registered with the
     * specified selector.
     */
    void register(Selector selector) {
	// save the Selector
	_selector = selector;
    }

    /* 
     * PURPOSE: Deregister this ChannelHandler with a Selector.
     * PRECONDITION: this Channel has been registered with a other
     * Selector.
     * POSTCONDITION: This ChannelHandler is registered with the
     * specified selector.
     */
    void deregister() {
    }

    /* 
     * PURPOSE: Give the NonBlockingObjectChannel time to handle the event.
     * PRECONDITION: key.associated-data==this
     * POSTCONDITION: implementation specific.
     */
    abstract void handle(SelectionKey key);

    //// over-rides to Selectable methods

    /* 
     * PURPOSE: Get an Object from the _inQ, since it has been
     * completely read and reconstituted.
     * PRECONDITION: select() indicated that this NonBlockingObjectChannel is readable
     * POSTCONDITION: Return a reconstituted object, or null if _inQ
     * is empty.
     */
    public final Object read() {
	boolean wakeup = (spaceRead()==0);
	Object ser = super.read();
	// full to non-full transition makes us want to read
	if (wakeup && (_selector!=null)) {
	    _selector.wakeup();
	}
	return ser;
    }
    
    /* 
     * PURPOSE: Put an Object into the _outQ, in preparation for writing.
     * PRECONDITION: ser!=null.
     * POSTCONDITION: Enqueues an Object for writing.
     */
    public final void write(Object ser) {
	boolean wakeup = (dataWrite()==0);
	super.write(ser);
	// empty to non-empty transition makes us want to write
	if (wakeup && (_selector!=null)) {
	    _selector.wakeup();
	}
    }

}
