package net.enilink.komma.core;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A simple {@link LinkedHashMap} based implementation of the {@link IBindings}
 * interface.
 */
public class LinkedHashBindings<T> implements IBindings<T> {
	private Map<String, T> values = new LinkedHashMap<String, T>();

	public LinkedHashBindings() {
		this(10);
	}

	public LinkedHashBindings(int size) {
		values = new LinkedHashMap<String, T>(size);
	}

	public void put(String key, T value) {
		values.put(key, value);
	}

	@Override
	public T get(String key) {
		return values.get(key);
	}

	@Override
	public Collection<String> getKeys() {
		return values.keySet();
	}

	@Override
	public Iterator<T> iterator() {
		return values.values().iterator();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((values == null) ? 0 : values.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LinkedHashBindings<?> other = (LinkedHashBindings<?>) obj;
		if (values == null) {
			if (other.values != null)
				return false;
		} else if (!values.equals(other.values))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + ": " + values.toString();
	}
}
