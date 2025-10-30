/*******************************************************************************
 * Copyright (c) 2024 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.composition.properties.util;

import net.enilink.composition.properties.PropertySet;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * A transient property set that is not backed by persistent storage.
 */
public class TransientPropertySet<E> implements PropertySet<E> {
	protected final Class<E> valueType;
	private volatile Set<E> values = new CopyOnWriteArraySet<>();

	public TransientPropertySet(Class<E> valueType) {
		this.valueType = valueType;
	}

	@Override
	public boolean addAll(Collection<? extends E> all) {
		return values.addAll(all);
	}

	@Override
	public boolean add(E single) {
		return values.add(single);
	}

	@Override
	public Set<E> getAll() {
		return values;
	}

	@Override
	public void setAll(Collection<E> elements) {
		values = new CopyOnWriteArraySet<>(elements);
	}

	@Override
	public E getSingle() {
		return values.stream().findFirst().orElse(null);
	}

	@Override
	public void setSingle(E single) {
		setAll(Collections.singleton(single));
	}

	@Override
	public void refresh() {
		// ignored as this property is not backed by persistent storage
	}

	@SuppressWarnings("unchecked")
	@Override
	public void init(Collection<? extends E> values) {
		setAll((Collection<E>) values);
	}

	@Override
	public Class<E> getElementType() {
		return valueType;
	}
}