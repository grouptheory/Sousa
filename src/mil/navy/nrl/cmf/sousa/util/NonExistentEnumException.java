package mil.navy.nrl.cmf.sousa.util;

import java.io.InvalidObjectException;

/**
 * NonExistentEnumException
 */
public class NonExistentEnumException
   extends InvalidObjectException {

/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

/**
 * NonExistentEnumException(Class, Object)
 */
NonExistentEnumException(Class cls, Object id) {

      super("The enum for class " + cls.getName() + " and id " + id + 
			" no longer exists but was found during deserialization.");
   }
}
