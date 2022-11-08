/*******************************************************************************
 * Copyright (c) 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.composition.properties.test;

import net.enilink.composition.properties.PropertySet;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

class TestPropertySet<E> extends HashSet<E> implements PropertySet<E> {
	private static final long serialVersionUID = 1L;

	@Override
	public Set<E> getAll() {
		return this;
	}

	@Override
	public void setAll(Collection<E> elements) {
		clear();
		addAll(elements);
	}

	@Override
	public E getSingle() {
		Iterator<E> it = iterator();
		if (it.hasNext()) {
			return it.next();
		}
		return null;
	}

	@Override
	public void setSingle(E single) {
		clear();
		add(single);
	}

	@Override
	public void refresh() {
		// not required
	}

	@Override
	public void init(Collection<? extends E> values) {
		// not required
	}

	@Override
	public Class<E> getElementType() {
		return null;
	}
}