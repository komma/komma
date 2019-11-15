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
package net.enilink.composition.properties.komma;

import java.lang.annotation.Annotation;

import net.enilink.composition.properties.PropertySet;
import net.enilink.composition.properties.PropertySetFactory;
import net.enilink.composition.properties.annotations.Localized;
import net.enilink.composition.properties.annotations.Type;

import com.google.inject.Inject;
import com.google.inject.Injector;

import net.enilink.komma.core.IReference;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIs;

public class KommaPropertySetFactory implements PropertySetFactory {
	@Inject
	protected Injector injector;

	@Override
	public <E> PropertySet<E> createPropertySet(Object bean, String uri,
			Class<E> elementType, Annotation... annotations) {
		URI predicate = URIs.createURI(uri);
		URI rdfValueType = null;
		boolean localized = false;
		for (Annotation annotation : annotations) {
			if (Localized.class.equals(annotation.annotationType())) {
				localized = true;
			} else if (Type.class.equals(annotation.annotationType())) {
				rdfValueType = URIs.createURI(((Type) annotation).value());
			}
		}
		PropertySet<E> propertySet;
		if (localized) {
			propertySet = (PropertySet<E>) new LocalizedKommaPropertySet(
					(IReference) bean, predicate);
		} else {
			propertySet = new KommaPropertySet<E>((IReference) bean, predicate,
					elementType, rdfValueType);
		}
		injector.injectMembers(propertySet);
		return propertySet;
	}
}
