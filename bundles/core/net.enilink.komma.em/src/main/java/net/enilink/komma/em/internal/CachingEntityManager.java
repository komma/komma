/*******************************************************************************
 * Copyright (c) 2009, 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.em.internal;

import java.util.Collection;
import java.util.Set;

import org.infinispan.tree.Fqn;
import org.infinispan.tree.TreeCache;
import net.enilink.composition.cache.IPropertyCache;

import com.google.inject.Inject;

import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IEntityDecorator;
import net.enilink.komma.core.IGraph;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.URI;

public class CachingEntityManager extends DecoratingEntityManager {

	@Inject
	Fqn baseFqn;

	@Inject
	TreeCache<Object, Object> cache;

	@Inject
	IPropertyCache propertyCache;

	@Inject
	public CachingEntityManager(Set<IEntityDecorator> decorators) {
		super(decorators);
	}

	public IEntity createBean(IReference resource, Collection<URI> types,
			Collection<Class<?>> concepts, boolean restrictTypes,
			boolean initialize, IGraph graph) {
		Object element = cache.get(Fqn.fromRelativeElements(baseFqn, resource),
				"");
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
		IEntity entity = super.createBean(resource, types, concepts,
				restrictTypes, initialize, graph);
		// do not cache entities created during transactions or with restricted
		// types
		if (!(restrictTypes || getTransaction().isActive())) {
			cache.put(Fqn.fromRelativeElements(baseFqn, resource), "", entity);
		}
		return entity;
	}

	@Override
	protected void initializeCache(IEntity entity, Object property, Object value) {
		log.trace("init cache for {}/{}: {}", new Object[] { entity, property,
				value });
		propertyCache.put(entity, property, new Object[0], value);
	}

	@Override
	public void refresh(Object entity) {
		super.refresh(entity);
		cache.removeNode(Fqn
				.fromRelativeElements(baseFqn, entity, "properties"));
	}
}
