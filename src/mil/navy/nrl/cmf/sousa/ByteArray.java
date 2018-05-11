// File: ByteArray.java

package mil.navy.nrl.cmf.sousa;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * ByteArray is a class which facilitates conversion of Objects to and
 * from byte[].
 * 
 * @version 	$Id: ByteArray.java,v 1.3 2006/05/30 19:34:52 talmage Exp $
 * @author 	Bilal Khan
 * @author 	David Talmage
 */
public class ByteArray implements Serializer
{
	public ByteArray() {}

    public byte[] toByteArray(Object ob)
		throws IOException
    {
		byte[] answer = null;
	
		// make a Stream
		ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
	
		ObjectOutputStream objectStream = 
			new ObjectOutputStream(byteOutputStream);
	
		// write the Object
		objectStream.writeObject(ob);
	
		// get the byte[]
		answer = byteOutputStream.toByteArray();
	
		return answer;
    }
    
	public Object toObject(byte[] data)
		throws IOException, ClassNotFoundException
    {
		Object answer = null;
	
		// make a Stream
		ByteArrayInputStream byteInputStream = new ByteArrayInputStream(data);
		ObjectInputStream objectStream =
			new ObjectInputStream(byteInputStream);
	
		// read the Object
		answer = objectStream.readObject();
	
		return answer;
    }
};

// File: ByteArray.java

