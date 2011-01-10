/*
 * (c) Copyright 2003, 2004, 2005, 2006, 2007 Hewlett-Packard Development
 * Company, LP All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * 3. The name of the author may not be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package net.enilink.commons.iterator;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * NiceIterator is the standard base class implementing ExtendedIterator. It
 * provides the static methods for <code>andThen</code>, <code>filterKeep</code>
 * and <code>filterDrop</code>; these can be reused from any other class. It
 * defines equivalent instance methods for descendants and to satisfy
 * ExtendedIterator.
 * 
 * @author kers
 */

public class NiceIterator<T> implements IExtendedIterator<T> {
	public NiceIterator() {
		super();
	}

	/**
	 * default close: don't need to do anything.
	 */
	public void close() {
	}

	/**
	 * default hasNext: no elements, return false.
	 */
	public boolean hasNext() {
		return false;
	}

	protected void ensureHasNext() {
		if (hasNext() == false)
			throw new NoSuchElementException();
	}

	/**
	 * default next: throw an exception.
	 */
	public T next() {
		return noElements("empty NiceIterator");
	}

	/**
	 * Utility method for this and other (sub)classes: raise the appropriate "no
	 * more elements" exception. I note that we raised the wrong exception in at
	 * least one case ...
	 * 
	 * @param message
	 *            the string to include in the exception
	 * @return never - but we have a return type to please the compiler
	 */
	protected T noElements(String message) {
		throw new NoSuchElementException(message);
	}

	/**
	 * default remove: we have no elements, so we can't remove any.
	 */
	public void remove() {
		throw new UnsupportedOperationException(
				"remove not supported for this iterator");
	}

	/**
	 * concatenate two iterators.
	 */

	public static <T> IExtendedIterator<T> andThen(
			final Iterator<? extends T> a, final Iterator<? extends T> b) {
		final List<Iterator<? extends T>> L = new ArrayList<Iterator<? extends T>>(
				2);
		L.add(b);
		return new NiceIterator<T>() {
			private int index = 0;

			private Iterator<? extends T> current = a;

			public boolean hasNext() {
				while (current.hasNext() == false && index < L.size())
					current = L.get(index++);
				return current.hasNext();
			}

			public T next() {
				return hasNext() ? current.next() : noElements("concatenation");
			}

			public void close() {
				close(current);
				for (int i = index; i < L.size(); i += 1) {
					close(L.get(i));
				}
			}

			public void remove() {
				current.remove();
			}

			public IExtendedIterator<T> andThen(Iterator<? extends T> other) {
				L.add(other);
				return this;
			}
		};
	}

	/**
	 * make a new iterator, which is us then the other chap.
	 */
	public IExtendedIterator<T> andThen(Iterator<? extends T> other) {
		return andThen(this, other);
	}

	/**
	 * make a new iterator, which is our elements that pass the filter
	 */
	public IExtendedIterator<T> filterKeep(Filter<? super T> f) {
		return new FilterKeepIterator<T>(f, this);
	}

	/**
	 * make a new iterator, which is our elements that do not pass the filter
	 */
	public IExtendedIterator<T> filterDrop(final Filter<? super T> f) {
		return new FilterDropIterator<T>(f, this);
	}

	/**
	 * make a new iterator which is the elementwise _map1_ of the base iterator.
	 */
	public <B> IExtendedIterator<B> mapWith(IMap<? super T, ? extends B> map) {
		return new MappedIterator<T, B>(map, this);
	}

	/**
	 * If <code>it</code> is a ClosableIterator, close it. Abstracts away from
	 * tests [that were] scattered through the code.
	 */
	public static void close(Iterator<?> it) {
		if (it instanceof IClosableIterator<?>) {
			((IClosableIterator<?>) it).close();
		}
		if (it instanceof Closeable) {
			try {
				((Closeable) it).close();
			} catch (IOException e) {
				// ignore
			}
		}
	}

	static final private NiceIterator<?> emptyInstance = new NiceIterator<Object>();

	/**
	 * An iterator over no elements.
	 * 
	 * @return A class singleton which doesn't iterate.
	 */
	@SuppressWarnings("unchecked")
	static public <T> IExtendedIterator<T> emptyIterator() {
		return (IExtendedIterator<T>) emptyInstance;
	}

	/**
	 * Answer a list of the elements in order, consuming this iterator.
	 */
	public List<T> toList() {
		return asList(this);
	}

	/**
	 * Answer a list of the elements in order, consuming this iterator.
	 */
	public Set<T> toSet() {
		return asSet(this);
	}

	/**
	 * Answer a list of the elements of <code>it</code> in order, consuming this
	 * iterator. Canonical implementation of toSet().
	 */
	public static <T> Set<T> asSet(IExtendedIterator<? extends T> it) {
		Set<T> result = new HashSet<T>();
		while (it.hasNext()) {
			result.add(it.next());
		}
		return result;
	}

	/**
	 * Answer a list of the elements from <code>it</code>, in order, consuming
	 * that iterator. Canonical implementation of toList().
	 */
	public static <T> List<T> asList(Iterator<? extends T> it) {
		List<T> result = new ArrayList<T>();
		while (it.hasNext()) {
			result.add(it.next());
		}
		return result;
	}

	public Iterator<T> iterator() {
		return this;
	}
}