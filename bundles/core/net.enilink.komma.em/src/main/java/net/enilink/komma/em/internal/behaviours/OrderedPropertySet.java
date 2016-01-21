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

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;
import java.util.Spliterator;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.commons.iterator.WrappedIterator;
import net.enilink.commons.util.IPartialOrderProvider;
import net.enilink.commons.util.LinearExtension;
import net.enilink.composition.properties.komma.KommaPropertySet;
import net.enilink.composition.properties.traits.Refreshable;
import net.enilink.komma.core.IReference;
import net.enilink.komma.em.concepts.IResource;
import net.enilink.vocab.komma.KOMMA;

public class OrderedPropertySet<E> extends KommaPropertySet<E> implements
		List<E>, RandomAccess, Cloneable, Refreshable {
	private List<E> internalList = new AbstractList<E>() {
		@Override
		public void add(int index, E element) {
			ensureCache().add(index, element);

			E succElement = null;
			if (index < getCache().size() - 1) {
				succElement = getCache().get(index + 1);
			}
			E predElement = null;
			if (index > 0) {
				predElement = getCache().get(index - 1);
			}

			if (succElement != null) {
				getPrecedes(element).add(succElement);
			}
			if (predElement != null) {
				if (succElement != null) {
					getPrecedes(predElement).remove(succElement);
				}
				getPrecedes(predElement).add(element);
			}

			OrderedPropertySet.super.add(element);
		}

		@Override
		public E get(int index) {
			return ensureCache().get(index);
		}

		@Override
		public int indexOf(Object o) {
			return ensureCache().indexOf(o);
		}

		@Override
		public int lastIndexOf(Object o) {
			return ensureCache().lastIndexOf(o);
		}

		@Override
		public E remove(int index) {
			E removed = ensureCache().remove(index);
			if (removed != null) {
				E succElement = null;
				if (index < getCache().size()) {
					succElement = getCache().get(index);
				}

				E predElement = null;
				if (index > 0) {
					predElement = getCache().get(index - 1);
				}

				if (succElement != null) {
					getPrecedes(removed).remove(succElement);
				}

				if (predElement != null) {
					if (succElement != null) {
						getPrecedes(predElement).add(succElement);
					}
					getPrecedes(predElement).remove(removed);
				}

				OrderedPropertySet.super.remove(removed);
			}
			return removed;
		}

		@Override
		public E set(int index, E element) {
			E oldElement = ensureCache().set(index, element);
			if (index < getCache().size() - 1) {
				E succElement = getCache().get(index + 1);
				if (succElement != null) {
					if (oldElement != null) {
						getPrecedes(oldElement).remove(succElement);
					}
					getPrecedes(element).add(succElement);
				}
			}

			if (index > 0) {
				E predElement = getCache().get(index - 1);
				if (predElement != null) {
					if (oldElement != null) {
						getPrecedes(predElement).remove(oldElement);
					}
					getPrecedes(predElement).add(element);
				}
			}

			OrderedPropertySet.super.add(element);

			return oldElement;
		}

		@Override
		public int size() {
			return OrderedPropertySet.super.size();
		}
	};

	public OrderedPropertySet(IReference bean, IReference property) {
		super(bean, property);
	}

	@Override
	public boolean add(E o) {
		add(ensureCache().size(), o);
		return true;
	}

	@Override
	public void add(int index, E element) {
		internalList.add(index, element);
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		return internalList.addAll(index, c);
	}

	protected IExtendedIterator<E> createElementsIterator() {
		class ElementsIterator extends WrappedIterator<E> {
			E current;
			int index = -1;
			List<E> list;

			protected ElementsIterator(List<E> list) {
				super(list.iterator());
			}

			public E next() {
				index++;
				return current = base.next();
			}

			public void remove() {
				base.remove();
				if (current != null && index < list.size()) {
					Object succElement = list.get(index);
					if (succElement != null) {
						getPrecedes(current).remove(succElement);
					}
				}
				current = null;
			}
		}

		return new ElementsIterator(ensureCache());
	}

	private final List<E> ensureCache() {
		List<E> list = getCache();
		if (list == null) {
			final List<E> values = super.createElementsIterator().toList();

			list = new LinearExtension<E>(new IPartialOrderProvider<E>() {
				@Override
				public Collection<E> getElements() {
					return values;
				}

				@Override
				public Collection<E> getSuccessors(E element) {
					return getPrecedes(element);
				}
			}).createLinearExtension(new ArrayList<E>());
			setCache(list);
		}
		return list;
	}

	@Override
	public E get(int index) {
		return internalList.get(index);
	}

	@Override
	protected int getCacheLimit() {
		return Integer.MAX_VALUE;
	}

	@SuppressWarnings("unchecked")
	private Collection<E> getPrecedes(E element) {
		return (Collection<E>) ((IResource) element)
				.get(KOMMA.PROPERTY_PRECEDES);
	}

	@Override
	public int indexOf(Object o) {
		return internalList.indexOf(o);
	}

	@Override
	public int lastIndexOf(Object o) {
		return internalList.lastIndexOf(o);
	}

	@Override
	public ListIterator<E> listIterator() {
		return internalList.listIterator();
	}

	@Override
	public ListIterator<E> listIterator(int index) {
		return internalList.listIterator(index);
	}

	public E remove(int index) {
		return internalList.remove(index);
	}

	@Override
	public boolean remove(Object o) {
		int index = ensureCache().indexOf(o);
		if (index >= 0) {
			remove(index);
			return true;
		}
		return false;
	}

	@Override
	public E set(int index, E element) {
		return internalList.set(index, element);
	}

	@Override
	public List<E> subList(int fromIndex, int toIndex) {
		return internalList.subList(fromIndex, toIndex);
	}

	@Override
	public Spliterator<E> spliterator() {
		return internalList.spliterator();
	}

}
