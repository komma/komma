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
package net.enilink.komma.em.internal.behaviours;

import java.util.AbstractSequentialList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import net.enilink.composition.annotations.Precedes;
import net.enilink.composition.annotations.Iri;
import net.enilink.composition.properties.traits.Mergeable;
import net.enilink.composition.properties.traits.Refreshable;
import net.enilink.composition.traits.Behaviour;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.vocab.rdf.RDF;
import net.enilink.komma.concepts.ResourceSupport;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IGraph;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.IValue;
import net.enilink.komma.core.Initializable;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.core.Statement;
import net.enilink.komma.core.URI;
import net.enilink.komma.util.IPartialOrderProvider;
import net.enilink.komma.util.ISparqlConstants;
import net.enilink.komma.util.LinearExtension;

/**
 * Java instance for rdf:List as a familiar interface to manipulate this List.
 * This implementation can only be modified when in autoCommit (autoFlush), or
 * when read uncommitted is supported.
 */
@Iri("http://www.w3.org/1999/02/22-rdf-syntax-ns#List")
@Precedes(ResourceSupport.class)
public abstract class RDFList extends AbstractSequentialList<Object> implements
		java.util.List<Object>, Refreshable, Mergeable, Initializable, IEntity,
		Behaviour<IEntity> {
	private static final boolean INIT_CACHE_WITH_PROPERTY_PATH = true;
	private static final Item NIL_ITEM = new Item(RDF.NIL, null, null);

	private static class Item {
		final IReference self;
		final IValue first;
		final IReference rest;

		Item(IReference self, IValue first, IReference rest) {
			this.self = self;
			this.first = first;
			this.rest = rest;
		}
	}

	private volatile int size = -1;
	private volatile List<Item> cache;

	void addStatement(IReference subj, URI pred, Object obj) {
		if (obj == null) {
			return;
		}
		getEntityManager().add(new Statement(subj, pred, obj));
	}

	private List<Item> getCache() {
		List<Item> localCache = cache;
		if (localCache == null && INIT_CACHE_WITH_PROPERTY_PATH) {
			final Map<IReference, Item> items = new LinkedHashMap<IReference, Item>();
			IExtendedIterator<Object[]> results = getEntityManager()
					.createQuery(
							ISparqlConstants.PREFIX
									+ "SELECT ?item ?first ?rest WHERE { ?self rdf:rest* ?item . ?item rdf:first ?first . OPTIONAL { ?item rdf:rest ?rest } }")
					.restrictResultType("first", IValue.class)
					.restrictResultType("item", IReference.class)
					.restrictResultType("rest", IReference.class)
					.setParameter("self", getBehaviourDelegate())
					.evaluate(Object[].class);
			for (Object[] result : results) {
				IReference self = (IReference) result[0];
				items.put(self, new Item(self, (IValue) result[1],
						(IReference) result[2]));
			}
			if (!items.containsKey(RDF.NIL)) {
				items.put(RDF.NIL, NIL_ITEM);
			}
			if (items.size() <= 2) {
				localCache = new ArrayList<Item>(items.values());
			} else {
				localCache = new LinearExtension<Item>(
						new IPartialOrderProvider<Item>() {
							@Override
							public Collection<Item> getElements() {
								return items.values();
							}

							@Override
							public Collection<Item> getSuccessors(Item element) {
								if (element.self.equals(RDF.NIL)) {
									return Collections.emptyList();
								}
								if (element.rest != null) {
									return Collections.singleton(items
											.get(element.rest));
								}
								return Collections.singleton(NIL_ITEM);
							}
						}).createLinearExtension();
			}
		}
		return cache = localCache;
	}

	private Item getItem(IReference self) {
		if (self == null || RDF.NIL.equals(self)) {
			return NIL_ITEM;
		}
		IExtendedIterator<Object[]> results = getEntityManager()
				.createQuery(
						ISparqlConstants.PREFIX
								+ "SELECT ?first ?rest WHERE { { ?item rdf:first ?first } OPTIONAL { ?item rdf:rest ?rest } }")
				.restrictResultType("first", IValue.class)
				.restrictResultType("rest", IReference.class)
				.setParameter("item", self).evaluate(Object[].class);
		try {
			if (results.hasNext()) {
				Object[] item = results.next();
				return new Item(self, (IValue) item[0], (IReference) item[1]);
			}
			return new Item(self, null, null);
		} finally {
			results.close();
		}
	}

	/**
	 * Initialize this list with data contained in <code>graph</code>.
	 */
	@Override
	public void init(IGraph graph) {
		if (graph != null && cache == null) {
			IReference list = getReference();
			List<Item> items = new ArrayList<Item>();
			Set<IReference> seen = new HashSet<IReference>();
			while (list != null && seen.add(list) && !RDF.NIL.equals(list)) {
				IReference rest = null;
				IValue first = null;
				for (IStatement stmt : graph.filter(list, null, null)) {
					if (RDF.PROPERTY_FIRST.equals(stmt.getPredicate())) {
						first = (IValue) stmt.getObject();
					} else if (RDF.PROPERTY_REST.equals(stmt.getPredicate())) {
						if (stmt.getObject() instanceof IReference) {
							rest = (IReference) stmt.getObject();
						} else {
							// invalid list data
							break;
						}
					}
				}
				if (first != null) {
					// eagerly initialize the item
					// getEntityManager().toInstance(first, null, graph);
					items.add(new Item(list, first, rest));
					list = rest;
				}
			}
			items.add(NIL_ITEM);
			// only initialize cache if list has been fully traversed
			if (list == null || RDF.NIL.equals(list)) {
				cache = items;
			}
		}
	}

	@Override
	public ListIterator<Object> listIterator(final int index) {
		final List<Item> cacheLocal = getCache(); // for thread-safety
		return new ListIterator<Object>() {
			Item next;
			Item item;

			private boolean cached = cacheLocal != null;
			private int nextIndex = index;

			private ArrayList<Item> items = cached ? new ArrayList<Item>(
					cacheLocal) : new ArrayList<Item>();

			{
				for (int i = 0; i < index; i++) {
					next();
				}
			}

			public void add(Object o) {
				boolean active = getEntityManager().getTransaction().isActive();
				try {
					if (!active) {
						getEntityManager().getTransaction().begin();
					}
					if (getReference().equals(RDF.NIL)) {
						// size == 0
						throw new KommaException(
								"cannot add a value to the nil list");
						/*
						 * list = _id = getValueFactory().createBNode();
						 * addStatement(list, RDF.FIRST,
						 * SesameProperty.createValue(List.this, o));
						 * addStatement(list, RDF.REST, RDF.NIL);
						 */
					}
					Item thisItem = getItem(getBehaviourDelegate());
					if (thisItem.first == null) {
						// size == 0
						IValue oValue = getEntityManager().toValue(o);
						item = new Item(getBehaviourDelegate(), oValue, RDF.NIL);
						addStatement(item.self, RDF.PROPERTY_FIRST, oValue);
						addStatement(item.self, RDF.PROPERTY_REST, RDF.NIL);
					} else if (item == null) {
						// index = 0
						IReference newList = getEntityManager().create();
						addStatement(newList, RDF.PROPERTY_FIRST,
								thisItem.first);
						addStatement(newList, RDF.PROPERTY_REST, thisItem.rest);
						removeStatements(getBehaviourDelegate(),
								RDF.PROPERTY_FIRST, thisItem.first);
						removeStatements(getBehaviourDelegate(),
								RDF.PROPERTY_REST, thisItem.rest);
						addStatement(getBehaviourDelegate(),
								RDF.PROPERTY_FIRST, o);
						addStatement(getBehaviourDelegate(), RDF.PROPERTY_REST,
								newList);
					} else if (!item.self.equals(RDF.NIL)) {
						IReference newList = getEntityManager().create();
						removeStatements(item.self, RDF.PROPERTY_REST,
								item.rest);
						addStatement(item.self, RDF.PROPERTY_REST, newList);
						addStatement(newList, RDF.PROPERTY_FIRST, o);
						addStatement(newList, RDF.PROPERTY_REST, item.rest);
						item = new Item(item.self, getEntityManager()
								.toValue(o), newList);
					} else {
						// index == size
						throw new NoSuchElementException();
					}
					if (!active) {
						getEntityManager().getTransaction().commit();
					}
					refresh();
				} catch (KommaException e) {
					if (!active) {
						getEntityManager().getTransaction().rollback();
					}
					throw e;
				}
			}

			public boolean hasNext() {
				if (next != null) {
					// next != RDF.NIL
					return next.first != null;
				} else if (nextIndex < items.size()) {
					next = items.get(nextIndex);
				} else if (!cached) {
					if (item == null) {
						next = getItem(getBehaviourDelegate());
					} else {
						next = getItem(item.rest);
					}
					items.add(next);
					if (next.first == null) {
						cache = new ArrayList<Item>(items);
					}
				}
				return next != null && next.first != null;
			}

			public boolean hasPrevious() {
				return nextIndex > 0;
			}

			public Object next() {
				if (hasNext()) {
					nextIndex++;
					item = next;
					next = null;
					return getEntityManager().toInstance(item.first);
				}
				throw new NoSuchElementException();
			}

			public int nextIndex() {
				return nextIndex;
			}

			public Object previous() {
				if (!hasPrevious()) {
					throw new NoSuchElementException();
				}
				nextIndex--;
				item = items.get(nextIndex - 1);
				IValue first = item.first;
				if (first == null)
					throw new NoSuchElementException();
				return getEntityManager().toInstance(first);
			}

			public int previousIndex() {
				return nextIndex - 2;
			}

			public void remove() {
				if (item == null) {
					throw new IllegalStateException(
							"next() has not yet been called");
				}

				boolean active = getEntityManager().getTransaction().isActive();
				try {
					if (!active) {
						getEntityManager().getTransaction().begin();
					}
					if (nextIndex == 1) {
						// remove index == 0
						removeStatements(item.self, RDF.PROPERTY_FIRST,
								item.first);
						Item next = getItem(item.rest);
						if (next != null) {
							removeStatements(item.self, RDF.PROPERTY_REST,
									next.self);
							if (next.first != null) {
								removeStatements(next.self, RDF.PROPERTY_FIRST,
										next.first);
								addStatement(item.self, RDF.PROPERTY_FIRST,
										next.first);
							}
							if (next.rest != null) {
								removeStatements(next.self, RDF.PROPERTY_REST,
										next.rest);
								addStatement(item.self, RDF.PROPERTY_REST,
										next.rest);
							}
						}
						item = new Item(item.self, next != null ? next.first
								: null, next != null ? next.rest : null);
					} else {
						// remove index > 0
						Item removedList = item;
						// replace previous item in list
						Item prev = items.get(nextIndex - 2);
						items.set(nextIndex - 2, new Item(prev.self,
								prev.first, prev.rest));
						// remove current item from list
						items.remove(nextIndex - 1);
						removeStatements(removedList.self, RDF.PROPERTY_FIRST,
								removedList.first);
						removeStatements(removedList.self, RDF.PROPERTY_REST,
								removedList.rest);
						removeStatements(prev.self, RDF.PROPERTY_REST,
								removedList.self);
						addStatement(prev.self, RDF.PROPERTY_REST,
								removedList.rest);
						item = prev;
					}

					next = null;
					hasNext();
					// invalidate iterator until call to next()
					item = null;

					if (!active) {
						getEntityManager().getTransaction().commit();
					}
					refresh();
				} catch (KommaException e) {
					if (!active) {
						getEntityManager().getTransaction().rollback();
					}
					throw e;
				}
			}

			public void set(Object o) {
				boolean active = getEntityManager().getTransaction().isActive();
				try {
					if (!active) {
						getEntityManager().getTransaction().begin();
					}
					if (getBehaviourDelegate().equals(RDF.NIL)) {
						// size == 0
						throw new NoSuchElementException();
					} else if (item.self.equals(RDF.NIL)) {
						// index = size
						throw new NoSuchElementException();
					} else {
						removeStatements(item.self, RDF.PROPERTY_FIRST,
								item.first);
						if (o != null) {
							addStatement(item.self, RDF.PROPERTY_FIRST, o);
						}
					}
					if (!active) {
						getEntityManager().getTransaction().commit();
					}
					refresh();
				} catch (KommaException e) {
					if (!active) {
						getEntityManager().getTransaction().rollback();
					}
					throw e;
				}
			}
		};
	}

	public void merge(Object source) {
		if (source instanceof java.util.List<?>) {
			clear();

			// works also for the read uncommitted isolation level
			Iterator<?> it = ((java.util.List<?>) source).iterator();
			if (it.hasNext()) {
				IReference current = getBehaviourDelegate();
				addStatement(current, RDF.PROPERTY_FIRST, it.next());
				while (it.hasNext()) {
					IReference last = current;
					current = getEntityManager().create();
					addStatement(current, RDF.PROPERTY_FIRST, it.next());
					addStatement(last, RDF.PROPERTY_REST, current);
				}
				addStatement(current, RDF.PROPERTY_REST, RDF.NIL);
			}
		}
	}

	@Override
	public void refresh() {
		size = -1;
		cache = null;
	}

	void removeStatements(IReference subj, URI pred, Object obj) {
		getEntityManager().remove(new Statement(subj, pred, obj));
	}

	@Override
	public int size() {
		if (this.size < 0) {
			synchronized (this) {
				if (this.size < 0) {
					List<Item> items = getCache();
					if (items != null) {
						this.size = Math.max(0, items.size() - 1);
					} else {
						items = new ArrayList<Item>();
						Item item = getItem(getBehaviourDelegate());
						items.add(item);
						int size;
						for (size = 0; item != null && item.first != null
								&& !item.self.equals(RDF.NIL); size++) {
							item = getItem(item.rest);
							items.add(item);
						}
						this.cache = items;
						this.size = size;
					}
				}
			}
		}
		return size;
	}

	@Override
	public String toString() {
		return super.toString();
	}
}
