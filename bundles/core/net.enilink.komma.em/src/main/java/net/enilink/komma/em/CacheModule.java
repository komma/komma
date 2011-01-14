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

import org.infinispan.Cache;
import org.infinispan.config.Configuration;
import org.infinispan.config.GlobalConfiguration;
import org.infinispan.manager.CacheManager;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.tree.TreeCache;
import org.infinispan.tree.TreeCacheFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;

import net.enilink.komma.util.IClosable;

public class CacheModule extends AbstractModule {
	Cache<Object, Object> cache;

	@Override
	protected void configure() {
		Multibinder<IClosable> closableBinder = Multibinder
				.<IClosable> newSetBinder(binder(),
						new TypeLiteral<IClosable>() {
						});
		closableBinder.addBinding().toInstance(new IClosable() {
			@Override
			public void close() {
				if (cache != null) {
					cache.stop();
					cache.getCacheManager().stop();
					cache = null;
				}
			}
		});
	}

	@Provides
	@Singleton
	CacheManager provideCacheManager() {
		GlobalConfiguration globalConfig = new GlobalConfiguration();

		Configuration config = new Configuration();
		config.setInvocationBatchingEnabled(true);
		config.setCacheMode(Configuration.CacheMode.LOCAL);
		config.setEvictionMaxEntries(10000);
		return new DefaultCacheManager(globalConfig, config);
	}

	@Provides
	@Singleton
	Cache<Object, Object> provideCache(CacheManager cacheManager) {
		return cache = cacheManager.getCache();
	}

	@Provides
	@Singleton
	TreeCache<Object, Object> provideTreeCache(Cache<Object, Object> cache) {
		return new TreeCacheFactory().createTreeCache(cache);
	}
}
