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
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.dm.change.DataChangeSupport;
import net.enilink.komma.dm.change.IDataChange;
import net.enilink.komma.dm.change.IDataChangeListener;
import net.enilink.komma.dm.change.IDataChangeSupport;
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

	public static CacheBuilder<Object, Object> DEFAULT_BUILDER = CacheBuilder.newBuilder()
			.expireAfterAccess(2, TimeUnit.MINUTES).maximumSize(30000);

	protected final CacheBuilder<Object, Object> cacheBuilder;

	/**
	 * Constructs an instance of cache module with cache name.
	 * 
	 * This method deprecated and should no longer be used.
	 * 
	 * @param cacheName
	 */
	@Deprecated
	public CacheModule(String cacheName) {
		this(DEFAULT_BUILDER);
	}

	/**
	 * Constructs an instance of cache module with {@link #DEFAULT_BUILDER}.
	 */
	public CacheModule() {
		this(DEFAULT_BUILDER);
	}

	/**
	 * Constructs an instance of cache module with a specific cache builder.
	 */
	public CacheModule(CacheBuilder<Object, Object> cacheBuilder) {
		this.cacheBuilder = cacheBuilder;
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
	Cache<Object, CachedEntity> provideCache(IDataChangeSupport changeSupport) {
		final Cache<Object, CachedEntity> cache = cacheBuilder.build();

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
						IReference stmtSubject = stmt.getSubject();
						IReference stmtPredicate = stmt.getPredicate();
						Object stmtObject = stmt.getObject();

						// refresh existing subjects and objects
						boolean subjectRefreshed = stmtSubject != null ? refresh(stmtSubject) : false;
						if (stmtObject != null) {
							refresh(stmtObject);
						}
						// TODO refresh all possible objects if stmtObject is null

						// clear cache completely if owl:imports has
						// changed
						// TODO find a better approach instead of clearing the
						// whole cache
						if (stmt.getContext() != null && OWL.PROPERTY_IMPORTS.equals(stmtPredicate)) {
							cache.invalidateAll();
							continue;
						}

						// do only remove "properties" node from cache to ensure
						// that the above refresh logic keeps working
						CachedEntity cachedSubject = stmtSubject != null ? cache.getIfPresent(stmtSubject) : null;
						if (cachedSubject != null) {
							cachedSubject.clearProperties();
						}
						CachedEntity cachedObject =  stmtObject != null ? cache.getIfPresent(stmtObject) : null;
						if (cachedObject != null) {
							cachedObject.clearProperties();
						}

						// remove entity completely from cache if its type has
						// been changed
						if (subjectRefreshed && RDF.PROPERTY_TYPE.equals(stmtPredicate)) {
							cache.invalidate(stmtSubject);
						}
					}
				}
			}
		};
		// ensure higher priority for this entity manager in the listener
		// list
		((DataChangeSupport) changeSupport).addInternalChangeListener(refreshListener);
		return cache;
	}

	public static void stop() {
		// do nothing
		// This is required for more sophisticated caching engines like
		// Infinispan.
	}
}
