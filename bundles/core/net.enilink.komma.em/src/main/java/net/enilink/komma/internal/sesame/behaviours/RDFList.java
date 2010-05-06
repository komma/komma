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

import java.util.AbstractSequentialList;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import net.enilink.composition.annotations.Iri;
import net.enilink.composition.annotations.precedes;
import net.enilink.composition.properties.traits.Mergeable;
import net.enilink.composition.properties.traits.Refreshable;
import net.enilink.composition.traits.Behaviour;
import org.openrdf.model.BNode;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.contextaware.ContextAwareConnection;
import org.openrdf.result.Result;
import org.openrdf.store.StoreException;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.komma.model.ObjectSupport;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.sesame.ISesameEntity;
import net.enilink.komma.sesame.ISesameManager;
import net.enilink.komma.sesame.iterators.SesameIterator;

/**
 * Java instance for rdf:List as a familiar interface to manipulate this List.
 * This implementation can only be modified when in autoCommit (autoFlush), or
 * when read uncommitted is supported.
 * 
 * @author James Leigh
 */
@Iri("http://www.w3.org/1999/02/22-rdf-syntax-ns#List")
@precedes(ObjectSupport.class)
public abstract class RDFList extends AbstractSequentialList<Object>
		implements java.util.List<Object>, Refreshable, Mergeable,
		Behaviour<ISesameEntity> {
	private int _size = -1;

	private RDFList parent;

	void addStatement(Resource subj, URI pred, Value obj) {
		if (obj == null)
			return;
		try {
			ContextAwareConnection conn = getManager().getConnection();
			conn.add(subj, pred, obj);
		} catch (StoreException e) {
			throw new KommaException(e);
		}
	}

	Value getFirst(Resource list) {
		if (list == null) {
			return null;
		}
		IExtendedIterator<Value> stmts = getStatements(list, RDF.FIRST, null);
		try {
			if (stmts.hasNext()) {
				return stmts.next();
			}
			return null;
		} finally {
			stmts.close();
		}
	}

	ISesameManager getManager() {
		return getBehaviourDelegate().getSesameManager();
	}

	Resource getResource() {
		return getBehaviourDelegate().getSesameResource();
	}

	Resource getRest(Resource list) {
		if (list == null) {
			return null;
		}
		IExtendedIterator<Value> stmts = getStatements(list, RDF.REST, null);
		try {
			if (stmts.hasNext()) {
				return (Resource) stmts.next();
			}
			return null;
		} finally {
			stmts.close();
		}
	}

	private IExtendedIterator<Value> getStatements(Resource subj, URI pred,
			Value obj) {
		try {
			ContextAwareConnection conn = getManager().getConnection();
			Result<Statement> stmts = conn.match(subj, pred, obj);
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

	ValueFactory getValueFactory() {
		RepositoryConnection conn = getManager().getConnection();
		return conn.getValueFactory();
	}

	@Override
	public ListIterator<Object> listIterator(final int index) {
		return new ListIterator<Object>() {
			Resource list;

			private ArrayList<Resource> prevLists = new ArrayList<Resource>();

			private boolean removed;
			{
				for (int i = 0; i < index; i++) {
					next();
				}
			}

			public void add(Object o) {
				RepositoryConnection conn = getManager().getConnection();
				try {
					boolean autoCommit = conn.isAutoCommit();
					if (autoCommit) {
						conn.begin();
					}
					if (getResource().equals(RDF.NIL)) {
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
					Value value = o == null ? null : getManager().getValue(o);
					if (getFirst(getResource()) == null) {
						// size == 0
						list = getResource();
						addStatement(list, RDF.FIRST, value);
						addStatement(list, RDF.REST, RDF.NIL);
					} else if (list == null) {
						// index = 0
						Value first = getFirst(getResource());
						Resource rest = getRest(getResource());
						BNode newList = getValueFactory().createBNode();
						addStatement(newList, RDF.FIRST, first);
						addStatement(newList, RDF.REST, rest);
						removeStatements(getResource(), RDF.FIRST, first);
						removeStatements(getResource(), RDF.REST, rest);
						addStatement(getResource(), RDF.FIRST, value);
						addStatement(getResource(), RDF.REST, newList);
					} else if (!list.equals(RDF.NIL)) {
						Resource rest = getRest(list);
						BNode newList = getValueFactory().createBNode();
						removeStatements(list, RDF.REST, rest);
						addStatement(list, RDF.REST, newList);
						addStatement(newList, RDF.FIRST, value);
						addStatement(newList, RDF.REST, rest);
					} else {
						// index == size
						throw new NoSuchElementException();
					}
					if (autoCommit) {
						conn.commit();
					}
					refresh();
				} catch (StoreException e) {
					throw new KommaException(e);
				}
			}

			private Object createInstance(Value first) {
				return getManager().getInstance(first);
			}

			public boolean hasNext() {
				Resource next;
				if (list == null) {
					next = getResource();
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
					list = getResource();
				} else if (!removed) {
					prevLists.add(list);
					list = getRest(list);
				} else {
					removed = false;
				}
				Value first = getFirst(list);
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
				Value first = getFirst(list);
				if (first == null)
					throw new NoSuchElementException();
				return createInstance(first);
			}

			public int previousIndex() {
				return prevLists.size() - 1;
			}

			public void remove() {
				RepositoryConnection conn = getManager().getConnection();
				try {
					boolean autoCommit = conn.isAutoCommit();
					if (autoCommit) {
						conn.begin();
					}
					if (prevLists.size() < 1) {
						// remove index == 0
						Value first = getFirst(list);
						removeStatements(list, RDF.FIRST, first);
						Resource next = getRest(list);
						first = getFirst(next);
						Resource rest = getRest(next);
						removeStatements(list, RDF.REST, next);
						if (first != null) {
							removeStatements(next, RDF.FIRST, first);
							addStatement(list, RDF.FIRST, first);
						}
						if (rest != null) {
							removeStatements(next, RDF.REST, rest);
							addStatement(list, RDF.REST, rest);
						}
					} else {
						// remove index > 0
						Resource removedList = list;
						list = prevLists.remove(prevLists.size() - 1);
						Value first = getFirst(removedList);
						Resource rest = getRest(removedList);
						removeStatements(removedList, RDF.FIRST, first);
						removeStatements(removedList, RDF.REST, rest);
						removeStatements(list, RDF.REST, removedList);
						addStatement(list, RDF.REST, rest);
					}
					if (autoCommit) {
						conn.commit();
					}
					removed = true;
					refresh();
				} catch (StoreException e) {
					throw new KommaException(e);
				}
			}

			public void set(Object o) {
				RepositoryConnection conn = getManager().getConnection();
				try {
					boolean autoCommit = conn.isAutoCommit();
					if (autoCommit) {
						conn.begin();
					}
					if (getResource().equals(RDF.NIL)) {
						// size == 0
						throw new NoSuchElementException();
					} else if (list.equals(RDF.NIL)) {
						// index = size
						throw new NoSuchElementException();
					} else {
						Value first = getFirst(list);
						removeStatements(list, RDF.FIRST, first);
						if (o != null) {
							Value obj = getManager().getValue(o);
							addStatement(list, RDF.FIRST, obj);
						}
					}
					if (autoCommit) {
						conn.commit();
					}
					refresh();
				} catch (StoreException e) {
					throw new KommaException(e);
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

	void removeStatements(Resource subj, URI pred, Value obj) {
		try {
			ContextAwareConnection conn = getManager().getConnection();
			conn.removeMatch(subj, pred, obj);
		} catch (StoreException e) {
			throw new KommaException(e);
		}
	}

	@Override
	public int size() {
		if (_size < 0) {
			synchronized (this) {
				if (_size < 0) {
					Resource list = getResource();
					int size;
					for (size = 0; list != null && !list.equals(RDF.NIL); size++) {
						Resource nlist = getRest(list);
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
