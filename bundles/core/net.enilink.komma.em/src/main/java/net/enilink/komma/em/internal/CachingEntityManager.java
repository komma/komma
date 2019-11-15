/*******************************************************************************
 * Copyright (c) 2009, 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.em.internal;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import com.google.common.cache.Cache;
import com.google.inject.Inject;

import net.enilink.composition.cache.IPropertyCache;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IEntityDecorator;
import net.enilink.komma.core.IGraph;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.URI;

public class CachingEntityManager extends DecoratingEntityManager {

	@Inject
	Fqn contextKey;

	@Inject
	Cache<Object, CachedEntity> cache;

	@Inject
	IPropertyCache propertyCache;

	@Inject
	public CachingEntityManager(Set<IEntityDecorator> decorators) {
		super(decorators);
	}

	public IEntity createBean(IReference resource, Collection<URI> types, Collection<Class<?>> concepts,
			boolean restrictTypes, boolean initialize, IGraph graph) {
		CachedEntity cached = cache.getIfPresent(resource);
		Object element = cached != null ? cached.getSelf(contextKey) : null;
		if (element != null) {
			boolean hasValidTypes = true;
			if (concepts != null && !concepts.isEmpty()) {
				for (Class<?> concept : concepts) {
					if (!concept.isAssignableFrom(element.getClass())) {
						hasValidTypes = false;
						break;
					}
				}
			}
			if (hasValidTypes) {
				if (graph != null) {
					initializeBean((IEntity) element, graph);
				}
				return (IEntity) element;
			}
		}
		IEntity entity = super.createBean(resource, types, concepts, restrictTypes, initialize, graph);
		// do not cache entities created during transactions or with restricted
		// types
		if (!(restrictTypes || getTransaction().isActive())) {
			try {
				CachedEntity cachedEntity = cache.get(resource, CachedEntity.FACTORY);
				cachedEntity.setSelf(contextKey, entity);
			} catch (ExecutionException e) {
				log.error("Exception while caching entity.", e);
			}
		}
		return entity;
	}

	@Override
	protected void initializeCache(IEntity entity, Object property, Object value) {
		log.trace("init cache for {}/{}: {}", new Object[] { entity, property, value });
		propertyCache.put(entity, property, new Object[0], value);
	}

	@Override
	public void refresh(Object entity) {
		super.refresh(entity);
		CachedEntity cachedEntity = cache.getIfPresent(entity);
		if (cachedEntity != null) {
			cachedEntity.clearProperties();
			// TODO clear only properties for the current context?
		}
	}
}
