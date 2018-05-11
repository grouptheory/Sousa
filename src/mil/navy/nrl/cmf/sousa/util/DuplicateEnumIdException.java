package mil.navy.nrl.cmf.sousa.util;

/**
 * DuplicateEnumIdException
 */
public class DuplicateEnumIdException
   extends RuntimeException {

/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

/**
 * DuplicateEnumIdException(Class, Object)
 */
DuplicateEnumIdException(Class cls, Object id) {

      super("An enum for class " + cls.getName() + 
			" is already registered for id " + id + ".");
   }
}
