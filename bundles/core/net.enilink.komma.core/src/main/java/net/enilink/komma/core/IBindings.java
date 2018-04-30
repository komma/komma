package net.enilink.komma.core;

import java.util.Collection;
import java.util.Iterator;

/**
 * An iterable set of key => value pairs.
 */
public interface IBindings<T> extends Iterable<T> {
	T get(String key);

	Collection<String> getKeys();

	Iterator<T> iterator();
}
