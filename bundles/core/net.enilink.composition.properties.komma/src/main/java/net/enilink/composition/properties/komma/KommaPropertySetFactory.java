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

import net.enilink.composition.mapping.PropertyAttribute;
import net.enilink.composition.properties.PropertySet;
import net.enilink.composition.properties.PropertySetFactory;
import net.enilink.composition.properties.util.TransientPropertySet;
import net.enilink.komma.core.IEntityManager;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IReferenceable;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIs;
import java.util.Arrays;
import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.MapMaker;
import com.google.inject.Inject;
import com.google.inject.Injector;

public class KommaPropertySetFactory implements PropertySetFactory {
	/**
	 * Index of property sets by subject. The buckets are weakly referenced and kept
	 * alive by their property sets (see {@link KommaPropertySet#bucket}) which in
	 * turn are weakly referenced by the buckets. This ensures that a property set is
	 * shared between multiple beans and can be efficiently looked up by its subject
	 * as long as it is in use.
	 */
	final ConcurrentMap<IReference, Bucket> propertySetsBySubject = new MapMaker().weakValues().makeMap();

	/**
	 * Transient property sets are not backed by the store and hence never need to be
	 * refreshed. They are only tracked to be shared between multiple beans.
	 */
	final ConcurrentMap<Key, PropertySet<?>> transientPropertySets = new MapMaker().weakValues().makeMap();

	/**
	 * Property sets of a single subject.
	 */
	static class Bucket {
		final ConcurrentMap<Key, KommaPropertySet<?>> propertySets = new MapMaker().weakValues().makeMap();
	}

	@Inject
	protected IEntityManager manager;

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
		IReference subject = ((IReferenceable) bean).getReference();
		Key cacheKey = new Key(subject, uri, elementType);

		URI rdfValueType = null;
		boolean localized = false;
		for (PropertyAttribute attribute : attributes) {
			if (PropertyAttribute.LOCALIZED.equals(attribute.getName())) {
				localized = true;
			} else if (PropertyAttribute.TYPE.equals(attribute.getName())) {
				rdfValueType = URIs.createURI(attribute.getValue());
			} else if (PropertyAttribute.TRANSIENT.equals(attribute.getName())) {
				// create a transient property set and directly return it
				return (PropertySet<E>) transientPropertySets.computeIfAbsent(cacheKey,
						k -> new TransientPropertySet<E>(elementType));
			}
		}

		final boolean isLocalized = localized;
		final URI valueType = rdfValueType;
		// the local variable keeps the bucket alive until it is referenced by the property set
		Bucket bucket = propertySetsBySubject.computeIfAbsent(subject, s -> new Bucket());
		return (PropertySet<E>) bucket.propertySets.computeIfAbsent(cacheKey, k -> {
			KommaPropertySet<E> propertySet = createPropertySetInternal(isLocalized, subject, URIs.createURI(uri),
					elementType, valueType);
			propertySet.bucket = bucket;
			propertySet.addOwner(bean);
			injector.injectMembers(propertySet);
			return propertySet;
		});
	}

	protected <E> KommaPropertySet<E> createPropertySetInternal(boolean localized, IReference subject,
	                                                            IReference property, Class<E> valueType, URI rdfValueType) {
		if (localized) {
			return (KommaPropertySet<E>) new LocalizedKommaPropertySet(subject, property);
		} else {
			return new KommaPropertySet<E>(subject, property, valueType, rdfValueType);
		}
	}

	/**
	 * Refreshes all property sets of this factory for the given subject and
	 * predicate. This is required if the underlying data has been changed without
	 * using the property sets.
	 *
	 * @param subject   the subject whose property sets should be refreshed
	 * @param predicate the predicate whose property sets should be refreshed or
	 *                  <code>null</code> to refresh all property sets of the subject
	 */
	public void refresh(IReference subject, IReference predicate) {
		Bucket bucket = propertySetsBySubject.get(subject);
		if (bucket == null) {
			return;
		}
		String predicateUri = predicate == null || predicate.getURI() == null ? null : predicate.getURI().toString();
		bucket.propertySets.forEach((key, propertySet) -> {
			if (predicateUri == null || predicateUri.equals(key.uri)) {
				propertySet.refresh();
			}
		});
	}

	/**
	 * Returns <code>true</code> if property sets for the given subject are
	 * currently tracked by this factory.
	 * <p>
	 * This is mainly intended for testing purposes.
	 *
	 * @param subject the subject
	 * @return <code>true</code> if the subject is indexed, else <code>false</code>
	 */
	protected boolean isIndexed(IReference subject) {
		return propertySetsBySubject.get(subject) != null;
	}

	/**
	 * Clears the internal cache of property sets.
	 * <p>
	 * This is mainly intended for testing purposes.
	 */
	public void clearPropertySetCache() {
		propertySetsBySubject.clear();
		transientPropertySets.clear();
	}

	/**
	 * Returns the associated entity manager.
	 *
	 * @return the entity manager instance
	 */
	public IEntityManager getManager() {
		return manager;
	}
}
