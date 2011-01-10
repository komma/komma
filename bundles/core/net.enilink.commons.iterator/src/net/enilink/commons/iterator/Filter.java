/*
 * (c) Copyright 2000, 2001, 2002, 2002, 2003, 2004, 2005, 2006, 2007
 * Hewlett-Packard Development Company, LP All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer. 2. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution. 3. The name of the author may not
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
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
 * 
 */

package net.enilink.commons.iterator;

import java.util.Iterator;

/**
 * Boolean functions wrapped to be used in filtering iterators.
 */
public abstract class Filter<T> {
	/**
	 * Answer true iff the object <code>o</code> is acceptable. This method may
	 * also throw an exception if the argument is of a wrong type; it is not
	 * required to return <code>false</code> in such a case.
	 */
	public abstract boolean accept(T o);

	public IExtendedIterator<T> filterKeep(Iterator<T> it) {
		return new FilterKeepIterator<T>(this, it);
	}

	public Filter<T> and(final Filter<? super T> other) {
		return other == any() ? this : new Filter<T>() {
			public boolean accept(T x) {
				return Filter.this.accept(x) && other.accept(x);
			}
		};
	}

	/**
	 * A Filter that accepts everything it's offered.
	 */
	private static final Filter<?> anyFilter = new Filter<Object>() {
		public final boolean accept(Object o) {
			return true;
		}

		@SuppressWarnings("unchecked")
		public Filter<Object> and(Filter<? super Object> other) {
			return (Filter<Object>) other;
		}

		public IExtendedIterator<Object> filterKeep(Iterator<Object> it) {
			return WrappedIterator.create(it);
		}
	};

	@SuppressWarnings("unchecked")
	public static <T> Filter<T> any() {
		return (Filter<T>) anyFilter;
	}

}