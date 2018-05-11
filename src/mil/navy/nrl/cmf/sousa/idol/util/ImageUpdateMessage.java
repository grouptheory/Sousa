package mil.navy.nrl.cmf.sousa.idol.util;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
   ImageUpdateMessage
 */
public final class ImageUpdateMessage
implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	   _adds
	 */
	/*@ non_null */ private final Set _adds = new HashSet();

	/**
	   _removes
	 */
	/*@ non_null */ private final Set _removes = new HashSet();

// Constructors

/**
   ImageUpdateMessage(Set, Set)
   @methodtype ctor
   @param adds .
   @param removes .
 */
public
ImageUpdateMessage(Set adds, Set removes)
{
	if (null != adds) {
		_adds.addAll(adds);
	}
	if (null != removes) {
		_removes.addAll(removes);
	}
}

// sousa.idol.util.ImageUpdateMessage

/**
   getAdds()
   @methodtype get
   @return Set
 */
public final Set
getAdds()
{
	return _adds;
}

/**
   getRemoves()
   @methodtype get
   @return Set
 */
public final Set
getRemoves()
{
	return _removes;
}

public final void
add(String add)
{
	_adds.add(add);
}

public final void
remove(String remove)
{
	_removes.add(remove);
}
}; // ImageUpdateMessage
