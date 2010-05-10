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
package net.enilink.komma.sesame.iterators;

import java.util.Iterator;

import org.openrdf.cursor.Cursor;

import net.enilink.commons.iterator.NiceIterator;
import net.enilink.komma.core.KommaException;

/**
 * A general purpose iteration wrapping Sesame's iterations. This class converts
 * the results, converts the Exceptions into ElmoRuntimeExeptions, and ensures
 * that the iteration is closed when all values have been read (on {
 * {@link #next()}).
 * 
 * @author James Leigh
 * 
 * @param <S>
 *            Type of the delegate (Statement)
 * @param <E>
 *            Type of the result
 */
public abstract class SesameIterator<S, E> extends NiceIterator<E> implements Cursor<E> {
	public static void close(Iterator<?> iter) {
		try {
			if (iter instanceof Cursor<?>) {
				((Cursor<?>) iter).close();
			}
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new KommaException(e);
		}
	}

	private Cursor<S> delegate;

	private S next, current;

	public SesameIterator(Cursor<S> delegate) {
		this.delegate = delegate;
		if (!hasNext()) {
			close();
		}
	}

	public boolean hasNext() {
		try {
			return next != null || (next = delegate.next()) != null;
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new KommaException(e);
		}
	}

	public E next() {
		try {
			current = next;
			next = null;
			if (!hasNext()) {
				close();
			}
			if (current == null) {
				return null;
			}
			return convert(current);
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new KommaException(e);
		}
	}

	public void remove() {
		try {
			remove(current);
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new KommaException(e);
		}
	}

	public void close() {
		try {
			delegate.close();
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new KommaException(e);
		}
	}

	protected abstract E convert(S element) throws Exception;

	protected void remove(S element) throws Exception {
		throw new UnsupportedOperationException();
	}

}
