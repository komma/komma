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
import java.lang.annotation.Annotation;

import net.enilink.composition.properties.PropertySet;
import net.enilink.composition.properties.PropertySetDescriptor;
import net.enilink.composition.properties.PropertySetFactory;

import net.enilink.komma.core.IReference;
import net.enilink.komma.core.URIImpl;

public class KommaPropertySetFactory implements PropertySetFactory {
	@Override
	public <E> PropertySetDescriptor<E> createDescriptor(
			PropertyDescriptor property, String uri, boolean readOnly) {
		return new KommaPropertySetDescriptor<E>(property, uri);
	}

	@Override
	public <E> PropertySet<E> createPropertySet(Object bean, String uri,
			boolean readonly, Annotation... annotations) {
		return new KommaPropertySet<E>((IReference) bean,
				URIImpl.createURI(uri));
	}
}
