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
}
