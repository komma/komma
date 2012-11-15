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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import net.enilink.composition.mappers.RoleMapper;
import net.enilink.composition.properties.Filterable;
import net.enilink.composition.properties.PropertySet;
import net.enilink.composition.properties.exceptions.PropertyException;
import net.enilink.composition.properties.traits.Mergeable;

import com.google.inject.Inject;

import net.enilink.commons.iterator.ConvertingIterator;
import net.enilink.commons.iterator.Filter;
import net.enilink.commons.iterator.IClosableIterator;
import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.commons.iterator.WrappedIterator;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IEntityManager;
import net.enilink.komma.core.ILiteral;
import net.enilink.komma.core.IQuery;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IReferenceable;
import net.enilink.komma.core.ITransaction;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.core.Statement;
import net.enilink.komma.core.URI;

/**
 * A set for a given subject and predicate.
 * 
 * @param <E>
 */
public class KommaPropertySet<E> implements PropertySet<E>, Set<E>,
		Filterable<E> {
	private static final int CACHE_LIMIT = 10;

	protected static final String QUERY = "SELECT DISTINCT ?o WHERE { ?s ?p ?o }";
	protected final IReference bean;
	private volatile List<E> cache;

	@Inject
	protected IEntityManager manager;

	@Inject
	protected RoleMapper<URI> roleMapper;

	protected IReference property;

	protected Class<E> valueType;

	protected URI rdfValueType;

	public KommaPropertySet(IReference bean, IReference property) {
		this(bean, property, null, null);
	}

	public KommaPropertySet(IReference bean, IReference property,
			Class<E> valueType, URI rdfValueType) {
		assert bean != null;
		assert property != null;
		this.bean = bean;
		this.property = property;
		this.valueType = valueType;
		this.rdfValueType = rdfValueType;
	}

	/**
	 * This method always returns <code>true</code>
	 * 
	 * @return <code>true</code>
	 */
	public boolean add(E o) {
		try {
			manager.add(new Statement(bean, property, convertInstance(o)));
		} catch (KommaException e) {
			throw new PropertyException(e);
		}
		refreshEntity();
		refresh(o);
		return true;
	}

	public boolean addAll(Collection<? extends E> c) {
		boolean modified = false;
		ITransaction transaction = manager.getTransaction();

		try {
			boolean active = transaction.isActive();
			if (!active) {
				transaction.begin();
			}
			try {
				for (E o : c) {
					if (add(o)) {
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

		refreshEntity();
		return modified;
	}

	public void clear() {
		manager.remove(new Statement(bean, property, null));
		refreshCache();
		refreshEntity();
	}

	protected boolean containsWithoutCache(Object o) {
		try {
			return manager.createQuery("ASK { ?s ?p ?o }")
					.setParameter("s", bean).setParameter("p", property)
					.setParameter("o", convertInstance(o)).getBooleanResult();
		} catch (KommaException e) {
			throw new PropertyException(e);
		}
	}

	public boolean contains(Object o) {
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

	public boolean containsAll(Collection<?> c) {
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
		Collection<Class<?>> roles = new HashSet<Class<?>>();
		roleMapper.findRoles(rdfType, roles);
		return WrappedIterator.create(roles.iterator())
				.filterKeep(new Filter<Class<?>>() {
					@Override
					public boolean accept(Class<?> o) {
						return o.isInterface();
					}
				}).toList();
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
					IEntity newEntity = (IEntity) manager.create(rdfValueType);
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

	protected IQuery<?> createElementsQuery(String query, String filterPattern,
			int limit) {
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
		IQuery<?> result = manager.createQuery(query).setParameter("s", bean)
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
		return new ConvertingIterator<E, E>(evaluateQueryForTypes(query)) {
			private List<E> list = filterPattern == null
					&& limit == Integer.MAX_VALUE ? new ArrayList<E>(Math.min(
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
		if (bean == null) {
			if (other.bean != null)
				return false;
		} else if (!bean.equals(other.bean))
			return false;
		if (manager == null) {
			if (other.manager != null)
				return false;
		} else if (!manager.equals(other.manager))
			return false;
		if (property == null) {
			if (other.property != null)
				return false;
		} else if (!property.equals(other.property))
			return false;
		return true;
	}

	public Set<E> getAll() {
		return this;
	}

	protected final List<E> getCache() {
		return cache;
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
			IExtendedIterator<E> iter = createElementsIterator();
			try {
				if (iter.hasNext()) {
					return iter.next();
				}
				return null;
			} finally {
				iter.close();
			}
		} catch (KommaException e) {
			throw new PropertyException(e);
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((bean == null) ? 0 : bean.hashCode());
		result = prime * result + ((manager == null) ? 0 : manager.hashCode());
		result = prime * result
				+ ((property == null) ? 0 : property.hashCode());
		return result;
	}

	@Override
	public void init(Collection<? extends E> values) {
		setCache(new ArrayList<E>(values));
	}

	private boolean isCacheComplete(List<E> cache) {
		return cache != null && cache.size() < getCacheLimit();
	}

	public boolean isEmpty() {
		List<E> cache = getCache();
		if (cache != null) {
			return cache.isEmpty();
		}
		IExtendedIterator<E> iter = createElementsIterator();
		try {
			return !iter.hasNext();
		} finally {
			iter.close();
		}
	}

	public Iterator<E> iterator() {
		List<E> cache = getCache();
		if (isCacheComplete(cache)) {
			final Iterator<E> iter = cache.iterator();
			return new Iterator<E>() {
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
		manager.refresh(o);
	}

	protected void refreshCache() {
		List<E> cache = getCache();
		if (cache != null) {
			for (E e : cache) {
				refresh(e);
			}
		}
	}

	protected void refreshEntity() {
		refresh();
		refresh(bean);
	}

	/**
	 * This method always returns <code>true</code>
	 * 
	 * @return <code>true</code>
	 */
	public boolean remove(Object o) {
		manager.remove(new Statement(bean, property, convertInstance(o)));
		refresh(o);
		refreshEntity();
		return true;
	}

	public boolean removeAll(Collection<?> c) {
		boolean modified = false;
		try {
			ITransaction transaction = manager.getTransaction();

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
		refreshEntity();
		return modified;
	}

	public boolean retainAll(Collection<?> c) {
		boolean modified = false;
		try {
			ITransaction transaction = manager.getTransaction();

			boolean active = transaction.isActive();
			if (!active) {
				transaction.begin();
			}
			try {
				IExtendedIterator<E> e = createElementsIterator();
				try {
					while (e.hasNext()) {
						if (!c.contains(e.next())) {
							remove(e);

							modified = true;
						}
					}
				} finally {
					e.close();
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
		refreshEntity();
		return modified;
	}

	public void setAll(Set<E> set) {
		if (this == set) {
			return;
		}
		if (set == null) {
			clear();
			return;
		}
		Set<E> c = new HashSet<E>(set);

		ITransaction transaction = manager.getTransaction();

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
				addAll(c);
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

	protected void setCache(List<E> cache) {
		this.cache = cache;
	}

	public void setSingle(E o) {
		if (o == null) {
			clear();
		} else {
			ITransaction transaction = manager.getTransaction();

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

	public int size() {
		List<E> cache = getCache();
		if (isCacheComplete(cache)) {
			return cache.size();
		}
		IExtendedIterator<IReference> values = manager.createQuery(QUERY)
				.setParameter("s", bean).setParameter("p", property)
				.evaluateRestricted(IReference.class);
		try {
			int size;
			for (size = 0; values.hasNext(); size++) {
				values.next();
			}
			return size;
		} finally {
			values.close();
		}
	}

	public Object[] toArray() {
		List<E> cache = getCache();
		if (isCacheComplete(cache)) {
			return cache.toArray();
		}
		IExtendedIterator<E> iter = createElementsIterator();
		try {
			return iter.toList().toArray();
		} finally {
			iter.close();
		}
	}

	public <T> T[] toArray(T[] a) {
		List<E> cache = getCache();
		if (isCacheComplete(cache)) {
			return cache.toArray(a);
		}
		IExtendedIterator<E> iter = createElementsIterator();
		try {
			return iter.toList().toArray(a);
		} finally {
			iter.close();
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
			if (iter instanceof IClosableIterator<?>) {
				((IClosableIterator<?>) iter).close();
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
