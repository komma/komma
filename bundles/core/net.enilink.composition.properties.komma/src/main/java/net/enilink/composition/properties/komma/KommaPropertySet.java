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

import net.enilink.composition.properties.PropertySet;
import net.enilink.composition.properties.exceptions.PropertyException;

import com.google.inject.Inject;

import net.enilink.commons.iterator.ConvertingIterator;
import net.enilink.commons.iterator.IClosableIterator;
import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.komma.core.IEntityManager;
import net.enilink.komma.core.ITransaction;
import net.enilink.komma.core.IQuery;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.core.Statement;

/**
 * A set for a given subject and predicate.
 * 
 * @param <E>
 */
public class KommaPropertySet<E> implements PropertySet<E>, Set<E> {
	private static final int CACHE_LIMIT = 10;

	protected static final String QUERY = "SELECT DISTINCT ?o WHERE {?s ?p ?o}";
	protected final IReference bean;
	private List<E> cache;

	private int cacheLimit = CACHE_LIMIT;

	@Inject
	protected IEntityManager manager;

	protected IReference property;

	protected Class<E> valueType;

	public KommaPropertySet(IReference bean, IReference property) {
		this(bean, property, null);
	}

	public KommaPropertySet(IReference bean, IReference property,
			Class<E> valueType) {
		assert bean != null;
		assert property != null;
		this.bean = bean;
		this.property = property;
		this.valueType = valueType;
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

	public boolean contains(Object o) {
		if (isCacheComplete()) {
			return getCache().contains(o);
		}
		if (cache != null && getCache().contains(o)) {
			return true;
		}
		try {
			return manager.createQuery("ASK {?s ?p ?o}")
					.setParameter("s", bean).setParameter("p", property)
					.setParameter("o", convertInstance(o)).getBooleanResult();
		} catch (KommaException e) {
			throw new PropertyException(e);
		}
	}

	public boolean containsAll(Collection<?> c) {
		if (isCacheComplete()) {
			return getCache().containsAll(c);
		}
		if (cache != null && getCache().containsAll(c)) {
			return true;
		}
		for (Object element : c) {
			if (!contains(element)) {
				return false;
			}
		}
		return true;
	}

	protected Object convertInstance(Object instance) {
		return instance;
	}

	@SuppressWarnings("unchecked")
	protected IExtendedIterator<E> createElementsIterator() {
		IQuery<?> query = manager.createQuery(QUERY).setParameter("s", bean)
				.setParameter("p", property);
		return new ConvertingIterator<E, E>(
				valueType == null ? (IExtendedIterator<E>) query.evaluate()
						: query.evaluate(valueType)) {
			private List<E> list = new ArrayList<E>(Math.min(10,
					getCacheLimit()));
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
		return cacheLimit;
	}

	public Class<E> getElementType() {
		return valueType;
	}

	@SuppressWarnings("unchecked")
	public E getSingle() {
		if (cache != null) {
			if (getCache().isEmpty()) {
				if (valueType != null && valueType.isPrimitive()) {
					return (E) ConversionUtil.convertValue(valueType, 0, null);
				}
				return null;
			} else {
				return getCache().get(0);
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

	private boolean isCacheComplete() {
		return cache != null && getCache().size() < getCacheLimit();
	}

	public boolean isEmpty() {
		if (cache != null) {
			return getCache().isEmpty();
		}
		IExtendedIterator<E> iter = createElementsIterator();
		try {
			return !iter.hasNext();
		} finally {
			iter.close();
		}
	}

	public Iterator<E> iterator() {
		if (isCacheComplete()) {
			final Iterator<E> iter = getCache().iterator();
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
		if (cache != null) {
			for (E e : getCache()) {
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
				if (cache == null || !getCache().isEmpty()) {
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
		if (cache != null) {
			cacheLimit = Math.max(cacheLimit, cache.size() + 1);
		} else {
			cacheLimit = CACHE_LIMIT;
		}
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
					if (cache == null || !getCache().isEmpty()) {
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
		if (isCacheComplete()) {
			return getCache().size();
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
		if (isCacheComplete()) {
			return getCache().toArray();
		}
		IExtendedIterator<E> iter = createElementsIterator();
		try {
			return iter.toList().toArray();
		} finally {
			iter.close();
		}
	}

	public <T> T[] toArray(T[] a) {
		if (isCacheComplete()) {
			return getCache().toArray(a);
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
		StringBuilder sb = new StringBuilder();
		Iterator<E> iter = isCacheComplete() ? getCache().iterator()
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
}
