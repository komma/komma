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

import java.util.AbstractList;
import java.util.HashSet;

import net.enilink.composition.annotations.Iri;
import net.enilink.composition.annotations.precedes;
import net.enilink.composition.properties.traits.Mergeable;
import net.enilink.composition.properties.traits.Refreshable;
import net.enilink.composition.traits.Behaviour;

import net.enilink.commons.iterator.ConvertingIterator;
import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.vocab.rdf.RDF;
import net.enilink.komma.concepts.ResourceSupport;
import net.enilink.komma.dm.IDataManager;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.IValue;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.core.Statement;
import net.enilink.komma.core.URI;

/**
 * This behaviour provides a java.util.List interface for RDF containers.
 * 
 */
@Iri("http://www.w3.org/2000/01/rdf-schema#Container")
@precedes(ResourceSupport.class)
public abstract class RDFSContainer extends AbstractList<Object> implements
		java.util.List<Object>, Refreshable, Mergeable, IEntity,
		Behaviour<IEntity> {

	private static final int UNKNOWN = -1;

	private int _size = UNKNOWN;

	@Override
	public void add(int index, Object obj) {
		boolean active = getEntityManager().getTransaction().isActive();
		try {
			if (!active) {
				getEntityManager().getTransaction().begin();
			}
			for (int i = size() - 1; i >= index; i--) {
				replace(i + 1, get(i));
			}
			replace(index, obj);
			if (_size > UNKNOWN)
				_size++;
			if (!active) {
				getEntityManager().getTransaction().commit();
			}
		} catch (KommaException e) {
			if (!active) {
				getEntityManager().getTransaction().rollback();
			}
			throw e;
		}
	}

	private void assign(int index, Object o) {
		URI pred = getMemberPredicate(index);
		getEntityManager().add(new Statement(getBehaviourDelegate(), pred, o));
	}

	private Object createInstance(IValue next) {
		return getEntityManager().toInstance(next);
	}

	@Override
	public Object get(int index) {
		URI pred = getMemberPredicate(index);
		IExtendedIterator<IValue> stmts = getStatements(pred);
		try {
			if (stmts.hasNext()) {
				IValue next = stmts.next();
				return createInstance(next);
			}
			return null;
		} finally {
			stmts.close();
		}
	}

	private IValue getAndSet(int index, Object o) {
		URI pred = getMemberPredicate(index);
		IExtendedIterator<IValue> stmts = getStatements(pred);
		try {
			IValue newValue = getEntityManager().toValue(o);
			IValue oldValue = null;
			while (stmts.hasNext()) {
				oldValue = stmts.next();
				if (newValue == null || !newValue.equals(oldValue)) {
					stmts.remove();
				}
			}
			if (newValue != null && !newValue.equals(oldValue)) {
				getEntityManager().add(
						new Statement(getBehaviourDelegate(), pred, newValue));
			}
			return oldValue;
		} finally {
			stmts.close();
		}
	}

	private URI getMemberPredicate(int index) {
		return RDF.NAMESPACE_URI.appendFragment("_" + (index + 1));
	}

	private int getSize() {
		try {
			HashSet<IReference> set = new HashSet<IReference>();
			IExtendedIterator<IStatement> result = getEntityManager().match(
					getBehaviourDelegate(), null, null);
			try {
				while (result.hasNext()) {
					set.add(result.next().getPredicate());
				}
			} finally {
				result.close();
			}
			int index = 0;
			while (set.contains(getMemberPredicate(index)))
				index++;
			return index;
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new KommaException(e);
		}
	}

	private IExtendedIterator<IValue> getStatements(URI pred) {
		return new ConvertingIterator<IStatement, IValue>(getEntityManager()
				.match(getBehaviourDelegate(), pred, null)) {
			IStatement currentStmt;

			protected IValue convert(IStatement stmt) {
				currentStmt = stmt;
				return (IValue) stmt.getObject();
			}

			@Override
			public void remove() {
				if (currentStmt != null) {
					getEntityManager().remove(currentStmt);
					currentStmt = null;
				}
			}
		};
	}

	public void merge(Object source) {
		if (source instanceof java.util.List<?>) {
			boolean active = getEntityManager().getTransaction().isActive();
			try {
				if (!active) {
					getEntityManager().getTransaction().begin();
				}
				java.util.List<?> list = (java.util.List<?>) source;
				int size = list.size();
				for (int i = 0, n = size; i < n; i++) {
					assign(i, list.get(i));
				}
				if (_size > UNKNOWN && _size < size)
					_size = size;
				if (!active) {
					getEntityManager().getTransaction().commit();
				}
			} catch (KommaException e) {
				if (!active) {
					getEntityManager().getTransaction().rollback();
				}
				throw e;
			}
		}
	}

	public void refresh() {
		_size = UNKNOWN;
	}

	@Override
	public Object remove(int index) {
		boolean active = getEntityManager().getTransaction().isActive();
		try {
			if (!active) {
				getEntityManager().getTransaction().begin();
			}
			Object obj = get(index);
			int size = size();
			for (int i = index; i < size - 1; i++) {
				replace(i, get(i + 1));
			}
			URI pred = getMemberPredicate(size - 1);
			IExtendedIterator<IValue> stmts = getStatements(pred);
			try {
				while (stmts.hasNext()) {
					stmts.next();
					stmts.remove();
				}
			} finally {
				stmts.close();
			}
			if (_size > UNKNOWN)
				_size--;
			if (!active) {
				getEntityManager().getTransaction().commit();
			}
			return obj;
		} catch (KommaException e) {
			if (!active) {
				getEntityManager().getTransaction().rollback();
			}
			throw e;
		}
	}

	private void replace(int index, Object o) {
		URI pred = getMemberPredicate(index);
		boolean active = getEntityManager().getTransaction().isActive();
		try {
			if (!active) {
				getEntityManager().getTransaction().begin();
			}
			getEntityManager().remove(
					new Statement(getBehaviourDelegate(), pred, null));
			getEntityManager().add(
					new Statement(getBehaviourDelegate(), pred, o));
			if (!active) {
				getEntityManager().getTransaction().commit();
			}
		} catch (KommaException e) {
			if (!active) {
				getEntityManager().getTransaction().rollback();
			}
			throw e;
		}
	}

	@Override
	public Object set(int index, Object obj) {
		boolean active = getEntityManager().getTransaction().isActive();
		try {
			if (!active) {
				getEntityManager().getTransaction().begin();
			}
			IValue value = getAndSet(index, obj);
			Object old = createInstance(value);
			if (!active) {
				getEntityManager().getTransaction().commit();
			}
			return old;
		} catch (KommaException e) {
			if (!active) {
				getEntityManager().getTransaction().rollback();
			}
			throw e;
		}
	}

	@Override
	public int size() {
		if (_size < 0) {
			synchronized (this) {
				if (_size < 0) {
					int index = getSize();
					_size = index;
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
