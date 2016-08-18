/*******************************************************************************
 * Copyright (c) 2009, 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.em.internal.behaviours;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import net.enilink.commons.iterator.ConvertingIterator;
import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.composition.properties.traits.Mergeable;
import net.enilink.composition.properties.traits.Refreshable;
import net.enilink.composition.traits.Behaviour;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IEntityManager;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.IValue;
import net.enilink.komma.core.Statement;
import net.enilink.komma.core.URI;
import net.enilink.vocab.komma.KOMMA;

public abstract class AbstractRDFMap extends
		java.util.AbstractMap<Object, Object> implements
		java.util.Map<Object, Object>, Refreshable, Mergeable, IEntity,
		Behaviour<IEntity> {
	private class Entry implements Map.Entry<Object, Object> {
		private Object key;
		private Object value;

		public Entry(Object key, Object value) {
			this.key = key;
			this.value = value;
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof Map.Entry<?, ?>))
				return false;
			Map.Entry<?, ?> candidate = (Map.Entry<?, ?>) obj;
			if ((this.key.equals(candidate.getKey()))
					&& (this.value.equals(candidate.getValue()))) {
				return true;
			} else {
				return false;
			}
		}

		@Override
		public Object getKey() {
			return key;
		}

		@Override
		public Object getValue() {
			return value;
		}

		@Override
		public Object setValue(Object value) {
			throw new UnsupportedOperationException();
		}
	}

	private final class EntrySet extends AbstractSet<Map.Entry<Object, Object>> {

		public void clear() {
			throw new UnsupportedOperationException();
		}

		public boolean contains(Object o) {
			if (!(o instanceof Map.Entry<?, ?>))
				return false;
			Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
			Map.Entry<?, ?> candidate;
			try {
				candidate = getEntryAsRDF4JMapEntry(e.getKey());
			} catch (Exception e1) {
				return false;
			}
			return candidate != null && candidate.equals(e);
		}

		public Iterator<Map.Entry<Object, Object>> iterator() {
			return new SimpleRDF4JMapIterator();// newEntryIterator();
		}

		public boolean remove(Object o) {
			throw new UnsupportedOperationException();
		}

		public int size() {
			throw new UnsupportedOperationException();
		}
	}

	private class SimpleRDF4JMapIterator implements
			Iterator<java.util.Map.Entry<Object, Object>> {

		private Stack<Entry> elements;

		public SimpleRDF4JMapIterator() {
			elements = new Stack<Entry>();

			IExtendedIterator<IValue> values = match(getBehaviourDelegate(),
					KOMMA.PROPERTY_ENTRY, null);
			while (values.hasNext()) {
				elements.push(createMapEntry(values.next()));
			}
		}

		@Override
		public boolean hasNext() {
			if (elements.empty())
				return false;
			else
				return true;
		}

		@Override
		public Entry next() {
			return elements.pop();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

	}

	private int _size = 0;

	private transient Set<Map.Entry<Object, Object>> entrySet;

	IEntityManager manager;

	transient volatile int modCount;

	final String SELECT_ENTRY_BY_KEY = "PREFIX komma:<" + KOMMA.NAMESPACE
			+ "> " + "SELECT DISTINCT ?entry where {?resource komma:"
			+ KOMMA.PROPERTY_ENTRY.localPart() + " ?entry "
			+ ". ?entry komma:" + getUri4Key().localPart() + " ?key}";

	void addStatement(IReference subj, URI pred, Object obj) {
		if (obj == null) {
			return;
		}

		getEntityManager().add(new Statement(subj, pred, obj));
	}

	@Override
	public void clear() {
		IExtendedIterator<IValue> values = match(getBehaviourDelegate(),
				KOMMA.PROPERTY_ENTRY, null);
		while (values.hasNext()) {
			removeEntry((IReference) values.next());
		}
	}

	@Override
	public boolean containsKey(Object key) {
		try {
			if (getEntry(key) == null) {
				return false;
			} else {
				return true;
			}
		} catch (Exception e) {
			return false;
		}
	}

	private Entry createMapEntry(IValue v) {
		IExtendedIterator<IValue> it = null;
		IValue key, value;
		try {
			it = match((IReference) v, getUri4Value(), null);
			value = it.next();
		} finally {
			it.close();
		}

		it = match((IReference) v, getUri4Key(), null);
		key = it.next();

		Entry entry = (Entry) new AbstractRDFMap.Entry(getEntityManager()
				.toInstance(key), getEntityManager().toInstance(value));
		return entry;
	}

	@Override
	public Set<java.util.Map.Entry<Object, Object>> entrySet() {
		Set<Map.Entry<Object, Object>> es = entrySet;
		return es != null ? es : (entrySet = new EntrySet());
	}

	@Override
	public Object get(Object key) {
		IReference entry = getEntry(key);
		if (entry == null) {
			return null;
		}

		IExtendedIterator<IValue> values = match(entry, getUri4Value(), null);
		try {
			if (values.hasNext()) {
				return getEntityManager().toInstance(values.next());
			}
		} finally {
			values.close();
		}
		return null;
	}

	private IReference getEntry(Object key) {
		IExtendedIterator<IReference> result = null;
		try {
			result = getEntityManager().createQuery(SELECT_ENTRY_BY_KEY)
					.setParameter("resource", getBehaviourDelegate())
					.setParameter("key", key)
					.evaluateRestricted(IReference.class);
			if (result.hasNext()) {
				return result.next();
			}
		} finally {
			if (result != null) {
				result.close();
			}
		}

		return null;
	}

	private AbstractRDFMap.Entry getEntryAsRDF4JMapEntry(Object key)
			throws Exception {
		return createMapEntry(getEntry(key));
	}

	protected abstract URI getUri4Key();

	protected abstract URI getUri4Value();

	@Override
	public Set<Object> keySet() {
		return super.keySet();
	}

	private IExtendedIterator<IValue> match(IReference subj, URI pred,
			IValue obj) {
		return new ConvertingIterator<IStatement, IValue>(getEntityManager()
				.match(subj, pred, obj)) {
			protected IValue convert(IStatement stmt) {
				return (IValue) stmt.getObject();
			}
		};
	}

	public void merge(Object source) {
		if (source instanceof java.util.Map<?, ?>) {
			clear();
			putAll((java.util.Map<?, ?>) source);
		}
	}

	@Override
	public Object put(Object key, Object value) {
		Object result = null;

		if (containsKey(key)) {
			result = get(key);
			remove(key);
		}

		// anonym. Entry anlegen
		IEntity mapEntry = manager.create(manager.find(KOMMA.TYPE_MAPENTRY));

		// Entry zu Map zuordnen
		addStatement(this, KOMMA.PROPERTY_ENTRY, mapEntry);

		// Key
		addStatement(mapEntry, getUri4Key(), key);

		// addProperty(property, obj)

		// Value
		addStatement(getBehaviourDelegate(), getUri4Value(), value);
		_size++;
		modCount++;
		return result;
	}

	public void refresh() {
		_size = 0;
	}

	@Override
	public Object remove(Object key) {
		Object result = get(key);
		if (result != null) {
			IReference entry = getEntry(key);

			removeEntry(entry);
		}

		return result;
	}

	private void removeEntry(IReference entry) {
		removeStatements(getBehaviourDelegate(), KOMMA.PROPERTY_ENTRY, entry);
		removeStatements(entry, getUri4Key(), null);
		removeStatements(entry, getUri4Value(), null);
		_size--;
	}

	void removeStatements(IReference subj, URI pred, IValue obj) {
		getEntityManager().remove(new Statement(subj, pred, obj));
	}

	@Override
	public int size() {
		return _size;
	}

	@Override
	public String toString() {
		return super.toString();
	}
}
