package mil.navy.nrl.cmf.sousa.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;
import java.util.HashSet;
import java.util.Properties;

public final class SetFactory implements ObjectFactory
{
	public SetFactory() {};

	// Constructs Set of some type.  Assumes a constructor for type
	// that takes a String.
	//
	//
	// These are the suffixes that may be attached to prefix in p:
	//
	// Suffix		Type	Meaning
	// ------		----	-------
	// .class		String	fully qualified name of the class to instantiate
	//						It defaults to String.
	//
	// .size		integer	number of members of the Set.  The default value is 0.
	//

	// .element.0	string	an instance of the class constructed from this
	// 						String or from "" if there is no entry in p named 
	//						prefix+".element.0".
	// ...
	// .element.n-1 string	Where size==(n-1).  As above.
	//
	public Object create(String prefix, Properties p)
		throws IllegalArgumentException 
	{
		Set answer = new HashSet();

		String className = p.getProperty(prefix + ".class", "java.lang.String");

		try {
			Class cls = Class.forName(className);
			Constructor ctor = cls.getConstructor(new Class[] {java.lang.String.class});

			String sizeVal = p.getProperty(prefix + ".size", "0");
			int size = intVal(prefix, ".size", "0", p, 0, Integer.MAX_VALUE);

			for (int i=0; i < size; i++) {
				String objVal = p.getProperty(prefix + ".element." + i, "");
				try {
					Object obj = ctor.newInstance(new Object[]{objVal});

					// DAVID: Consider catching Exceptions thrown by
					// Set.add().
					answer.add(obj);
				} catch (IllegalAccessException ex) {
					throw new IllegalArgumentException("Instantiating " + prefix + ".element. " + i + ":" + ex);
				} catch (IllegalArgumentException ex) {
					throw new IllegalArgumentException("Instantiating " + prefix + ".element. " + i + ":" + ex);
				} catch (InstantiationException ex) {
					throw new IllegalArgumentException("Instantiating " + prefix + ".element. " + i + ":" + ex);
				} catch (InvocationTargetException ex) {
					throw new IllegalArgumentException("Instantiating " + prefix + ".element. " + i + ":" + ex);
				} catch (ExceptionInInitializerError ex) {
					throw new IllegalArgumentException("Instantiating " + prefix + ".element. " + i + ":" + ex);
				}
			}
		} catch (ClassNotFoundException ex) {
			throw new IllegalArgumentException("No such class " + className);
		} catch (NoSuchMethodException ex) {
			throw new IllegalArgumentException("No constructor takes a String in " + className);
		}

		return answer;
	}

	// DAVID: this was copied whole from CalendarFactory.  Consider
	// refactoring.
	private int intVal(String prefix, String fieldName, String defaultVal,
						   Properties p, int minVal, int maxVal) 
		throws IllegalArgumentException
	{
		int answer = 0;
		String val = p.getProperty(prefix + fieldName, defaultVal);

		try {
			answer = Integer.parseInt(val);
			if ((answer < minVal) || (answer > maxVal))
				throw new IllegalArgumentException("Expecting " + minVal + "-" + maxVal + " for " + 
												   prefix + fieldName + " but found " + 
												   val + " instead.");
		} catch (NumberFormatException ex) {
				throw new IllegalArgumentException("Expecting " + minVal + "-" + maxVal + " for " + 
												   prefix + fieldName + " but found " + 
												   val + " instead.");
		}

		return answer;
	}
}
