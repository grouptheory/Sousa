// File: MethodResponseMessage.java

package mil.navy.nrl.cmf.sousa;

import java.io.Serializable;

/**
 * A response message for SOUSA's Asynchronous RMI calls 
 */
public class MethodResponseMessage implements Serializable
{
    private static final long serialVersionUID = 1L;
    // DAVID: Consider making _token a java.rmi.server.UID.
    private final Object _token;
    private final Object _response; // XXX Must be Serializable
    private final Exception _exception;
    
    /**
     * Construct a response message for a successful given method call (specified
     * by a token).
     *
     * @param token the token identifying the call
     * @param response the return value of the method call at the remote process
     */
    public MethodResponseMessage(Object token, Object response) 
    {
	_token = token;
	_response = response;
	_exception = null;
    }
    
    /**
     * Construct a response message for an unsuccessful given method call (specified
     * by a token).
     *
     * @param token the token identifying the call
     * @param ex the exception at the remote process
     */
    public MethodResponseMessage(Object token, Exception ex) 
    {
	_token = token;
	_response = null;
	_exception = ex;
    }
    
    /**
     * Get the token
     * @return the token
     */
    public  Object getToken() 
    {
	return _token;
    }
    
    /**
     * Get the return value
     * @return the return value
     */
    public Object getResponse()
    {
	return _response;
    }
    
    /**
     * Get the exception (note that the stack trace is entirely at the
     * remote process, unlike standard synchronous RMI).
     * @return the exception which occurred at the remote process
     */
    public Exception getException()
    {
	return _exception;
    }
}
