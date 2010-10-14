/*
 * Copyright (c) 2007, 2010, James Leigh All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution. 
 * - Neither the name of the openrdf.org nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package net.enilink.komma.internal.sesame.behaviours;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import net.enilink.composition.properties.traits.Mergeable;
import net.enilink.composition.properties.traits.Refreshable;
import net.enilink.composition.traits.Behaviour;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.contextaware.ContextAwareConnection;
import org.openrdf.result.Result;
import org.openrdf.result.TupleResult;
import org.openrdf.store.StoreException;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.komma.common.util.URIUtil;
import net.enilink.komma.concepts.CONCEPTS;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.sesame.ISesameEntity;
import net.enilink.komma.sesame.ISesameManager;
import net.enilink.komma.sesame.ISesameResourceAware;
import net.enilink.komma.sesame.iterators.SesameIterator;

/**
 * 
 */
public abstract class AbstractRDFMap extends java.util.AbstractMap<Object, Object>
		implements java.util.Map<Object, Object>, Refreshable, Mergeable,
		Behaviour<ISesameEntity> {

	private transient Set<Map.Entry<Object, Object>> entrySet = null;
	transient volatile int modCount;

	final String SELECT_ENTRY_BY_KEY = "PREFIX abc:<" + CONCEPTS.NAMESPACE
			+ "> " + "SELECT DISTINCT ?entry where {?resource abc:"
			+ CONCEPTS.PROPERTY_ENTRY.localPart() + " ?entry "
			+ ". ?entry abc:" + getUri4Key().getLocalName() + " ?key}";

	private class Entry implements Map.Entry<Object, Object> {
		private Object key;
		private Object value;

		public Entry(Object key, Object value) {
			this.key = key;
			this.value = value;
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
	}

	private final class EntrySet extends AbstractSet<Map.Entry<Object, Object>> {

		public Iterator<Map.Entry<Object, Object>> iterator() {
			return new SimpleSesameMapIterator();// newEntryIterator();
		}

		public boolean contains(Object o) {
			if (!(o instanceof Map.Entry<?, ?>))
				return false;
			Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
			Map.Entry<?, ?> candidate;
			try {
				candidate = getEntryAsSesameMapEntry(e.getKey());
			} catch (Exception e1) {
				return false;
			}
			return candidate != null && candidate.equals(e);
		}

		public boolean remove(Object o) {
			throw new UnsupportedOperationException();
		}

		public int size() {
			throw new UnsupportedOperationException();
		}

		public void clear() {
			throw new UnsupportedOperationException();
		}
	}

	private class SimpleSesameMapIterator implements
			Iterator<java.util.Map.Entry<Object, Object>> {

		private Stack<Entry> elements;

		public SimpleSesameMapIterator() {
			elements = new Stack<Entry>();

			IExtendedIterator<Value> values = getStatements(
					getBehaviourDelegate().getSesameResource(),
					URIUtil.toSesameUri(CONCEPTS.PROPERTY_ENTRY), null);
			while (values.hasNext()) {
				Value v = values.next();

				Entry entry = (Entry) createMapEntry(v);

				elements.push(entry);
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

	ISesameManager manager;

	private int _size = 0;

	public void refresh() {
		_size = 0;
	}

	public void merge(Object source) {
		if (source instanceof java.util.Map<?, ?>) {
			clear();
			putAll((java.util.Map<?, ?>) source);
		}
	}

	ValueFactory getValueFactory() {
		RepositoryConnection conn = manager.getConnection();
		return conn.getValueFactory();
	}

	private IExtendedIterator<Value> getStatements(Resource subj, URI pred,
			Value obj) {
		try {
			ContextAwareConnection conn = manager.getConnection();
			Result<Statement> stmts = conn.match(subj, pred, obj);
			// stmts.enableDuplicateFilter();
			return new SesameIterator<Statement, Value>(stmts) {
				@Override
				protected Value convert(Statement stmt) throws Exception {
					return stmt.getObject();
				}
			};
		} catch (StoreException e) {
			throw new KommaException(e);
		}
	}

	void addStatement(Resource subj, URI pred, Value obj) {
		if (obj == null)
			return;
		try {
			ContextAwareConnection conn = manager.getConnection();
			conn.add(subj, pred, obj);
		} catch (StoreException e) {
			throw new KommaException(e);
		}
	}

	void removeStatements(Resource subj, URI pred, Value obj) {
		try {
			ContextAwareConnection conn = manager.getConnection();
			conn.removeMatch(subj, pred, obj);
		} catch (StoreException e) {
			throw new KommaException(e);
		}
	}

	@Override
	public int size() {
		return _size;
	}

	@Override
	public String toString() {
		return super.toString();
	}

	@Override
	public Set<java.util.Map.Entry<Object, Object>> entrySet() {
		Set<Map.Entry<Object, Object>> es = entrySet;
		return es != null ? es : (entrySet = new EntrySet());
	}

	@Override
	public Set<Object> keySet() {
		return super.keySet();
	}

	protected abstract URI getUri4Key();

	protected abstract URI getUri4Value();

	@Override
	public Object put(Object key, Object value) {

		Object result = null;

		if (containsKey(key)) {
			result = get(key);
			remove(key);
		}

		// anonym. Entry anlegen
		IEntity mapEntry = manager.create(manager.find(CONCEPTS.TYPE_MAPENTRY));

		// Entry zu Map zuordnen
		addStatement(getBehaviourDelegate().getSesameResource(),
				URIUtil.toSesameUri(CONCEPTS.PROPERTY_ENTRY),
				((ISesameResourceAware) mapEntry).getSesameResource());

		// Key
		Value mapKey = ((ISesameManager) manager).getValue(key);

		addStatement(((ISesameResourceAware) mapEntry).getSesameResource(),
				getUri4Key(), mapKey);

		// addProperty(property, obj)

		// Value
		Value mapValue = ((ISesameManager) manager).getValue(value);
		addStatement(((ISesameResourceAware) mapEntry).getSesameResource(),
				getUri4Value(), mapValue);
		_size++;
		modCount++;
		return result;
	}

	private Value getEntry(Object key) throws MalformedQueryException,
			StoreException {
		Value result = null;
		TupleResult queryResult = null;
		TupleQuery tupleQuery = null;
		try {
			tupleQuery = manager.getConnection().prepareTupleQuery(
					QueryLanguage.SPARQL, SELECT_ENTRY_BY_KEY);
			tupleQuery.setBinding("resource", getBehaviourDelegate()
					.getSesameResource());
			tupleQuery.setBinding("key",
					((ISesameManager) manager).getValue(key));

			queryResult = tupleQuery.evaluate();

			if (queryResult.hasNext()) {
				BindingSet set = queryResult.next();
				Binding x = set.getBinding("entry");
				result = x.getValue();

			}

		} finally {
			queryResult.close();
		}

		return result;
	}

	private AbstractRDFMap.Entry getEntryAsSesameMapEntry(Object key)
			throws Exception {
		Value v = getEntry(key);
		return createMapEntry(v);
	}

	@Override
	public Object get(Object key) {
		Value entry = null;
		try {
			entry = getEntry(key);
			if (entry == null) {
				return null;
			}

			IExtendedIterator<Value> values = getStatements((Resource) entry,
					getUri4Value(), null);
			entry = values.next();

		} catch (MalformedQueryException e) {
			throw new RuntimeException(e);
		} catch (StoreException e) {
			throw new RuntimeException(e);
		}

		if (entry != null) {
			return ((ISesameManager) manager).getInstance(entry, null);
		} else {
			return null;
		}
	}

	@Override
	public Object remove(Object key) {
		Value entry = null;

		Object result = get(key);

		if (result != null) {
			try {
				entry = getEntry(key);

				removeEntry(entry);
			} catch (MalformedQueryException e) {
				throw new RuntimeException(e);
			} catch (StoreException e) {
				throw new RuntimeException(e);
			}
		}

		return result;
	}

	private void removeEntry(Value entry) {
		removeStatements(getBehaviourDelegate().getSesameResource(),
				URIUtil.toSesameUri(CONCEPTS.PROPERTY_ENTRY), (Resource) entry);
		removeStatements((Resource) entry, getUri4Key(), null);
		removeStatements((Resource) entry, getUri4Value(), null);
		_size--;
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

	@Override
	public void clear() {
		Value entry = null;

		IExtendedIterator<Value> values = getStatements(getBehaviourDelegate()
				.getSesameResource(),
				URIUtil.toSesameUri(CONCEPTS.PROPERTY_ENTRY), null);
		while (values.hasNext()) {
			entry = values.next();
			removeEntry(entry);
		}
	}

	private Entry createMapEntry(Value v) {
		IExtendedIterator<Value> valueIter = getStatements((Resource) v,
				getUri4Value(), null);
		Value value = valueIter.next();

		IExtendedIterator<Value> keyIter = getStatements((Resource) v,
				getUri4Key(), null);
		Value key = keyIter.next();

		Object keyObj = ((ISesameManager) manager).getInstance(key, null);
		Object valueObj = ((ISesameManager) manager).getInstance(value, null);

		Entry entry = (Entry) new AbstractRDFMap.Entry(keyObj, valueObj);
		return entry;
	}
}
