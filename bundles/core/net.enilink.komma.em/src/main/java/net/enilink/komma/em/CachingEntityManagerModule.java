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
package net.enilink.komma.em;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;

import net.enilink.commons.iterator.WrappedIterator;
import net.enilink.composition.asm.BehaviourMethodProcessor;
import net.enilink.composition.cache.IPropertyCache;
import net.enilink.composition.cache.behaviours.CacheBehaviourMethodProcessor;
import net.enilink.composition.properties.PropertySet;
import net.enilink.komma.core.IEntityManager;
import net.enilink.komma.core.URI;
import net.enilink.komma.em.internal.CachedEntity;
import net.enilink.komma.em.internal.CachingEntityManager;
import net.enilink.komma.em.internal.Fqn;

public class CachingEntityManagerModule extends DecoratingEntityManagerModule {
	@Override
	protected void configure() {
		super.configure();

		requireBinding(new Key<Cache<Object, CachedEntity>>() {
		});

		Multibinder<BehaviourMethodProcessor> multibinder = Multibinder.newSetBinder(binder(),
				BehaviourMethodProcessor.class);
		multibinder.addBinding().to(CacheBehaviourMethodProcessor.class);
	}

	@Provides
	@Inject(optional = true)
	Fqn provideContextKey(@Named("modifyContexts") Set<URI> modifyContexts) {
		if (modifyContexts != null) {
			return new Fqn(modifyContexts.toArray());
		}
		return new Fqn();
	}

	/**
	 * The property cache implementation that is used within {@link PropertySet}
	 * .
	 */
	static class PropertyCache implements IPropertyCache {
		protected static Logger log = LoggerFactory.getLogger(PropertyCache.class);

		final Cache<Object, CachedEntity> cache;
		final Fqn contextKey;

		PropertyCache(Cache<Object, CachedEntity> cache, Fqn contextKey) {
			this.cache = cache;
			this.contextKey = contextKey;
		}

		@SuppressWarnings("serial")
		class IteratorList extends ArrayList<Object> {
		};

		@Override
		public Object put(Object entity, Object property, Object[] parameters, Object value) {
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
			try {
				CachedEntity cached = cache.get(entity, CachedEntity.FACTORY);
				cached.put(contextKey, new Fqn(property, Arrays.asList(parameters)), value);
			} catch (ExecutionException e) {
				log.error("Error while caching property data.", e);
			}

			if (isIterator) {
				return WrappedIterator.create(((List<?>) value).iterator());
			}
			return value;
		}

		@Override
		public Object get(Object entity, Object property, Object[] parameters) {
			CachedEntity cached = cache.getIfPresent(entity);
			if (cached != null) {
				Object value = cached.get(contextKey, new Fqn(property, Arrays.asList(parameters)));
				boolean isIterator = value instanceof IteratorList;
				if (isIterator) {
					return WrappedIterator.create(((List<?>) value).iterator());
				}
				return value;
			}
			return null;
		}
	}

	@Override
	protected void bindNoopPropertyCache() {
		// do nothing
	}

	@Singleton
	@Provides
	IPropertyCache providePropertyCache(final Cache<Object, CachedEntity> cache, final Fqn contextKey) {
		return new PropertyCache(cache, contextKey);
	}

	@Override
	protected Class<? extends IEntityManager> getManagerClass() {
		return CachingEntityManager.class;
	}
}
