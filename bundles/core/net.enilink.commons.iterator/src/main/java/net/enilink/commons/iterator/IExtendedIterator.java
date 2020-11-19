package net.enilink.commons.iterator;

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
 */

import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * An ExtendedIterator is a ClosableIterator on which other operations are
 * defined for convenience in iterator composition: composition, filtering in,
 * filtering out, and element mapping.
 * 
 * NOTE that the result of each of these operations consumes the base
 * iterator(s); they do not make independent copies.
 * 
 * The canonical implementation of ExtendedIterator is NiceIterator, which also
 * defines static methods for these operations that will work on any
 * ClosableIterators.
 */

public interface IExtendedIterator<T> extends Iterator<T>, AutoCloseable,
		Iterable<T> {

	/**
	 * return a new iterator which delivers all the elements of this iterator
	 * and then all the elements of the other iterator. Does not copy either
	 * iterator; they are consumed as the result iterator is consumed.
	 */
	IExtendedIterator<T> andThen(Iterator<? extends T> other);

	/**
	 * return a new iterator containing only the elements of _this_ which pass
	 * the filter _f_. The order of the elements is preserved. Does not copy
	 * _this_, which is consumed as the result is consumed.
	 */
	IExtendedIterator<T> filterKeep(Filter<? super T> f);

	/**
	 * return a new iterator containing only the elements of _this_ which are
	 * rejected by the filter _f_. The order of the elements is preserved. Does
	 * not copy _this_, which is consumed as the result is consumed.
	 */
	IExtendedIterator<T> filterDrop(Filter<? super T> f);

	/**
	 * return a new iterator where each element is the result of applying _map1_
	 * to the corresponding element of _this_. _this_ is not copied; it is
	 * consumed as the result is consumed.
	 */
	<B> IExtendedIterator<B> mapWith(IMap<? super T, ? extends B> map);

	/**
	 * Answer a list of the [remaining] elements of this iterator, in order,
	 * consuming this iterator.
	 */
	List<T> toList();

	/**
	 * Answer a set of the [remaining] elements of this iterator while
	 * consuming this iterator.
	 */
	Set<T> toSet();

	/**
	 * Close this iterator and free the corresponding resources.
	 */
	void close();
}