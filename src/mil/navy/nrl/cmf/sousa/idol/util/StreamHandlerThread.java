package mil.navy.nrl.cmf.sousa.idol.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
   StreamHandlerThread
 */
public final class StreamHandlerThread
extends Thread
{
	/**
	   Default process input stream handler
	 */
	public static final StreamHandler INPUTHANDLER = new StreamHandler()
	{
		// StreamHandlerThread$StreamHandler

		/**
		   @see StreamHandlerThread$StreamHandler#handleInput(String)
		 */
		public final void
		handleInput(String line)
		{
			System.out.println(line);
			System.out.flush();
		}

		/**
		   @see StreamHandlerThread$StreamHandler#handleException()
		 */
		public final void
		handleException(IOException exception)
		{
			exception.printStackTrace(System.err);
		}

		/**
		   @see StreamHandlerThread$StreamHandler#handleEOF()
		 */
		public final void
		handleEOF()
		{
		}
	};

	/**
	   Default process error stream handler
	 */
	public static final StreamHandler ERRORHANDLER = new StreamHandler()
	{
		// StreamHandlerThread$StreamHandler

		/**
		   @see StreamHandlerThread$StreamHandler#handleInput(String)
		 */
		public final void
		handleInput(String line)
		{
			System.err.println(line);
			System.err.flush();
		}

		/**
		   @see StreamHandlerThread$StreamHandler#handleException()
		 */
		public final void
		handleException(IOException exception)
		{
			exception.printStackTrace(System.err);
		}

		/**
		   @see StreamHandlerThread$StreamHandler#handleEOF()
		 */
		public final void
		handleEOF()
		{
		}
	};

	/**
	   _stream
	 */
	/*@ non_null */ private final InputStream _stream;

	/**
	   _handler
	 */
	/*@ non_null */ private final StreamHandler _handler;

// Constructors

/**
   StreamHandlerThread(String, InputStream, StreamHandler)
   @methodtype ctor
   @param name .
   @param stream .
   @param handler .
 */
public
StreamHandlerThread(/*@ non_null */ String name, /*@ non_null */ InputStream stream, /*@ non_null */ StreamHandler handler)
{
	super(name);

	this._stream = stream;
	this._handler = handler;
}

// StreamHandlerThread

/**
   priority(int)
   @methodtype set
   @param pri .
   @return StreamHandlerThread
 */
public final StreamHandlerThread
priority(int pri)
{
	setPriority(pri);
	return this;
}

// java.lang.Thread

/**
   @see java.lang.Thread#run()
 */
public synchronized final void
run()
{
	try {
		BufferedReader reader = new BufferedReader(new InputStreamReader(_stream));
		String line;
		while (null != (line = reader.readLine())) {
			_handler.handleInput(line);
		}

		_handler.handleEOF();
	} catch (IOException e) {
		_handler.handleException(e);
	}
}

// ****************************************************************

/**
   StreamHandler
 */
public interface StreamHandler
{
// StreamHandlerThread$StreamHandler

/**
   handleInput(String)
   @methodtype handler
   @param line .
 */
public void
handleInput(/*@ non_null */ String line);

/**
   handleException(IOException)
   @methodtype handler
   @param exception
 */
public void
handleException(/*@ non_null */ IOException exception);

/**
   handleEOF()
   @methodtype handler
 */
public void
handleEOF();
}; // StreamHandler

// ****************************************************************

}; // StreamHandlerThread
