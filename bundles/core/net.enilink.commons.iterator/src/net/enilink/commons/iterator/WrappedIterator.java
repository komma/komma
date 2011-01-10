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

import java.util.Enumeration;
import java.util.Iterator;

/**
 * A WrappedIterator is an ExtendedIterator wrapping around a plain (or
 * presented as plain) Iterator. The wrapping allows the usual extended
 * operations (filtering, concatenating) to be done on an Iterator derived from
 * some other source. <br>
 * 
 */
public class WrappedIterator<T> extends NiceIterator<T> {
	private static class EnumerationIterator<E> implements Iterator<E> {
		Enumeration<E> e;

		private EnumerationIterator(Enumeration<E> e) {
			this.e = e;
		}

		@Override
		public boolean hasNext() {
			return e.hasMoreElements();
		}

		@Override
		public E next() {
			return e.nextElement();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException(
					"Underlying enumeration does not support removal of elements");
		}

	}

	/**
	 * set to <code>true</code> if this wrapping doesn't permit the use of
	 * .remove(). Otherwise the .remove() is delegated to the base iterator.
	 */
	protected boolean removeDenied;

	/**
	 * Answer an ExtendedIterator returning the elements of <code>it</code>. If
	 * <code>it</code> is itself an ExtendedIterator, return that; otherwise
	 * wrap <code>it</code>.
	 */
	public static <T> IExtendedIterator<T> create(Iterator<T> it) {
		return it instanceof IExtendedIterator<?> ? (IExtendedIterator<T>) it
				: new WrappedIterator<T>(it, false);
	}

	/**
	 * Answer an ExtendedIterator returning the elements of <code>e</code>.
	 */
	public static <T> IExtendedIterator<T> create(Enumeration<T> e) {
		return new WrappedIterator<T>(new EnumerationIterator<T>(e), false);
	}

	/**
	 * Answer an ExtendedIterator wrapped round <code>it</code> which does not
	 * permit <code>.remove()</code> even if <code>it</code> does.
	 */
	public static <T> WrappedIterator<T> createNoRemove(Iterator<T> it) {
		return new WrappedIterator<T>(it, true);
	}

	/** the base iterator that we wrap */
	protected final Iterator<? extends T> base;

	/** constructor: remember the base iterator */
	protected WrappedIterator(Iterator<? extends T> base) {
		this(base, false);
	}

	/**
	 * Initialise this wrapping with the given base iterator and remove-control.
	 * 
	 * @param base
	 *            the base iterator that this tierator wraps
	 * @param removeDenied
	 *            true if .remove() must throw an exception
	 */
	protected WrappedIterator(Iterator<? extends T> base, boolean removeDenied) {
		this.base = base;
		this.removeDenied = removeDenied;
	}

	/** hasNext: defer to the base iterator */
	public boolean hasNext() {
		return base.hasNext();
	}

	/** next: defer to the base iterator */
	public T next() {
		return base.next();
	}

	/**
	 * if .remove() is allowed, delegate to the base iterator's .remove;
	 * otherwise, throw an UnsupportedOperationException.
	 */
	public void remove() {
		if (removeDenied)
			throw new UnsupportedOperationException();
		base.remove();
	}

	/** close: defer to the base, iff it is closable */
	public void close() {
		close(base);
	}

	/**
	 * if <code>it</code> is a Closableiterator, close it. Abstracts away from
	 * tests [that were] scattered through the code.
	 */
	public static void close(Iterator<?> it) {
		NiceIterator.close(it);
	}
}