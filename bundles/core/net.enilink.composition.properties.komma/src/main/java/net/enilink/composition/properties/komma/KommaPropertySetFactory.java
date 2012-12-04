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

import java.lang.annotation.Annotation;

import net.enilink.composition.properties.PropertySet;
import net.enilink.composition.properties.PropertySetFactory;
import net.enilink.composition.properties.annotations.Localized;
import net.enilink.composition.properties.annotations.Type;
import net.enilink.composition.properties.util.UnmodifiablePropertySet;

import com.google.inject.Inject;
import com.google.inject.Injector;

import net.enilink.komma.core.IReference;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIImpl;

public class KommaPropertySetFactory implements PropertySetFactory {
	@Inject
	protected Injector injector;

	@Override
	public <E> PropertySet<E> createPropertySet(Object bean, String uri,
			Class<E> elementType, boolean readonly, Annotation... annotations) {
		URI predicate = URIImpl.createURI(uri);
		URI rdfValueType = null;
		boolean localized = false;
		for (Annotation annotation : annotations) {
			if (Localized.class.equals(annotation.annotationType())) {
				localized = true;
			} else if (Type.class.equals(annotation.annotationType())) {
				rdfValueType = URIImpl.createURI(((Type) annotation).value());
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
		if (readonly) {
			propertySet = new UnmodifiablePropertySet<E>(propertySet);
		}
		injector.injectMembers(propertySet);
		return propertySet;
	}
}
