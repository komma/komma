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

public class CacheModule extends AbstractModule {
	@Override
	protected void configure() {
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
	Cache<Object, Object> provideCache(CacheManager cacheManager) {
		return cacheManager.getCache();
	}

	@Singleton
	@Provides
	TreeCache<Object, Object> provideTreeCache(Cache<Object, Object> cache) {
		return new TreeCacheFactory().createTreeCache(cache);
	}
}
