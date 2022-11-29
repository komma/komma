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
package net.enilink.composition.properties.komma;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import net.enilink.commons.iterator.ConvertingIterator;
import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.commons.iterator.NiceIterator;
import net.enilink.commons.iterator.WrappedIterator;
import net.enilink.composition.properties.PropertySetFactory;
import net.enilink.composition.properties.traits.Filterable;
import net.enilink.composition.properties.PropertySet;
import net.enilink.composition.properties.exceptions.PropertyException;
import net.enilink.composition.properties.traits.Mergeable;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.ILiteral;
import net.enilink.komma.core.IQuery;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IReferenceable;
import net.enilink.komma.core.ITransaction;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.core.Statement;
import net.enilink.komma.core.URI;

import javax.inject.Inject;

/**
 * A set for a given subject and predicate.
 *
 * @param <E>
 */
public class KommaPropertySet<E> implements PropertySet<E>, Set<E>, Filterable<E> {
	protected static final String QUERY = "SELECT DISTINCT ?o WHERE { ?s ?p ?o }";
	private static final int CACHE_LIMIT = 10;
	protected final List<WeakReference<Object>> ownerBeans = new ArrayList<>(1);

	protected KommaPropertySetFactory factory;
	protected final IReference subject;
	protected final IReference property;
	protected Class<E> valueType;
	protected URI rdfValueType;
	protected List<ITransaction> activeTxns = null;
	private volatile List<E> cache;

	public KommaPropertySet(IReference subject, IReference property) {
		this(subject, property, null, null);
	}

	public KommaPropertySet(IReference subject, IReference property, Class<E> valueType, URI rdfValueType) {
		assert subject != null;
		assert property != null;
		this.subject = subject;
		this.property = property;
		this.valueType = valueType;
		this.rdfValueType = rdfValueType;
	}

	/**
	 * Inject factory to keep constructors clean.
	 *
	 * @param factory the property set factory
	 */
	@Inject
	void setFactory(PropertySetFactory factory) {
		this.factory = (KommaPropertySetFactory) factory;
	}

	/**
	 * Tracks active connections and checks if a transaction is active in the current thread
	 * or any thread that has called this method.
	 *
	 * @return <code>true</code> if a transaction is currently active, else <code>false</code>
	 */
	public synchronized boolean trackRelatedActiveTransactions() {
		ITransaction tx = factory.getManager().getTransaction();
		if (tx.isActive()) {
			if (activeTxns == null) {
				activeTxns = new ArrayList<>();
				activeTxns.add(tx);
			} else if (!activeTxns.contains(tx)) {
				activeTxns.add(tx);
			}
		} else {
			if (activeTxns != null) {
				boolean removed = false;
				for (int i = 0; i < activeTxns.size(); i++) {
					if (!activeTxns.get(i).isActive()) {
						activeTxns.remove(i--);
						removed = true;
					}
				}
				if (removed && activeTxns.isEmpty()) {
					activeTxns = null;
				}
			}
		}
		return activeTxns != null;
	}

	/**
	 * Adds <code>bean</code> as additional owner for this property set.
	 *
	 * @param bean Additional bean that uses this property set.
	 */
	public void addOwner(Object bean) {
		synchronized (ownerBeans) {
			boolean found = false;
			// search bean and clear stale references
			for (Iterator<WeakReference<Object>> it = ownerBeans.iterator(); it.hasNext(); ) {
				WeakReference<Object> ref = it.next();
				Object target = ref.get();
				if (target == null) {
					it.remove();
					// use identity equality here as two beans are always equal if they have the same
					// RDF reference
				} else if (!found && bean == target) {
					found = true;
				}
			}
			if (!found) {
				ownerBeans.add(new WeakReference<>(bean));
			}
		}
	}

	/**
	 * Refreshes all owner beans.
	 */
	protected void refreshOwners() {
		synchronized (ownerBeans) {
			// refresh beans and clear stale references
			for (Iterator<WeakReference<Object>> it = ownerBeans.iterator(); it.hasNext(); ) {
				WeakReference<Object> ref = it.next();
				Object target = ref.get();
				if (target == null) {
					it.remove();
				} else {
					refresh(target);
				}
			}
		}
	}

	protected boolean addWithoutRefresh(E o) {
		try {
			factory.getManager().add(new Statement(subject, property, convertInstance(o)));
		} catch (KommaException e) {
			throw new PropertyException(e);
		}
		return true;
	}

	/**
	 * This method always returns <code>true</code>
	 *
	 * @return <code>true</code>
	 */
	public synchronized boolean add(E o) {
		refresh();
		addWithoutRefresh(o);
		refreshOwners();
		refresh(o);
		return true;
	}

	public synchronized boolean addAll(Collection<? extends E> c) {
		refresh();
		boolean modified = false;
		ITransaction transaction = factory.getManager().getTransaction();
		try {
			boolean active = transaction.isActive();
			if (!active) {
				transaction.begin();
			}
			try {
				for (E o : c) {
					if (addWithoutRefresh(o)) {
						modified = true;
					}
				}
				if (!active) {
					transaction.commit();
				}
			} finally {
				if (!active && transaction.isActive()) {
					transaction.rollback();
				}
			}
		} catch (KommaException e) {
			throw new PropertyException(e);
		}
		refreshOwners();
		return modified;
	}

	public synchronized void clear() {
		factory.getManager().remove(new Statement(subject, property, null));
		refreshCache();
		refresh();
		refreshOwners();
	}

	protected boolean containsWithoutCache(Object o) {
		try {
			return factory.getManager().createQuery("ASK { ?s ?p ?o }")
					.setParameter("s", subject).setParameter("p", property)
					.setParameter("o", convertInstance(o)).getBooleanResult();
		} catch (KommaException e) {
			throw new PropertyException(e);
		}
	}

	public synchronized boolean contains(Object o) {
		if (!(o instanceof ILiteral)) {
			// raw literals are handled different
			List<E> cache = getCache();
			if (isCacheComplete(cache)) {
				return cache.contains(o);
			}
			if (cache != null && cache.contains(o)) {
				return true;
			}
		}
		return containsWithoutCache(o);
	}

	public synchronized boolean containsAll(Collection<?> c) {
		List<E> cache = getCache();
		if (cache != null) {
			boolean allInCache = true;
			for (Object e : c) {
				if (e instanceof ILiteral) {
					// raw literals are handled different
					if (!containsWithoutCache(e)) {
						return false;
					}
				} else if (!cache.contains(e)) {
					allInCache = false;
					break;
				}
			}
			if (allInCache || isCacheComplete(cache)) {
				return allInCache;
			}
		}
		for (Object element : c) {
			if (!containsWithoutCache(element)) {
				return false;
			}
		}
		return true;
	}

	protected Collection<Class<?>> findConcepts(URI rdfType) {
		Collection<Class<?>> roles = factory.getManager().rolesForType(rdfType);
		return WrappedIterator.create(roles.iterator())
				.filterKeep(o -> o.isInterface()).toList();
	}

	protected Object convertInstance(Object instance) {
		// handle the explicit rdf:type set with the @Type annotation
		if (rdfValueType != null
				&& !(instance instanceof IReference || instance instanceof IReferenceable)) {
			Collection<Class<?>> roles = findConcepts(rdfValueType);
			if (!roles.isEmpty()) {
				// test if instance already has the required roles
				boolean hasValidType = true;
				for (Class<?> role : roles) {
					if (!role.isAssignableFrom(instance.getClass())) {
						hasValidType = false;
						break;
					}
				}
				if (!hasValidType) {
					// create a new instance with correct rdf:type
					IEntity newEntity = factory.getManager().create(rdfValueType);
					if (newEntity instanceof Mergeable) {
						try {
							((Mergeable) newEntity).merge(instance);
						} catch (Exception e) {
							throw new KommaException(e);
						}
					}
					return newEntity;
				}
			}
		}
		return instance;
	}

	@SuppressWarnings("unchecked")
	protected IExtendedIterator<E> evaluateQueryForTypes(IQuery<?> query) {
		// handle the explicit rdf:type set from the @Type annotation
		if (rdfValueType != null) {
			Collection<Class<?>> roles = findConcepts(rdfValueType);
			if (!roles.isEmpty()) {
				Iterator<Class<?>> it = roles.iterator();
				Class<?> role1 = it.next();
				it.remove();
				return (IExtendedIterator<E>) query.evaluate(role1,
						roles.toArray(new Class<?>[0]));
			}
		}
		if (valueType != null) {
			return query.evaluate(valueType);
		}
		return (IExtendedIterator<E>) query.evaluate();
	}

	protected IQuery<?> createElementsQuery(String query, String filterPattern, int limit) {
		boolean useFilter = filterPattern != null && !filterPattern.isEmpty();
		if (useFilter || limit != Integer.MAX_VALUE) {
			StringBuilder querySb = new StringBuilder(query);
			if (useFilter) {
				querySb.insert(query.lastIndexOf('}'),
						" FILTER regex(str(?o), ?filter, \"i\")");
			}
			if (limit != Integer.MAX_VALUE) {
				querySb.append(" LIMIT " + limit);
			}
			query = querySb.toString();
		}
		IQuery<?> result = factory.getManager().createQuery(query).setParameter("s", subject)
				.setParameter("p", property);
		if (useFilter) {
			result.setParameter("filter", ".*" + filterPattern + ".*");
		}
		return result;
	}

	protected IExtendedIterator<E> createElementsIterator() {
		return createElementsIterator(null, Integer.MAX_VALUE);
	}

	protected IExtendedIterator<E> createElementsIterator(
			final String filterPattern, final int limit) {
		IQuery<?> query = createElementsQuery(QUERY, filterPattern, limit);
		return new ConvertingIterator<>(evaluateQueryForTypes(query)) {
			private List<E> list = filterPattern == null
					&& limit == Integer.MAX_VALUE ? new ArrayList<>(Math.min(
					CACHE_LIMIT, getCacheLimit())) : null;
			private E current;

			@Override
			public void close() {
				if (list != null
						&& (!hasNext() || list.size() == getCacheLimit())) {
					setCache(list);
				}
				try {
					super.close();
				} catch (KommaException e) {
					throw new PropertyException(e);
				}
			}

			protected E convert(E value) {
				if (list != null && list.size() < getCacheLimit()) {
					list.add(value);
				}
				return value;
			}

			@Override
			public boolean hasNext() {
				try {
					return super.hasNext();
				} catch (KommaException e) {
					throw new PropertyException(e);
				}
			}

			@Override
			public E next() {
				try {
					return current = super.next();
				} catch (KommaException e) {
					throw new PropertyException(e);
				}
			}

			@Override
			public void remove() {
				if (current == null) {
					throw new NoSuchElementException();
				}
				KommaPropertySet.this.remove(current);
			}
		};
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		KommaPropertySet<?> other = (KommaPropertySet<?>) obj;
		if (subject == null) {
			if (other.subject != null)
				return false;
		} else if (!subject.equals(other.subject))
			return false;
		if (!factory.equals(other.factory))
			return false;
		if (property == null) {
			return other.property == null;
		} else if (!property.equals(other.property))
			return false;
		return true;
	}

	public Set<E> getAll() {
		return this;
	}

	public void setAll(Collection<E> elements) {
		if (this == elements) {
			return;
		}
		if (elements == null) {
			clear();
			return;
		}
		ITransaction transaction = factory.getManager().getTransaction();
		try {
			boolean active = transaction.isActive();
			if (!active) {
				transaction.begin();
			}
			try {
				List<E> cache = getCache();
				if (cache == null || !cache.isEmpty()) {
					clear();
				}
				addAll(elements);
				if (!active) {
					transaction.commit();
				}
			} finally {
				if (!active && transaction.isActive()) {
					transaction.rollback();
				}
			}
		} catch (KommaException e) {
			throw new PropertyException(e);
		}
		refreshCache();
	}

	protected final List<E> getCache() {
		return cache;
	}

	protected void setCache(List<E> cache) {
		// the cache should only be used if no transactions are currently active
		// that have touched this property set so far
		boolean anyTxActive = trackRelatedActiveTransactions();
		if (!anyTxActive || cache == null) {
			this.cache = cache;
		}
	}

	protected int getCacheLimit() {
		return CACHE_LIMIT;
	}

	public Class<E> getElementType() {
		return valueType;
	}

	@SuppressWarnings("unchecked")
	public E getSingle() {
		List<E> cache = getCache();
		if (cache != null) {
			if (cache.isEmpty()) {
				if (valueType != null && valueType.isPrimitive()) {
					return (E) ConversionUtil.convertValue(valueType, 0, null);
				}
				return null;
			} else {
				return cache.get(0);
			}
		}
		try {
			try (IExtendedIterator<E> iter = createElementsIterator()) {
				if (iter.hasNext()) {
					return iter.next();
				}
				return null;
			}
		} catch (KommaException e) {
			throw new PropertyException(e);
		}
	}

	public void setSingle(E o) {
		if (o == null) {
			clear();
		} else {
			ITransaction transaction = factory.getManager().getTransaction();
			try {
				boolean active = transaction.isActive();
				if (!active) {
					transaction.begin();
				}
				try {
					List<E> cache = getCache();
					if (cache == null || !cache.isEmpty()) {
						clear();
					}
					add(o);
					if (!active) {
						transaction.commit();
					}
				} finally {
					if (!active && transaction.isActive()) {
						transaction.rollback();
					}
				}
			} catch (KommaException e) {
				throw new PropertyException(e);
			}
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((subject == null) ? 0 : subject.hashCode());
		result = prime * result + ((factory == null) ? 0 : factory.hashCode());
		result = prime * result
				+ ((property == null) ? 0 : property.hashCode());
		return result;
	}

	@Override
	public void init(Collection<? extends E> values) {
		if (getCache() == null) {
			setCache(new ArrayList<>(values));
		}
	}

	private boolean isCacheComplete(List<E> cache) {
		return cache != null && cache.size() < getCacheLimit();
	}

	public boolean isEmpty() {
		List<E> cache = getCache();
		if (cache != null) {
			return cache.isEmpty();
		}
		try (IExtendedIterator<E> iter = createElementsIterator()) {
			return !iter.hasNext();
		}
	}

	public IExtendedIterator<E> iterator() {
		List<E> cache = getCache();
		if (isCacheComplete(cache)) {
			final Iterator<E> iter = cache.iterator();
			return new NiceIterator<>() {
				private E e;

				public boolean hasNext() {
					return iter.hasNext();
				}

				public E next() {
					return e = iter.next();
				}

				public void remove() {
					KommaPropertySet.this.remove(e);
				}
			};
		}
		try {
			return createElementsIterator();
		} catch (KommaException e) {
			throw new PropertyException(e);
		}
	}

	public void refresh() {
		setCache(null);
	}

	protected void refresh(Object o) {
		factory.getManager().refresh(o);
	}

	protected void refreshCache() {
		List<E> cache = getCache();
		if (cache != null) {
			for (E e : cache) {
				refresh(e);
			}
		}
	}

	/**
	 * This method always returns <code>true</code>
	 *
	 * @return <code>true</code>
	 */
	public boolean remove(Object o) {
		refresh();
		factory.getManager().remove(new Statement(subject, property, convertInstance(o)));
		refresh(o);
		refreshOwners();
		return true;
	}

	public synchronized boolean removeAll(Collection<?> c) {
		boolean modified = false;
		try {
			ITransaction transaction = factory.getManager().getTransaction();
			boolean active = transaction.isActive();
			if (!active) {
				transaction.begin();
			}
			try {
				for (Object o : c) {
					if (remove(o)) {
						modified = true;
					}
				}
				if (!active) {
					transaction.commit();
				}
			} finally {
				if (!active && transaction.isActive()) {
					transaction.rollback();
				}
			}
		} catch (KommaException e) {
			throw new PropertyException(e);
		}
		refreshCache();
		refreshOwners();
		return modified;
	}

	public synchronized boolean retainAll(Collection<?> c) {
		refresh();
		boolean modified = false;
		try {
			ITransaction transaction = factory.getManager().getTransaction();
			boolean active = transaction.isActive();
			if (!active) {
				transaction.begin();
			}
			try {
				try (IExtendedIterator<E> e = createElementsIterator()) {
					while (e.hasNext()) {
						if (!c.contains(e.next())) {
							remove(e);
							modified = true;
						}
					}
				}
				if (!active) {
					transaction.commit();
				}
			} finally {
				if (!active && transaction.isActive()) {
					transaction.rollback();
				}
			}
		} catch (KommaException e) {
			throw new PropertyException(e);
		}
		refreshCache();
		refreshOwners();
		return modified;
	}

	public int size() {
		List<E> cache = getCache();
		if (isCacheComplete(cache)) {
			return cache.size();
		}
		try (IExtendedIterator<IReference> values = factory.getManager().createQuery(QUERY)
				.setParameter("s", subject).setParameter("p", property)
				.evaluateRestricted(IReference.class)) {
			int size;
			for (size = 0; values.hasNext(); size++) {
				values.next();
			}
			return size;
		}
	}

	public Object[] toArray() {
		List<E> cache = getCache();
		if (isCacheComplete(cache)) {
			return cache.toArray();
		}
		try (IExtendedIterator<E> iter = createElementsIterator()) {
			return iter.toList().toArray();
		}
	}

	public <T> T[] toArray(T[] a) {
		List<E> cache = getCache();
		if (isCacheComplete(cache)) {
			return cache.toArray(a);
		}
		try (IExtendedIterator<E> iter = createElementsIterator()) {
			return iter.toList().toArray(a);
		}
	}

	@Override
	public String toString() {
		List<E> cache = getCache();
		StringBuilder sb = new StringBuilder();
		Iterator<E> iter = isCacheComplete(cache) ? cache.iterator()
				: createElementsIterator();
		try {
			if (iter.hasNext()) {
				sb.append(iter.next().toString());
			}
			while (iter.hasNext()) {
				sb.append(", ");
				sb.append(iter.next());
			}
		} finally {
			if (iter instanceof AutoCloseable) {
				try {
					((AutoCloseable) iter).close();
				} catch (Exception e) {
					// ignore
				}
			}
		}
		return sb.toString();
	}

	@Override
	public Iterator<E> filter(String pattern, int limit) {
		return createElementsIterator(pattern, limit);
	}

	@Override
	public Iterator<E> filter(String pattern) {
		return filter(pattern, Integer.MAX_VALUE);
	}
}
