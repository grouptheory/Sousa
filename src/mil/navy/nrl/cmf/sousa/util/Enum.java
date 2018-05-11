package mil.navy.nrl.cmf.sousa.util;

import java.io.Serializable;
import java.util.Hashtable;

/**
 * Enum
 */
public abstract class Enum
   implements Serializable {

   private final static Hashtable subclassInstanceDictionary__ = new Hashtable(5, 0.75f);
   private int id_;

/**
 * Enum(int)
 */
protected Enum(int id) throws 
   DuplicateEnumIdException {

      id_ = id;

      Hashtable instance_dictionary = (Hashtable)getInstanceDictionaryFor(getClass());
      synchronized (instance_dictionary) {
         Integer obj_id = new Integer(id);
         if (instance_dictionary.containsKey(obj_id))
            throw new DuplicateEnumIdException(getClass(), obj_id);

         instance_dictionary.put(obj_id, this);
      }               
   }

/**
 * getInstanceDictionaryFor(Class)
 */
private synchronized Hashtable getInstanceDictionaryFor(Class cls) {

      Hashtable ret = (Hashtable)subclassInstanceDictionary__.get(cls);
      if (ret == null) {
         ret = new Hashtable(5, 0.75f);
         subclassInstanceDictionary__.put(cls, ret);
      }
      return ret;
   }

/**
 * asInt()
 */
public final int asInt() {

      return id_;
   }

/**
 * readResolve()
 */
protected final Object readResolve() throws 
   NonExistentEnumException {

      Hashtable instance_dictionary = (Hashtable)getInstanceDictionaryFor(getClass());

      Enum ret = (Enum)instance_dictionary.get(new Integer(asInt()));

      if (ret == null)
         throw new NonExistentEnumException(getClass(), new Integer(asInt()));

      return ret;
   }

/**
 * hashCode()
 */
public final int hashCode() {

      String tmp = getClass().getName() + ":" + asInt();
      return tmp.hashCode();
   }

/**
 * equals(Object)
 */
public final boolean equals(Object obj) {

      return (obj == this);
   }
}
