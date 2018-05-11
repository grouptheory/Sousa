// File: StackTrace.java

package mil.navy.nrl.cmf.sousa.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public final class StackTrace
{

/**
   formatStackTrace(Throwable)

   Formats a Throwable's stack trace for printing

   @param ex an Throwable
   @return a ByteArrayOutputStream that contains the stack trace of
   the exception formatted for printing
   @methodtype convenience
 */
public static ByteArrayOutputStream formatStackTrace(Throwable ex) 
{
	ByteArrayOutputStream bs = new ByteArrayOutputStream();
	if (null != ex) {
		PrintStream ps = new PrintStream(bs);
		ex.printStackTrace(ps);
	}

	return bs;
}

public static ByteArrayOutputStream formatStackTrace() 
{
	Exception ex = new Exception("Stack Trace");
	ex.fillInStackTrace();

	return StackTrace.formatStackTrace(ex);
}

}
