// File: MethodInvocationMessage.java

package mil.navy.nrl.cmf.sousa;

import java.io.Serializable;

/**
 * The carrier of SOUSA's Asynchronous RMI requests
 */
public class MethodInvocationMessage implements Serializable
{
    private static final long serialVersionUID = 1L;
    private final String _signature;
    private final Object _token;
    private final Object[] _args;
    
    /**
     * Make a new ARMI message.
     *
     * @param signature the method signature
     * @param token to multiplex multiple calls to the same method
     * @param args the arguments to the method
    */
    public MethodInvocationMessage(String signature, Object token, Object[] args) 
    {
	_signature = signature;
	_token = token;
	_args = args;
    }
    
    /**
     * Get the signature in this ARMI message.
     * @return the signature as a String
    */
    public String getSignature() 
    {
	return _signature;
    }
    
    /**
     * Get the token in this ARMI message.
     * @return the token as an Object
    */
    public  Object getToken() 
    {
	return _token;
    }
    
    /**
     * Get the arguments in this ARMI message
     * @return the arguments as an Object array.
    */
    public Object[] getArgs() 
    {
	return _args;
    }
}
