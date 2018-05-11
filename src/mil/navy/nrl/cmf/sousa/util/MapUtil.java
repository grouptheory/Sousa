package mil.navy.nrl.cmf.sousa.util;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Utilities for working with Maps that permit duplicate keys.
 */
public final class MapUtil
{
	private MapUtil() {}

	/**
	 * addToMapList(Map, Object, Object)
	 *
	 * Add an Object into a Map that permits duplicate keys.  The
	 * values that have the same key are stored in a List.  You've
	 * seen this a million times before.
	 *
	 * @param m the Map
	 * @param key the key to m
	 * @param value a value to associate with key in m
	 *
	 * @methodtype set
	 */
	public static void addToMapList(Map m, Object key, Object value) {
		List items = (List)m.get(key);

		if (null == items) {
			items = new LinkedList();
			m.put(key, items);
		}

		items.add(value);
	}

	/**
	 * removeFromMapList(Map, Object, Object)
	 *
	 * Remove an Object from a Map that permits duplicate keys.
	 * The values that have the same key are stored in a List.
	 * You've seen this a million times before.
	 *
	 * @param m the Map
	 * @param key the key to m
	 * @param value a value associated with key in m
	 *
	 * @methodtype set
	 */
	public static void removeFromMapList(Map m, Object key, Object value) {
		List items = (List)m.get(key);

		if (null != items) {
			items.remove(value);

			if (items.size() == 0)
				m.remove(key);
		}
	}


	/**
	 * addToMapSet(Map, Object, Object)
	 *
	 * Add an Object into a Map that permits duplicate keys.  The
	 * values that have the same key are stored in a Set.  You've
	 * seen this a million times before.
	 *
	 * @param m the Map
	 * @param key the key to m
	 * @param value a value to associate with key in m
	 *
	 * @methodtype set
	 */
	public static void addToMapSet(Map m, Object key, Object value) {
		Set items = (Set)m.get(key);

		if (null == items) {
			items = new HashSet();
			m.put(key, items);
		}

		items.add(value);
	}

	/**
	 * removeFromMapSet(Map, Object, Object)
	 *
	 * Remove an Object from a Map that permits duplicate keys.
	 * The values that have the same key are stored in a Set.
	 * You've seen this a million times before.
	 *
	 * @param m the Map
	 * @param key the key to m
	 * @param value a value associated with key in m
	 *
	 * @methodtype set
	 */
	public static void removeFromMapSet(Map m, Object key, Object value) {
		Set items = (Set)m.get(key);

		if (null != items) {
			items.remove(value);

			if (items.size() == 0)
				m.remove(key);
		}
	}
}
