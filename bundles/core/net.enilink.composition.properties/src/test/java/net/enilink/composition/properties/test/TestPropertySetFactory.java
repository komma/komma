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

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import net.enilink.composition.properties.PropertySet;
import net.enilink.composition.properties.PropertySetFactory;
import net.enilink.composition.properties.annotations.Localized;

public class TestPropertySetFactory implements PropertySetFactory {
	class TestPropertySet<E> extends HashSet<E> implements PropertySet<E> {
		private static final long serialVersionUID = 1L;

		@Override
		public Set<E> getAll() {
			return this;
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
		public void setAll(Set<E> all) {
			clear();
			addAll(all);
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

	@Override
	public <E> PropertySet<E> createPropertySet(Object bean, String uri,
			Class<E> elementType, Annotation... annotations) {
		boolean localized = false;
		for (Annotation annotation : annotations) {
			if (Localized.class.equals(annotation.annotationType())) {
				localized = true;
			}
		}
		return new TestPropertySet<E>();
	}
}
