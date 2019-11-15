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

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;

import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.dm.change.DataChangeTracker;
import net.enilink.komma.dm.change.IDataChange;
import net.enilink.komma.dm.change.IDataChangeListener;
import net.enilink.komma.dm.change.IDataChangeTracker;
import net.enilink.komma.dm.change.IStatementChange;
import net.enilink.komma.em.internal.CachedEntity;
import net.enilink.komma.em.util.IClosable;
import net.enilink.vocab.owl.OWL;
import net.enilink.vocab.rdf.RDF;

public class CacheModule extends AbstractModule {
	static class CacheClosable implements IClosable {
		@Inject
		Cache<Object, CachedEntity> cache;

		@Override
		public void close() {
			if (cache != null) {
				cache.invalidateAll();
				cache = null;
			}
		}
	}

	private String cacheName;

	public static CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder()
			.expireAfterAccess(2, TimeUnit.MINUTES).maximumSize(30000);

	public CacheModule(String cacheName) {
		this.cacheName = cacheName;
	}

	@Override
	protected void configure() {
		Multibinder<IClosable> closableBinder = Multibinder.<IClosable> newSetBinder(binder(),
				new TypeLiteral<IClosable>() {
				});
		closableBinder.addBinding().to(CacheClosable.class);
	}

	@Provides
	@Singleton
	Cache<Object, CachedEntity> provideCache(IDataChangeTracker changeTracker) {
		final Cache<Object, CachedEntity> cache = builder.build();

		IDataChangeListener refreshListener = new IDataChangeListener() {
			boolean refresh(Object entity) {
				CachedEntity cached = cache.getIfPresent(entity);
				boolean refreshed = false;
				if (cached != null) {
					// iterate over all contexts and refresh each entity
					for (Object contextKey : cached.contexts()) {
						Object entityInCtx = cached.getSelf(contextKey);
						if (entityInCtx instanceof IEntity) {
							((IEntity) entityInCtx).refresh();
							refreshed = true;
						}
					}
				}
				return refreshed;
			}

			@Override
			public void dataChanged(List<IDataChange> changes) {
				for (IDataChange change : changes) {
					if (change instanceof IStatementChange) {
						IStatement stmt = ((IStatementChange) change).getStatement();

						// refresh existing subjects and objects
						boolean subjectRefreshed = refresh(stmt.getSubject());
						refresh(stmt.getObject());

						// clear cache completely if owl:imports has
						// changed
						// TODO find a better approach instead of clearing the
						// whole cache
						if (stmt.getContext() != null && OWL.PROPERTY_IMPORTS.equals(stmt.getPredicate())) {
							cache.invalidateAll();
							continue;
						}

						// do only remove "properties" node from cache to ensure
						// that the above refresh logic keeps working
						CachedEntity cachedSubject = cache.getIfPresent(stmt.getSubject());
						if (cachedSubject != null) {
							cachedSubject.clearProperties();
						}
						CachedEntity cachedObject = cache.getIfPresent(stmt.getObject());
						if (cachedObject != null) {
							cachedObject.clearProperties();
						}

						// remove entity completely from cache if its type has
						// been changed
						if (subjectRefreshed && RDF.PROPERTY_TYPE.equals(stmt.getPredicate())) {
							cache.invalidate(stmt.getSubject());
						}
					}
				}
			}
		};
		// ensure higher priority for this entity manager in the listener
		// list
		((DataChangeTracker) changeTracker).addInternalChangeListener(refreshListener);
		return cache;
	}

	public static void stop() {
		// do nothing
		// This is required for more sophisticated caching engines like
		// Infinispan.
	}
}
