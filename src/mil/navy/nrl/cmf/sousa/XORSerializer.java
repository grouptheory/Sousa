// File: XORSerializer.java

package mil.navy.nrl.cmf.sousa;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * XORSerializer is a class which facilitates conversion of Objects to
 * and from byte[], encoding and decoding with a symmetric key.
 * 
 * @version 	$Id: XORSerializer.java,v 1.2 2006/07/20 15:19:09 talmage Exp $
 * @author 	Bilal Khan
 * @author 	David Talmage
 */
public class XORSerializer implements Serializer
{
	private final int _key;
	private final byte key0;
	private final byte key1;
	private final byte key2;
	private final byte key3;

	/**
	 * Class constructor that uses an integer symmetric key.
	 *
	 * @param key the symmetric key for encoding and decoding
	 * serialized Objects.
	 */
	public XORSerializer(int key)
	{
		_key = key;
		key0 = (byte)((_key & 0xFF000000) >> 24);
		key1 = (byte)((_key & 0x00FF0000) >> 16);
		key2 = (byte)((_key & 0x0000FF00) >> 8);
		key3 = (byte)(_key & 0x000000FF);
	}

	/**
	 * Serializes <CODE>ob</CODE> to a byte array then XORs every four
	 * consecutive bytes with the symmetric key.
	 */
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
	
		// Encode
		applyKey(answer);

		return answer;
    }
    
	/**
	 * XORs every four consecutive bytes in <CODE>data</CODE> with the
	 * symmetric key then deserializes the result.  <EM>WARNING!
	 * toObject changes the values in <CODE>data</CODE>.</EM>
	 */
	public Object toObject(byte[] data)
		throws IOException, ClassNotFoundException
    {
		Object answer = null;

		// Decode
		applyKey(data);

		// make a Stream
		ByteArrayInputStream byteInputStream = new ByteArrayInputStream(data);
		ObjectInputStream objectStream =
			new ObjectInputStream(byteInputStream);
	
		// read the Object
		answer = objectStream.readObject();
	
		return answer;

	};

	// Apply the symmetric key to data.
	//
	// WARNING: applyKey changes the values in data.
	//
	private void applyKey(byte[] data) {
		for (int i=0; i < data.length; i+=4) {

			data[i] = (byte)(data[i] ^ key0);

			if (data.length > i+1) {

				data[i+1] = (byte)(data[i+1] ^ key1);

				if (data.length > i+2) {

					data[i+2] = (byte)(data[i+2] ^ key2);

					if (data.length > i+3) {

						data[i+3] = (byte)(data[i+3] ^ key3);
					}
				}
			}
		}
	}

	/*
	 * Usage: XORSerializer key
	 *
	 * key is a 32-bit integer.
	 */
	public static void main(String[] args) {
		Serializer s;
		Integer i = new Integer(32767);
		Integer result;
		byte[] bytes;
		Serializer b = new ByteArray();

		try {
			s = new XORSerializer(Integer.parseInt(args[0]));

			try {
				bytes = s.toByteArray(i);

				try {
					Object notAnObject = b.toObject(bytes);
					System.out.println("Deserialized the XORed object! " +
									   notAnObject);
				} catch (ClassNotFoundException ex) {
					System.err.println("Expected & caught " + ex);
				} catch (IOException ex) {
					System.err.println("Expected & caught " + ex);
				}

				try {
					result = (Integer)s.toObject(bytes);
					System.out.println("Input:  " + i);
					System.out.println("Output: " + result);
					System.out.println("Identical? " + i.equals(result));
				} catch (ClassNotFoundException ex) {
					System.err.println("Error during deserialization: " + ex);
				} catch (IOException ex) {
					System.err.println("Error during deserialization: " + ex);
				}
			} catch (IOException ex) {
				System.err.println("Error during serialization: " + ex);
			}
		} catch (NumberFormatException ex) {
			System.err.println("Usage: XORSerializer key");
			System.err.println("       key must be an integer");
		} catch (ArrayIndexOutOfBoundsException ex) {
			System.err.println("Usage: XORSerializer key");
			System.err.println("       key must be an integer");
		}
	}
}
// File: XORSerializer.java

