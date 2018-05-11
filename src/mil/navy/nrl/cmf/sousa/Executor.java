package mil.navy.nrl.cmf.sousa;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import mil.navy.nrl.cmf.sousa.util.Strings;
import org.apache.log4j.Logger;

/**
 * An Executor accepts requests to execute a method based on
 * signature.  It's an implementation of the mediator design pattern.
 */
public class Executor {
    private static final Logger _LOG = 
		Logger.getLogger(Executor.class);
    
    // Command logic
    //
    // _methods maps the String signature of a Method into the Method.
    private final Map _methods = new HashMap(); // String->Method
    
    // _commandLogics maps the String signature of a Method into the
    // Object that implements the Method.
    private final Map _commandLogics = new HashMap(); // String->Object
    
    private final Map _signatures = new HashMap(); // Class->Set of String
    
    /**
     *  Constructor to make an Executor with no CommandLogics
     */
    public Executor() {
    }
    
    /**
     * Execute a method given a signature and an array of argument Objects
     * @param signature the signature of the method to be executed
     * @param args an array of argument objects
     * @return the return value of calling the method with the specified arguments
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     */
    public Object execute(String signature, Object[] args)
		throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		Object answer = null;
		Method method = (Method)_methods.get(signature);
	
		_LOG.debug(new Strings(new Object[] 
			{this, ":execute(", signature, ", ...) method=", method}));
	
		if (null != method) {
			// Assumption: the server does not defer execution
			Object cl = _commandLogics.get(signature);
			answer = method.invoke(cl, args);
	    
			_LOG.debug(new Strings(new Object[] 
				{this, ":execute(", signature, ", ...) returns ", answer}));
	    
	    
		} else {
			throw new NoSuchMethodException(signature);
		}
		return answer;
    }
    
    /** 
	 * <P>
     * Give this Executor methods that its clients can invoke.
	 * </P>
	 * <P>
     * <CODE>add</CODE> permits no duplicate methods in this Executor.
	 * If any of <CODE>executable's</CODE> methods have the same 
	 * signature as another Object already known to this Executor, then
	 * <CODE>add</CODE> inserts none of <CODE>executable's</CODE> methods
	 * into this Executor.
	 *
     * @param executable an object implementing some number of interfaces
	 * @throws IllegalArgumentException when <code>executable</code>
	 * has a method whose signature is already known to this Executor
     */
    public void add(Object executable) 
		throws IllegalArgumentException {
		Class[] interfaces = executable.getClass().getInterfaces();

		for (int i=0; i < interfaces.length; i++) {
			Method[] methods = interfaces[i].getMethods();

			for (int j=0; j < methods.length; j++) {
				String signature = methods[j].toString();

				if (null != _methods.put(signature, methods[j])) {
					throw new IllegalArgumentException("Duplicate signature: "
													   + signature);
				}
			}

		}

		for (int i=0; i < interfaces.length; i++) {
			Method[] methods = interfaces[i].getMethods();

			for (int j=0; j < methods.length; j++) {
				String signature = methods[j].toString();

				_commandLogics.put(signature, executable);
				addSignature(interfaces[i], signature);

				_LOG.debug(new Strings(new Object[] 
					{this, ":addCommandLogic(): Class=", interfaces[i], 
					 ", Signature=", signature, 
					 ", Method=", methods[j], 
					 ", Object=", executable}));
			}
		}
    }
    
    /**
     * Test if a signature is executable
     * @param signature the signature of a method as a String
     */
    public boolean canExecute(String signature) {
		return _methods.containsKey(signature);
    }
    
    /**
     * Return a set of signatures of methods of a class
     * @param c the class
     * @return a Set of Strings representing signatures of methods
     */
    public Set getSignatures(Class c) {
		return Collections.unmodifiableSet((Set)_signatures.get(c));
    }
    
    /**
     * Get the Set of interfaces supported by this Executor
     * @return a Set of Class objects
     */
    public Set getInterfaces() {
		return Collections.unmodifiableSet(_signatures.keySet());
    }
    
    /**
     * Add a signature, bound to an Object of a Class implementing a
     * method having that signature
     * @param c the class
     * @param signature the method signature
     */
    private final void addSignature(Class c, String signature) {
		Set signatures = (Set)_signatures.get(c);
		if (null == signatures) {
			signatures = new HashSet();
			_signatures.put(c, signatures);
		}
		signatures.add(signature);
    }

	/**
	 * Tests the duplicate signature rejection feature of Executor.
	 */
	public static void main(String[] args) {
		Executor e = new Executor();
		Object obj;

		obj = new java.util.LinkedList();

		System.out.println("Adding first Object: " + obj);
		System.out.println("Expect no errors");

		try {
			e.add(obj);
		} catch (IllegalArgumentException ex) {
			System.err.println(ex);
		}

		obj = new java.util.ArrayList();
		System.out.println("Adding second Object: " + obj);
		System.out.println("Expect an error");
		try {
			e.add(obj);
		} catch (IllegalArgumentException ex) {
			System.err.println(ex);
		}

		obj = new java.util.HashMap();

		System.out.println("Adding third Object: " + obj);
		System.out.println("Expect no errors");

		try {
			e.add(obj);
		} catch (IllegalArgumentException ex) {
			System.err.println(ex);
		}

	}
}
