package mil.navy.nrl.cmf.sousa.spatiotemporal;

import java.io.Serializable;
import mil.navy.nrl.cmf.sousa.util.HashCodeUtil;
import mil.navy.nrl.cmf.stk.XYZd;

/**
   <CODE>Vector3d</CODE> is a partial reimplementation of
   <CODE>javax.vecmath.Vector3d</CODE>.  It exists to provide the
   funcionality if <CODE>javax.vecmath.Vector3d</CODE> without
   requiring Java3D.
 */
public class Vector3d 
    implements Serializable, XYZd
{
	private static final long serialVersionUID = 1L;
	public double x = 0.0;
    public double y = 0.0;
    public double z = 0.0;

	//// ctors

	/**
	   Constructs a <CODE>Vector3d</CODE> with the default value
	   &lt;0.0, 0.0, 0.0&gt;.
	 */
    public Vector3d() {
    }

	/**
	   Contructs a <CODE>Vector3d</CODE> from three doubles.
	 */
    public Vector3d(double x0, double y0, double z0) {
		x = x0;
		y = y0;
		z = z0;
    }

	/**
	   Contructs a <CODE>Vector3d</CODE> from a <CODE>Vector3d</CODE>.
	 */
    public Vector3d(Vector3d v) {
		x = v.x;
		y = v.y;
		z = v.z;
    }

	//// mil.navy.nrl.cmf.stk

	/**
	   Returns the X component of this <CODE>Vector3d</CODE>

	   @return the X value
	 */
	public double getX() {
		return x;
	}

	/**
	   Returns the Y component of this <CODE>Vector3d</CODE>

	   @return the Y value
	 */
	public double getY() {
		return y;
	}

	/**
	   Returns the Z component of this <CODE>Vector3d</CODE>

	   @return the Z value
	 */
	public double getZ() {
		return z;
	}

	/**
	   Sets the X component of this <CODE>Vector3d</CODE>

	   @param x1 the X value
	 */
	public void setX(double x1) {
		x = x1;
	}

	/**
	   Sets the Y component of this <CODE>Vector3d</CODE>

	   @param y1 the Y value
	 */
	public void setY(double y1) {
		y = y1;
	}

	/**
	   Sets the Z component of this <CODE>Vector3d</CODE>

	   @param z1 the Z value
	 */
	public void setZ(double z1) {
		z = z1;
	}

	/**
	   Sets the X, Y, and Z components from three doubles.

	   @param x1 the X value
	   @param y1 the Y value
	   @param z1 the Z value
	 */
	public void set(double x1, double y1, double z1) {
		x = x1;
		y = y1;
		z = z1;
	}

	// Vector3d

	/**
	   Sets the X, Y, and Z components from a <CODE>Vector3d</CODE>.

	   @param v the <CODE>Vector3d</CODE>
	 */
    public void set(Vector3d v) {
		x = v.x;
		y = v.y;
		z = v.z;
    }

	/**
	   Sets the value of this <CODE>Vector3d</CODE> to v1 - v2.
	   <P>

	   <A
	   HREF="http://en.wikipedia.org/wiki/Subtraction">Wikipedia</A>
	   offers an explanation of the terminology used in the
	   description of the arguments.

	   @param v1 the minuend
	   @param v2 the subtrahend
	 */
	public void sub(Vector3d v1, Vector3d v2) {
		x = v1.x - v2.x;
		y = v1.y - v2.y;
		z = v1.z - v2.z;
	}

	/**
	   Sets the value of this <CODE>Vector3d</CODE> to (s * v1) + v2.

	   @param s the scale
	   @param v1 the scaled <CODE>Vector3d</CODE>
	   @param v2 the offset <CODE>Vector3d</CODE>
	 */
	public void scaleAdd(double s, Vector3d v1, Vector3d v2) {
		x = s * v1.x + v2.x;
		y = s * v1.y + v2.y;
		z = s * v1.z + v2.z;
	}

	/**
	   Calculates the <A
	   HREF="http://www.mcasco.com/qa_ab3dv.html">angle</A> between
	   this <CODE>Vector3d</CODE> and <CODE>v</CODE>.

	   @param v the other <CODE>Vector3d</CODE>

	   @return the angle
	*/
	public double angle(Vector3d v) {
		return Math.acos((dot(v) / (length() * v.length())));
	}

	/**
	   Calculates the <A HREF="http://www.mcasco.com/qa_ab3dv.html">
	   length</A> of this <CODE>Vector3d</CODE>.

	   @return the length
	*/
	public double length() {
		return Math.sqrt(x * x + y * y  + z * z);
	}

	/**
	   Calculates the <A HREF="http://www.mcasco.com/qa_ab3dv.html">
	   dot product</A> of this <CODE>Vector3d</CODE> and <CODE>v</CODE>

	   @param v the other <CODE>Vector3d</CODE>

	   @return the dot product
	*/
	public double dot(Vector3d v) {
		return (x * v.x) + (y * v.y) + (z * v.z);
	}

	// java.lang.Object
	
	/**
	   Computes the hash code of this <CODE>Vector3d</CODE> using the
	   rules from Effective Java.

	   @return the hashcode
	   @see mil.navy.nrl.cmf.sousa.util.HashCodeUtil
	 */
	public int hashCode() {
		int answer = HashCodeUtil.SEED;
		answer = HashCodeUtil.hash(answer, x);
		answer = HashCodeUtil.hash(answer, y);
		answer = HashCodeUtil.hash(answer, z);

		return answer;
	}

	/**
	   Determines the equality of this <CODE>Vector3d</CODE> and
	   <CODE>obj</CODE>, which must also be a <CODE>Vector3d</CODE>.
	   Two <CODE>Vector3d</CODE>s are equal iff their corresponding
	   data members are equal.

	   @param obj the object to test for equality

	   @return <CODE>true</CODE> if this <CODE>Vector3d</CODE> equals
	   <CODE>obj</CODE>; <CODE>false</CODE> otherwise
	 */
	public boolean equals(Object obj) {
		return ((x == ((Vector3d)obj).x) && 
				(y == ((Vector3d)obj).y) && 
				(z == ((Vector3d)obj).z));
	}

	/**
	   Constructs a String representation of this <CODE>Vector3d</CODE>.
	   The string has the form &lt;x, y, z&gt;.

	   @return the String representation of this <CODE>Vector3d</CODE>
	 */
	public String toString() {
		return "<" + x + ", " + y + ", " + z + ">";
	}
}
