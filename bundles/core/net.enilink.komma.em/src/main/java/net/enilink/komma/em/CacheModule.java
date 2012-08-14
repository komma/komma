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

import java.util.List;

import org.infinispan.Cache;
import org.infinispan.config.Configuration;
import org.infinispan.config.GlobalConfiguration;
import org.infinispan.manager.CacheContainer;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.tree.Fqn;
import org.infinispan.tree.TreeCache;
import org.infinispan.tree.TreeCacheFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;

import net.enilink.komma.dm.change.DataChangeTracker;
import net.enilink.komma.dm.change.IDataChange;
import net.enilink.komma.dm.change.IDataChangeListener;
import net.enilink.komma.dm.change.IDataChangeTracker;
import net.enilink.komma.dm.change.IStatementChange;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.util.IClosable;

public class CacheModule extends AbstractModule {
	static class CacheClosable implements IClosable {
		@Inject
		Cache<Object, Object> cache;

		@Override
		public void close() {
			if (cache != null) {
				cache.stop();
				cache.getCacheManager().stop();
				cache = null;
			}
		}
	}

	@Override
	protected void configure() {
		Multibinder<IClosable> closableBinder = Multibinder
				.<IClosable> newSetBinder(binder(),
						new TypeLiteral<IClosable>() {
						});
		closableBinder.addBinding().to(CacheClosable.class);
	}

	@Provides
	@Singleton
	CacheContainer provideCacheContainer() {
		GlobalConfiguration globalConfig = new GlobalConfiguration();

		Configuration config = new Configuration();
		config.setInvocationBatchingEnabled(true);
		config.setCacheMode(Configuration.CacheMode.LOCAL);
		config.setEvictionMaxEntries(10000);

		// workaround for classloading issues w/ factory methods
		// http://community.jboss.org/wiki/ModuleCompatibleClassloadingGuide
		ClassLoader oldTCCL = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(
					DefaultCacheManager.class.getClassLoader());
			return new DefaultCacheManager(globalConfig, config);
		} finally {
			Thread.currentThread().setContextClassLoader(oldTCCL);
		}
	}

	@Provides
	@Singleton
	Cache<Object, Object> provideCache(CacheContainer cacheContainer) {
		// workaround for classloading issues w/ factory methods
		// http://community.jboss.org/wiki/ModuleCompatibleClassloadingGuide
		ClassLoader oldTCCL = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(
					DefaultCacheManager.class.getClassLoader());
			return cacheContainer.getCache();
		} finally {
			Thread.currentThread().setContextClassLoader(oldTCCL);
		}
	}

	@Provides
	@Singleton
	TreeCache<Object, Object> provideTreeCache(Cache<Object, Object> cache,
			IDataChangeTracker changeTracker) {
		final TreeCache<Object, Object> entityCache = new TreeCacheFactory()
				.createTreeCache(cache);
		IDataChangeListener refreshListener = new IDataChangeListener() {
			@Override
			public void dataChanged(List<IDataChange> changes) {
				for (IDataChange change : changes) {
					if (change instanceof IStatementChange) {
						IStatementChange stmtChange = (IStatementChange) change;

						Fqn baseFqn = (stmtChange.getContext() != null) ? Fqn
								.fromElements(stmtChange.getContext()) : Fqn
								.root();

						// refresh existing subjects and objects
						Object subject = entityCache.get(
								Fqn.fromRelativeElements(baseFqn,
										stmtChange.getSubject()), "");
						if (subject instanceof IEntity) {
							((IEntity) subject).refresh();
						}
						Object object = entityCache.get(
								Fqn.fromRelativeElements(baseFqn,
										stmtChange.getObject()), "");
						if (object instanceof IEntity) {
							((IEntity) object).refresh();
						}

						// do only remove "properties" node from cache to ensure
						// that the above refresh logic is working
						entityCache
								.removeNode(Fqn.fromRelativeElements(baseFqn,
										stmtChange.getSubject(), "properties"));
						entityCache.removeNode(Fqn.fromRelativeElements(
								baseFqn, stmtChange.getObject(), "properties"));
					}
				}
			}
		};
		// ensure higher priority for this entity manager in the listener
		// list
		((DataChangeTracker) changeTracker)
				.addInternalChangeListener(refreshListener);
		return entityCache;
	}
}
