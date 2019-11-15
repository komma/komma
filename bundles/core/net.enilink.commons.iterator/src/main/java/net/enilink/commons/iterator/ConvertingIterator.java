/*******************************************************************************
 * Copyright (c) 2009, 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.commons.iterator;

import java.util.Iterator;

/**
 * An iterator that consumes an underlying iterator and converts its results
 * before delivering them; supports remove if the underlying iterator does.
 * 
 */
public abstract class ConvertingIterator<A, B> extends NiceIterator<B> {
	private Iterator<? extends A> base;

	/**
	 * Construct a list of the converted.
	 * 
	 * @param it
	 *            the iterator of elements to convert
	 */
	public ConvertingIterator(Iterator<? extends A> it) {
		base = it;
	}

	public B next() {
		return convert(base.next());
	}

	abstract protected B convert(A value);

	/** hasNext: defer to the base iterator */
	public boolean hasNext() {
		return base.hasNext();
	}

	/**
	 * if .remove() is allowed, delegate to the base iterator's .remove;
	 * otherwise, throw an UnsupportedOperationException.
	 */
	public void remove() {
		base.remove();
	}

	/** close: defer to the base, iff it is closable */
	public void close() {
		WrappedIterator.close(base);
	}
}