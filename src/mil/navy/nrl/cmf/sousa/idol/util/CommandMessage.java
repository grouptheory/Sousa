package mil.navy.nrl.cmf.sousa.idol.util;

import java.io.Serializable;

/**
   CommandMessage
 */
public final class CommandMessage
implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	   _command
	 */
	/*@ non_null */ private final String _command;

// Constructors

/**
   CommandMessage(String)
   @methodtype ctor
   @param command .
 */
public
CommandMessage(/*@ non_null */ String command)
{
	this._command = command;
}

// sousa.idol.util.CommandMessage

/**
   getCommand()
   @methodtype get
   @return String
 */
public final String
getCommand()
{
	return _command;
}
}; // CommandMessage
