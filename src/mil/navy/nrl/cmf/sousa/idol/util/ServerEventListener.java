package mil.navy.nrl.cmf.sousa.idol.util;

import java.nio.channels.SocketChannel;

/**
   ServerEventListener
 */
public interface ServerEventListener
{
// sousa.idol.util.ServerEventListener

/**
   acceptEvent(SocketChannel)
   @methodtype handler
   @param chan .
 */
public void
acceptEvent(/*@ non_null */ SocketChannel chan);

/**
   exceptionEvent(Exception)
   @methodtype handler
   @param exception .
 */
public void
exceptionEvent(/*@ non_null */ Exception exception);
}; // ServerEventListener
