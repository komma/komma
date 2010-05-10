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

import java.util.AbstractList;
import java.util.HashSet;

import net.enilink.composition.annotations.Iri;
import net.enilink.composition.annotations.precedes;
import net.enilink.composition.properties.traits.Mergeable;
import net.enilink.composition.properties.traits.Refreshable;
import net.enilink.composition.traits.Behaviour;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.contextaware.ContextAwareConnection;
import org.openrdf.result.Result;
import org.openrdf.store.StoreException;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.komma.concepts.ResourceSupport;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.sesame.ISesameEntity;
import net.enilink.komma.sesame.ISesameManager;
import net.enilink.komma.sesame.iterators.SesameIterator;

/**
 * This behaviour provides a java.util.List interface for RDF containers.
 * 
 * @author James Leigh
 */
@Iri("http://www.w3.org/2000/01/rdf-schema#Container")
@precedes(ResourceSupport.class)
public abstract class RDFSContainer extends AbstractList<Object> implements
		java.util.List<Object>, Refreshable, Mergeable,
		Behaviour<ISesameEntity> {

	private static final int UNKNOWN = -1;

	private int _size = UNKNOWN;

	@Override
	public void add(int index, Object obj) {
		RepositoryConnection conn = getManager().getConnection();
		try {
			boolean autoCommit = conn.isAutoCommit();
			if (autoCommit)
				conn.begin();
			for (int i = size() - 1; i >= index; i--) {
				replace(i + 1, get(i));
			}
			replace(index, obj);
			if (_size > UNKNOWN)
				_size++;
			if (autoCommit)
				conn.commit();
		} catch (StoreException e) {
			throw new KommaException(e);
		}
	}

	private void assign(int index, Object o) {
		URI pred = getMemberPredicate(index);
		try {
			Value newValue = o == null ? null : getManager().getValue(o);
			ContextAwareConnection conn = getManager().getConnection();
			conn.add(getResource(), pred, newValue);
		} catch (StoreException e) {
			throw new KommaException(e);
		}
	}

	private Object createInstance(Value next) {
		return getManager().getInstance(next);
	}

	@Override
	public Object get(int index) {
		URI pred = getMemberPredicate(index);
		IExtendedIterator<Value> stmts = getStatements(pred);
		try {
			if (stmts.hasNext()) {
				Value next = stmts.next();
				return createInstance(next);
			}
			return null;
		} finally {
			stmts.close();
		}
	}

	private Value getAndSet(int index, Object o) {
		URI pred = getMemberPredicate(index);
		IExtendedIterator<Value> stmts = getStatements(pred);
		try {
			Value newValue = o == null ? null : getManager().getValue(o);
			Value oldValue = null;
			while (stmts.hasNext()) {
				oldValue = stmts.next();
				if (newValue == null || !newValue.equals(oldValue))
					stmts.remove();
			}
			if (newValue != null && !newValue.equals(oldValue)) {
				ContextAwareConnection conn = getManager().getConnection();
				conn.add(getResource(), pred, newValue);
			}
			return oldValue;
		} catch (StoreException e) {
			throw new KommaException(e);
		} finally {
			stmts.close();
		}
	}

	ISesameManager getManager() {
		return getBehaviourDelegate().getSesameManager();
	}

	private URI getMemberPredicate(int index) {
		RepositoryConnection conn = getManager().getConnection();
		Repository repository;
		repository = conn.getRepository();
		String uri = RDF.NAMESPACE + '_' + (index + 1);
		return repository.getURIFactory().createURI(uri);
	}

	Resource getResource() {
		return getBehaviourDelegate().getSesameResource();
	}

	private int getSize() {
		try {
			HashSet<URI> set = new HashSet<URI>();
			ContextAwareConnection conn = getManager().getConnection();
			Result<Statement> result = conn.match(getResource(), null, null);
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

	private IExtendedIterator<Value> getStatements(URI pred) {
		try {
			final ContextAwareConnection conn = getManager().getConnection();
			Result<Statement> result = conn.match(getResource(), pred, null);
			return new SesameIterator<Statement, Value>(result) {
				@Override
				protected Value convert(Statement stmt) throws Exception {
					return stmt.getObject();
				}

				@Override
				protected void remove(Statement stmt) throws Exception {
					conn.remove(stmt);
				}
			};
		} catch (StoreException e) {
			throw new KommaException(e);
		}
	}

	public void merge(Object source) {
		if (source instanceof java.util.List<?>) {
			RepositoryConnection conn = getManager().getConnection();
			try {
				boolean autoCommit = conn.isAutoCommit();
				if (autoCommit) {
					conn.begin();
				}
				java.util.List<?> list = (java.util.List<?>) source;
				int size = list.size();
				for (int i = 0, n = size; i < n; i++) {
					assign(i, list.get(i));
				}
				if (_size > UNKNOWN && _size < size)
					_size = size;
				if (autoCommit) {
					conn.commit();
				}
			} catch (StoreException e) {
				throw new KommaException(e);
			}
		}
	}

	public void refresh() {
		_size = UNKNOWN;
	}

	@Override
	public Object remove(int index) {
		RepositoryConnection conn = getManager().getConnection();
		try {
			boolean autoCommit = conn.isAutoCommit();
			if (autoCommit) {
				conn.begin();
			}
			Object obj = get(index);
			int size = size();
			for (int i = index; i < size - 1; i++) {
				replace(i, get(i + 1));
			}
			URI pred = getMemberPredicate(size - 1);
			IExtendedIterator<Value> stmts = getStatements(pred);
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
			if (autoCommit) {
				conn.commit();
			}
			return obj;
		} catch (StoreException e) {
			throw new KommaException(e);
		}
	}

	private void replace(int index, Object o) {
		URI pred = getMemberPredicate(index);
		Value newValue = o == null ? null : getManager().getValue(o);
		ContextAwareConnection conn = getManager().getConnection();
		try {
			boolean autoCommit = conn.isAutoCommit();
			if (autoCommit)
				conn.begin();
			conn.removeMatch(getResource(), pred, null);
			conn.add(getResource(), pred, newValue);
			if (autoCommit)
				conn.commit();
		} catch (StoreException e) {
			throw new KommaException(e);
		}
	}

	@Override
	public Object set(int index, Object obj) {
		RepositoryConnection conn = getManager().getConnection();
		try {
			boolean autoCommit = conn.isAutoCommit();
			if (autoCommit)
				conn.begin();
			Value value = getAndSet(index, obj);
			Object old = createInstance(value);
			if (autoCommit)
				conn.commit();
			return old;
		} catch (StoreException e) {
			throw new KommaException(e);
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
