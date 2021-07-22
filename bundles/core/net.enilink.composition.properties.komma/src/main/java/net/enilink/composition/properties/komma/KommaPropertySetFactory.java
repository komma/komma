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

import java.util.Arrays;
import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.MapMaker;
import com.google.inject.Inject;
import com.google.inject.Injector;

import net.enilink.composition.mapping.PropertyAttribute;
import net.enilink.composition.properties.PropertySet;
import net.enilink.composition.properties.PropertySetFactory;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IReferenceable;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIs;

public class KommaPropertySetFactory implements PropertySetFactory {
	// Use weak references to property sets.
	// This ensures that a property set is shared between multiple beans
	final ConcurrentMap<Key, PropertySet<?>> propertySetCache = new MapMaker().weakValues().makeMap();

	static class Key {
		IReference subject;
		String uri;
		Class<?> elementType;

		Key(IReference subject, String uri, Class<?> elementType) {
			this.subject = subject;
			this.uri = uri;
			this.elementType = elementType;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((elementType == null) ? 0 : elementType.hashCode());
			result = prime * result + ((subject == null) ? 0 : subject.hashCode());
			result = prime * result + ((uri == null) ? 0 : uri.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Key other = (Key) obj;
			if (elementType == null) {
				if (other.elementType != null)
					return false;
			} else if (!elementType.equals(other.elementType))
				return false;
			if (subject == null) {
				if (other.subject != null)
					return false;
			} else if (!subject.equals(other.subject))
				return false;
			if (uri == null) {
				if (other.uri != null)
					return false;
			} else if (!uri.equals(other.uri))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + Arrays.asList(subject, uri, elementType).toString();
		}
	}

	@Inject
	protected Injector injector;

	@SuppressWarnings("unchecked")
	@Override
	public <E> PropertySet<E> createPropertySet(Object bean, String uri, Class<E> elementType,
			PropertyAttribute... attributes) {
		Key cacheKey = new Key(((IReferenceable) bean).getReference(), uri, elementType);
		return (PropertySet<E>) propertySetCache.computeIfAbsent(cacheKey, k -> {
			URI predicate = URIs.createURI(uri);
			URI rdfValueType = null;
			boolean localized = false;
			for (PropertyAttribute attribute : attributes) {
				if (PropertyAttribute.LOCALIZED.equals(attribute.getName())) {
					localized = true;
				} else if (PropertyAttribute.TYPE.equals(attribute.getName())) {
					rdfValueType = URIs.createURI(attribute.getValue());
				}
			}

			PropertySet<E> propertySet;
			if (localized) {
				propertySet = (PropertySet<E>) new LocalizedKommaPropertySet((IReference) bean, predicate);
			} else {
				propertySet = new KommaPropertySet<E>((IReference) bean, predicate, elementType, rdfValueType);
			}
			injector.injectMembers(propertySet);
			return propertySet;
		});
	}

	// for testing purposes
	public ConcurrentMap<Key, PropertySet<?>> getPropertySetCache() {
		return propertySetCache;
	}
}
