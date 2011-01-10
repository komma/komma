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
import java.util.ListIterator;
import java.util.NoSuchElementException;

import net.enilink.composition.annotations.Iri;
import net.enilink.composition.annotations.precedes;
import net.enilink.composition.properties.traits.Mergeable;
import net.enilink.composition.properties.traits.Refreshable;
import net.enilink.composition.traits.Behaviour;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.vocab.rdf.RDF;
import net.enilink.komma.concepts.ResourceSupport;
import net.enilink.komma.dm.IDataManager;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.ILiteral;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.IValue;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.core.Statement;
import net.enilink.komma.core.URI;

/**
 * Java instance for rdf:List as a familiar interface to manipulate this List.
 * This implementation can only be modified when in autoCommit (autoFlush), or
 * when read uncommitted is supported.
 * 
 * @author James Leigh
 */
@Iri("http://www.w3.org/1999/02/22-rdf-syntax-ns#List")
@precedes(ResourceSupport.class)
public abstract class RDFList extends AbstractSequentialList<Object> implements
		java.util.List<Object>, Refreshable, Mergeable, IEntity,
		Behaviour<IEntity> {
	private int _size = -1;

	private RDFList parent;

	void addStatement(IReference subj, URI pred, Object obj) {
		if (obj == null) {
			return;
		}
		getEntityManager().add(new Statement(subj, pred, obj));
	}

	IValue getFirst(IReference list) {
		if (list == null) {
			return null;
		}
		IExtendedIterator<IStatement> stmts = getEntityManager().match(list,
				RDF.PROPERTY_FIRST, null);
		try {
			if (stmts.hasNext()) {
				return (IValue) stmts.next().getObject();
			}
			return null;
		} finally {
			stmts.close();
		}
	}

	IReference getRest(IReference list) {
		if (list == null) {
			return null;
		}
		IExtendedIterator<IStatement> stmts = getEntityManager().match(list,
				RDF.PROPERTY_REST, null);
		try {
			if (stmts.hasNext()) {
				return (IReference) stmts.next().getObject();
			}
			return null;
		} finally {
			stmts.close();
		}
	}

	@Override
	public ListIterator<Object> listIterator(final int index) {
		return new ListIterator<Object>() {
			IReference list;

			private ArrayList<IReference> prevLists = new ArrayList<IReference>();

			private boolean removed;
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
					if (getFirst(getBehaviourDelegate()) == null) {
						// size == 0
						list = getBehaviourDelegate();
						addStatement(list, RDF.PROPERTY_FIRST, o);
						addStatement(list, RDF.PROPERTY_REST, RDF.NIL);
					} else if (list == null) {
						// index = 0
						IValue first = getFirst(getBehaviourDelegate());
						IReference rest = getRest(getBehaviourDelegate());
						IReference newList = getEntityManager().create();
						addStatement(newList, RDF.PROPERTY_FIRST, first);
						addStatement(newList, RDF.PROPERTY_REST, rest);
						removeStatements(getBehaviourDelegate(),
								RDF.PROPERTY_FIRST, first);
						removeStatements(getBehaviourDelegate(),
								RDF.PROPERTY_REST, rest);
						addStatement(getBehaviourDelegate(),
								RDF.PROPERTY_FIRST, o);
						addStatement(getBehaviourDelegate(), RDF.PROPERTY_REST,
								newList);
					} else if (!list.equals(RDF.NIL)) {
						IReference rest = getRest(list);
						IReference newList = getEntityManager().create();
						removeStatements(list, RDF.PROPERTY_REST, rest);
						addStatement(list, RDF.PROPERTY_REST, newList);
						addStatement(newList, RDF.PROPERTY_FIRST, o);
						addStatement(newList, RDF.PROPERTY_REST, rest);
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

			private Object createInstance(IValue first) {
				if (first instanceof IReference) {
					getEntityManager().find((IReference) first);
				}
				return getEntityManager().toInstance((ILiteral) first);
			}

			public boolean hasNext() {
				IReference next;
				if (list == null) {
					next = getBehaviourDelegate();
				} else {
					next = getRest(list);
				}
				return getFirst(next) != null;
			}

			public boolean hasPrevious() {
				return prevLists.size() > 0;
			}

			public Object next() {
				if (list == null) {
					list = getBehaviourDelegate();
				} else if (!removed) {
					prevLists.add(list);
					list = getRest(list);
				} else {
					removed = false;
				}
				IValue first = getFirst(list);
				if (first == null)
					throw new NoSuchElementException();
				return createInstance(first);
			}

			public int nextIndex() {
				if (list == null)
					return 0;
				return prevLists.size() + 1;
			}

			public Object previous() {
				list = prevLists.remove(prevLists.size() - 1);
				removed = false;
				IValue first = getFirst(list);
				if (first == null)
					throw new NoSuchElementException();
				return createInstance(first);
			}

			public int previousIndex() {
				return prevLists.size() - 1;
			}

			public void remove() {
				boolean active = getEntityManager().getTransaction().isActive();
				try {
					if (!active) {
						getEntityManager().getTransaction().begin();
					}
					if (prevLists.size() < 1) {
						// remove index == 0
						IValue first = getFirst(list);
						removeStatements(list, RDF.PROPERTY_FIRST, first);
						IReference next = getRest(list);
						first = getFirst(next);
						IReference rest = getRest(next);
						removeStatements(list, RDF.PROPERTY_REST, next);
						if (first != null) {
							removeStatements(next, RDF.PROPERTY_FIRST, first);
							addStatement(list, RDF.PROPERTY_FIRST, first);
						}
						if (rest != null) {
							removeStatements(next, RDF.PROPERTY_REST, rest);
							addStatement(list, RDF.PROPERTY_REST, rest);
						}
					} else {
						// remove index > 0
						IReference removedList = list;
						list = prevLists.remove(prevLists.size() - 1);
						IValue first = getFirst(removedList);
						IReference rest = getRest(removedList);
						removeStatements(removedList, RDF.PROPERTY_FIRST, first);
						removeStatements(removedList, RDF.PROPERTY_REST, rest);
						removeStatements(list, RDF.PROPERTY_REST, removedList);
						addStatement(list, RDF.PROPERTY_REST, rest);
					}
					if (!active) {
						getEntityManager().getTransaction().commit();
					}
					removed = true;
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
					} else if (list.equals(RDF.NIL)) {
						// index = size
						throw new NoSuchElementException();
					} else {
						IValue first = getFirst(list);
						removeStatements(list, RDF.PROPERTY_FIRST, first);
						if (o != null) {
							addStatement(list, RDF.PROPERTY_FIRST, o);
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
			addAll((java.util.List<?>) source);
		}
	}

	public void refresh() {
		_size = -1;
		if (parent != null) {
			parent.refresh();
		}
	}

	void removeStatements(IReference subj, URI pred, Object obj) {
		getEntityManager().remove(new Statement(subj, pred, obj));
	}

	@Override
	public int size() {
		if (_size < 0) {
			synchronized (this) {
				if (_size < 0) {
					IReference list = getBehaviourDelegate();
					int size;
					for (size = 0; list != null && !list.equals(RDF.NIL); size++) {
						IReference nlist = getRest(list);
						if (nlist == null && getFirst(list) == null)
							break;
						list = nlist;
					}
					_size = size;
				}
			}
		}
		return _size;
	}

	@Override
	public String toString() {
		return super.toString();
	}
}
