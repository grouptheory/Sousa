// File: Serializer.java

package mil.navy.nrl.cmf.sousa;

import java.io.IOException;

/**
 * Serializer is an interface which facilitates conversion of Objects
 * to and from byte[].
 * 
 * @version 	$Id: Serializer.java,v 1.1 2006/05/30 19:34:53 talmage Exp $
 * @author 	Bilal Khan
 * @author 	David Talmage
 */
public interface Serializer
{
    /** 
     * Convert an Object to a byte[].  This method is typically used
     * prior to serial transmission of the Object.  Precondition: ob
     * is Serializable.
     *
     * @param ob     the object to be serialized.
     * @return       the array of byte into which the object is now encoded.
     */
    public byte[] toByteArray(Object ob) 
		throws IOException;
    
    /** 
     * Convert a byte[] to an Object.  This method is typically used
     * after serial transmission of the Object.  <EM>There is no guarantee
     * that <CODE>data</CODE> has the same values on return.</EM>
     *
     * @param data     the array of byte into which the object is now encoded.
     * @return         the object reconstituted from the byte[].
     */
	public Object toObject(byte[] data) 
		throws IOException, ClassNotFoundException;
};

// File: Serializer.java

