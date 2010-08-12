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
package net.enilink.composition.properties.komma;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Set;

import net.enilink.composition.annotations.Iri;
import net.enilink.composition.properties.PropertySet;
import net.enilink.composition.properties.PropertySetDescriptor;
import net.enilink.composition.properties.annotations.localized;
import net.enilink.composition.properties.util.UnmodifiablePropertySet;

import net.enilink.komma.core.IReference;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIImpl;

class KommaPropertySetDescriptor<E> implements PropertySetDescriptor<E> {
	private String name;

	private Class<?> type;

	private boolean localized;

	private boolean readOnly;

	private URI predicate;

	public KommaPropertySetDescriptor(PropertyDescriptor property,
			String predicate) {
		Method getter = property.getReadMethod();
		localized = getter.isAnnotationPresent(localized.class);
		readOnly = property.getWriteMethod() == null;

		Iri rdf = getter.getAnnotation(Iri.class);
		if (predicate != null) {
			setPredicate(predicate);
		} else if (rdf != null && rdf.value() != null) {
			setPredicate(rdf.value());
		}
		assert this.predicate != null;

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
	@SuppressWarnings("unchecked")
	public PropertySet<E> createPropertySet(Object bean) {
		PropertySet<E> propertySet;
		if (localized) {
			propertySet = (PropertySet<E>) new LocalizedKommaPropertySet(
					(IReference) bean, predicate);
		} else {
			propertySet = new KommaPropertySet<E>((IReference) bean, predicate,
					(Class<E>) type);
		}

		if (readOnly) {
			return new UnmodifiablePropertySet<E>(propertySet);
		}

		return propertySet;
	}

	@Override
	public String getName() {
		return name;
	}

	private void setPredicate(String uri) {
		predicate = URIImpl.createURI(uri);
	}
}