/*******************************************************************************
 * Copyright (c) 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.composition.properties.test;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import net.enilink.composition.properties.PropertySet;
import net.enilink.composition.properties.PropertySetDescriptor;
import net.enilink.composition.properties.PropertySetDescriptorFactory;
import net.enilink.composition.properties.annotations.Localized;

public class TestPropertySetDescriptorFactory implements
		PropertySetDescriptorFactory {
	class TestPropertySetDescriptor<E> implements PropertySetDescriptor<E> {
		private String name;

		private Class<?> type;

		private boolean localized;

		private boolean readOnly;

		public TestPropertySetDescriptor(PropertyDescriptor property,
				String predicate) {
			Method getter = property.getReadMethod();
			localized = getter.isAnnotationPresent(Localized.class);
			readOnly = property.getWriteMethod() == null;

			name = property.getName();
			type = property.getPropertyType();
			if (Set.class.equals(type)) {
				Type t = property.getReadMethod().getGenericReturnType();
				if (t instanceof ParameterizedType) {
					ParameterizedType pt = (ParameterizedType) t;
					Type[] args = pt.getActualTypeArguments();
					if (args.length == 1 && args[0] instanceof Class<?>) {
						type = (Class<?>) args[0];
					}
				}
			}
		}

		@Override
		public PropertySet<E> createPropertySet(Object bean) {
			return new TestPropertySet<E>();
		}

		@Override
		public String getName() {
			return name;
		}
	}

	class TestPropertySet<E> extends HashSet<E> implements PropertySet<E> {
		private static final long serialVersionUID = 1L;

		@Override
		public Set<E> getAll() {
			return this;
		}

		@Override
		public Object getSingle() {
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
	public <E> PropertySetDescriptor<E> createDescriptor(
			PropertyDescriptor property, String uri, boolean readOnly) {
		return new TestPropertySetDescriptor<E>(property, uri);
	}
}
