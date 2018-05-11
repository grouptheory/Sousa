// File: SignalType.java

package mil.navy.nrl.cmf.sousa;

import java.util.HashSet;
import java.util.Iterator;

/**
   SignalType -- a signal between Selectables and SelectableSets.
*/
public final class SignalType 
{
    public static final SignalType READ = new SignalType("SignalType.READ");
    public static final SignalType WRITE = new SignalType("SignalType.WRITE");
    public static final SignalType ERROR = new SignalType("SignalType.ERROR");

    private String _name = "";

    public SignalType(String name) {
		_name = name;
    }

    public String toString() {
		return _name;
    }

    private static final HashSet _signals = new HashSet();
    private static final boolean _initialized = false;

    public static Iterator iterator() 
    {
		if (!_initialized) {
			_signals.add(SignalType.READ);
			_signals.add(SignalType.WRITE);
			_signals.add(SignalType.ERROR);
		}
		return _signals.iterator();
    }
};
