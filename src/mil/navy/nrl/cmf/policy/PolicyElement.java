package mil.navy.nrl.cmf.policy;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class PolicyElement implements Serializable {
	private List _identities; // List of Identity
	private List _results;    // List of Object

	// Required by the Skaringa XML serialization framework.
	private PolicyElement() {
		_identities = new LinkedList();
		_identities = new LinkedList();
	}

	public PolicyElement(List identities, List results) {
		_identities = identities;
		_results = results;
	}

	public List identities() {
		return Collections.unmodifiableList(_identities);
	}

	public List results() {
		return Collections.unmodifiableList(_results);
	}
}