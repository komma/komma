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
package net.enilink.komma.em;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.infinispan.Cache;
import org.infinispan.tree.Fqn;
import org.infinispan.tree.TreeCache;
import org.infinispan.tree.TreeCacheFactory;
import net.enilink.composition.asm.BehaviourMethodProcessor;
import net.enilink.composition.cache.IPropertyCache;
import net.enilink.composition.cache.behaviours.CacheBehaviourMethodProcessor;

import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;

import net.enilink.commons.iterator.WrappedIterator;
import net.enilink.komma.em.internal.CachingEntityManager;
import net.enilink.komma.core.IEntityManager;
import net.enilink.komma.core.IReferenceable;

public class CachingEntityManagerModule extends DecoratingEntityManagerModule {
	@Override
	protected void configure() {
		super.configure();

		requireBinding(new Key<Cache<Object, Object>>() {
		});

		Multibinder<BehaviourMethodProcessor> multibinder = Multibinder
				.newSetBinder(binder(), BehaviourMethodProcessor.class);
		multibinder.addBinding().to(CacheBehaviourMethodProcessor.class);
	}

	@Singleton
	@Provides
	TreeCache<Object, Object> provideTreeCache(Cache<Object, Object> cache) {
		return new TreeCacheFactory().createTreeCache(cache);
	}

	@Singleton
	@Provides
	IPropertyCache providePropertyCache(
			final TreeCache<Object, Object> treeCache) {
		return new IPropertyCache() {
			@SuppressWarnings("serial")
			class IteratorList extends ArrayList<Object> {
			};

			@Override
			public Object put(Object entity, Object property,
					Object[] parameters, Object value) {
				Fqn fqn = Fqn.fromElements(
						((IReferenceable) entity).getReference(), property);
				boolean isIterator = value instanceof Iterator<?>;
				if (isIterator) {
					// usually an iterator cannot be cached
					// -> cache a special list instead and return an
					// iterator for this list

					IteratorList itValues = new IteratorList();
					while (((Iterator<?>) value).hasNext()) {
						itValues.add(((Iterator<?>) value).next());
					}
					value = itValues;
				}

				treeCache.put(fqn, Arrays.asList(parameters), value);

				if (isIterator) {
					return WrappedIterator.create(((List<?>) value).iterator());
				}
				return value;
			}

			@Override
			public Object get(Object entity, Object property,
					Object[] parameters) {
				Fqn fqn = Fqn.fromElements(
						((IReferenceable) entity).getReference(), property);
				Object value = treeCache.get(fqn, Arrays.asList(parameters));

				boolean isIterator = value instanceof IteratorList;

				if (isIterator) {
					return WrappedIterator.create(((List<?>) value).iterator());
				}
				return value;
			}
		};
	}

	@Override
	protected Class<? extends IEntityManager> getManagerClass() {
		return CachingEntityManager.class;
	}
}
