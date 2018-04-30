package net.enilink.komma.core;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

/**
 * Represents a fragment of a query and related binding values.
 */
public class QueryFragment {
	static final IBindings<Object> EMPTY_BINDINGS = new IBindings<Object>() {
		@Override
		public Iterator<Object> iterator() {
			return Collections.emptyList().iterator();
		}

		@Override
		public Collection<String> getKeys() {
			return Collections.emptyList();
		}

		@Override
		public Object get(String key) {
			return null;
		}
	};

	public final String text;
	public final IBindings<Object> bindings;

	public QueryFragment(String text) {
		this(text, EMPTY_BINDINGS);
	}

	public QueryFragment(String text, IBindings<Object> bindings) {
		this.text = text;
		this.bindings = bindings;
	}

	public void addParameters(IQuery<?> query) {
		for (String key : bindings.getKeys()) {
			query.setParameter(key, bindings.get(key));
		}
	}

	@Override
	public String toString() {
		return text;
	}
}