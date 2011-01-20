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

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Names;

import net.enilink.komma.em.internal.DecoratingEntityManager;
import net.enilink.komma.core.IEntityManager;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.util.UnitOfWork;

public class DecoratingEntityManagerModule extends AbstractModule {
	@Override
	protected void configure() {
		bind(boolean.class).annotatedWith(Names.named("injectManager"))
				.toInstance(isInjectManager());
		bind(IEntityManager.class).annotatedWith(Names.named("unmanaged")).to(
				getManagerClass());
	}

	@Provides
	@Singleton
	protected IEntityManager provideEntityManager(final Injector injector,
			final UnitOfWork uow) {
		ThreadLocalEntityManager manager = new ThreadLocalEntityManager();
		manager.setEntityManagerProvider(new Provider<IEntityManager>() {
			@Override
			public IEntityManager get() {
				if (!uow.isActive()) {
					throw new KommaException("No active unit of work found.");
				}
				IEntityManager manager = injector
						.getInstance(getManagerClass());
				uow.addManager(manager);
				return manager;
			}
		});
		return manager;
	}

	protected Class<? extends IEntityManager> getManagerClass() {
		return DecoratingEntityManager.class;
	}

	protected boolean isInjectManager() {
		return true;
	}
}
