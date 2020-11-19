package net.enilink.komma.core;

import java.util.Collection;
import java.util.Iterator;

/**
 * An iterable set of key => value pairs.
 */
public interface IBindings<T> extends Iterable<T> {
	/**
	 * Returns the value that is associated with the given <code>key</code>.
	 * 
	 * @param key A key (usually a variable name within a SPARQL query).
	 * @return The value that is associated with the key or <code>null</code>.
	 */
	T get(String key);

	/**
	 * Returns all keys that have associated values (including <code>null</code>)
	 * within this bindings set.
	 * 
	 * @return The keys of this bindings set
	 */
	Collection<String> getKeys();
	
	/**
	 * Returns an iterator for all values within this bindings set.
	 * 
	 * @return An iterator for all values
	 */
	Iterator<T> iterator();
}
