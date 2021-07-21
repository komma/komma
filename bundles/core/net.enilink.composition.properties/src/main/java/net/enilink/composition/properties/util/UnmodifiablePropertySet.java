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
package net.enilink.composition.properties.util;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import net.enilink.composition.properties.PropertySet;

/**
 * Property used when only a getter method exists for the bean property.
 * 
 * @param <E>
 *            property type
 */
public class UnmodifiablePropertySet<E> implements PropertySet<E> {
	private PropertySet<E> delegate;

	UnmodifiablePropertySet(PropertySet<E> delegate) {
		this.delegate = delegate;
	}

	@Override
	public boolean add(E single) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(Collection<? extends E> all) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<E> getAll() {
		return Collections.unmodifiableSet(delegate.getAll());
	}

	public PropertySet<E> getDelegate() {
		return delegate;
	}

	@Override
	public Class<E> getElementType() {
		return delegate.getElementType();
	}

	@Override
	public E getSingle() {
		return delegate.getSingle();
	}

	@Override
	public void init(Collection<? extends E> values) {
		delegate.init(values);
	}

	@Override
	public void refresh() {
		delegate.refresh();
	}

	@Override
	public void setAll(Collection<E> elements) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setSingle(E single) {
		throw new UnsupportedOperationException();
	}
}