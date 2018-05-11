package mil.navy.nrl.cmf.sousa.idol.util;

import java.io.Serializable;
import java.rmi.server.UID;

import javax.crypto.SecretKey;

/**
   KeyUpdateMessage
 */
public final class KeyUpdateMessage
implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	   _uid
	 */
	/*@ non_null */ private final UID _uid;

	/**
	   _keySeq
	 */
	private final long _keySeq;

	/**
	   _secretKey
	 */
	/*@ non_null */ private final SecretKey _secretKey;

// Constructors

/**
   KeyUpdateMessage(UID, long, SecretKey)
   @methodtype ctor
   @param uid .
   @param keySeq .
   @param secretKey .
 */
public
KeyUpdateMessage(/*@ non_null */ UID uid, long keySeq, /*@ non_null */ SecretKey secretKey)
{
	this._uid = uid;
	this._keySeq = keySeq;
	this._secretKey = secretKey;
}

// sousa.idol.util.KeyUpdateMessage

/**
   getUID()
   @methodtype get
   @return UID
 */
public final UID
getUID()
{
	return _uid;
}

/**
   getKeySeq()
   @methodtype get
   @return long
 */
public final long
getKeySeq()
{
	return _keySeq;
}

/**
   getSecretKey()
   @methodtype get
   @return SecretKey
 */
public final SecretKey
getSecretKey()
{
	return _secretKey;
}
}; // KeyUpdateMessage
