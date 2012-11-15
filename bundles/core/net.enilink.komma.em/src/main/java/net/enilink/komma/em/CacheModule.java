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
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.manager.CacheContainer;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachemanagerlistener.annotation.CacheStarted;
import org.infinispan.notifications.cachemanagerlistener.annotation.CacheStopped;
import org.infinispan.notifications.cachemanagerlistener.event.Event;
import org.infinispan.tree.Fqn;
import org.infinispan.tree.TreeCache;
import org.infinispan.tree.TreeCacheFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;

import net.enilink.vocab.owl.OWL;
import net.enilink.vocab.rdf.RDF;
import net.enilink.komma.dm.change.DataChangeTracker;
import net.enilink.komma.dm.change.IDataChange;
import net.enilink.komma.dm.change.IDataChangeListener;
import net.enilink.komma.dm.change.IDataChangeTracker;
import net.enilink.komma.dm.change.IStatementChange;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.util.IClosable;

public class CacheModule extends AbstractModule {
	/**
	 * Listener that closes the cache container after the last cache has been
	 * stopped.
	 */
	@Listener
	public static class StartStopListener {
		int runningCaches = 0;

		@CacheStarted
		@CacheStopped
		public void doSomething(Event event) {
			if (event.getType() == Event.Type.CACHE_STARTED) {
				runningCaches++;
			} else if (event.getType() == Event.Type.CACHE_STOPPED) {
				runningCaches--;
			}
			if (runningCaches <= 0) {
				synchronized (CacheModule.class) {
					if (cacheContainer != null) {
						cacheContainer.stop();
						cacheContainer = null;
					}
				}
			}
		}
	}

	static class CacheClosable implements IClosable {
		@Inject
		Cache<Object, Object> cache;

		@Override
		public void close() {
			if (cache != null) {
				cache.stop();
				cache = null;
			}
		}
	}

	private String cacheName;
	static CacheContainer cacheContainer;

	public CacheModule(String cacheName) {
		this.cacheName = cacheName;
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
	CacheContainer provideCacheContainer() {
		synchronized (CacheModule.class) {
			if (cacheContainer == null) {
				Configuration configuration = new ConfigurationBuilder()
						.invocationBatching().enable() // required for TreeCache
						.eviction().strategy(EvictionStrategy.LIRS) // LIRS
																	// instead
																	// of LRU?
						.maxEntries(30000) // a reasonable limit?
						.jmxStatistics().enable() // expose statistics via JMX
						.build();
				// workaround for classloading issues w/ factory methods
				// http://community.jboss.org/wiki/ModuleCompatibleClassloadingGuide
				ClassLoader oldTCCL = Thread.currentThread()
						.getContextClassLoader();
				try {
					Thread.currentThread().setContextClassLoader(
							DefaultCacheManager.class.getClassLoader());
					EmbeddedCacheManager cacheManager = new DefaultCacheManager(
							configuration);
					cacheManager.addListener(new StartStopListener());
					cacheContainer = cacheManager;
				} finally {
					Thread.currentThread().setContextClassLoader(oldTCCL);
				}
			}
			return cacheContainer;
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
			return cacheContainer.getCache(cacheName);
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

						Fqn baseFqn = stmtChange.getContext() != null ? Fqn
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

						// clear model cache completely if owl:imports has
						// changed
						if (stmtChange.getContext() != null
								&& OWL.PROPERTY_IMPORTS.equals(stmtChange
										.getPredicate())) {
							entityCache.removeNode(baseFqn);
							continue;
						}

						// do only remove "properties" node from cache to ensure
						// that the above refresh logic keeps working
						entityCache
								.removeNode(Fqn.fromRelativeElements(baseFqn,
										stmtChange.getSubject(), "properties"));
						entityCache.removeNode(Fqn.fromRelativeElements(
								baseFqn, stmtChange.getObject(), "properties"));

						// remove entity completely from cache if its type has
						// been changed
						if (subject != null
								&& RDF.PROPERTY_TYPE.equals(stmtChange
										.getPredicate())) {
							entityCache.removeNode(Fqn.fromRelativeElements(
									baseFqn, stmtChange.getSubject()));
						}
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
