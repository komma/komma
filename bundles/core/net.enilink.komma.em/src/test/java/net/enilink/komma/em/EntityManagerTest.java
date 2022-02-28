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
import java.util.List;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.After;
import org.junit.Before;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import net.enilink.komma.core.IEntityManager;
import net.enilink.komma.core.IEntityManagerFactory;
import net.enilink.komma.core.IUnitOfWork;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.core.KommaModule;
import net.enilink.komma.em.CacheModule;
import net.enilink.komma.em.CachingEntityManagerModule;
import net.enilink.komma.em.DecoratingEntityManagerModule;
import net.enilink.komma.em.EntityManagerFactoryModule;
import net.enilink.komma.em.util.UnitOfWork;
import net.enilink.komma.rdf4j.RDF4JModule;

public abstract class EntityManagerTest {
	protected Injector injector;
	protected IEntityManagerFactory factory;
	protected IEntityManager manager;
	protected UnitOfWork uow;

	protected Module createStorageModule() {
		// create an RDF4J memory store
		return new AbstractModule() {
			@Override
			protected void configure() {
				install(new RDF4JModule());
			}

			@Singleton
			@Provides
			Repository provideRepository() {
				Repository repository = new SailRepository(new MemoryStore());
				try {
					repository.init();
				} catch (RepositoryException e) {
					throw new KommaException(e);
				}

				return repository;
			}
		};
	}

	@Before
	public void beforeTest() throws Exception {
		List<Module> modules = new ArrayList<>();
		modules.add(createStorageModule());
		modules.add(new EntityManagerFactoryModule(createModule(), null,
				enableCaching() ? new CachingEntityManagerModule() : new DecoratingEntityManagerModule()));
		if (enableCaching()) {
			modules.add(new CacheModule());
		}
		modules.add(new AbstractModule() {
			@Override
			protected void configure() {
				UnitOfWork uow = new UnitOfWork();
				uow.begin();

				bind(UnitOfWork.class).toInstance(uow);
				bind(IUnitOfWork.class).toInstance(uow);
			}
		});
		injector = Guice.createInjector(modules);
		factory = injector.getInstance(IEntityManagerFactory.class);
		manager = factory.get();
	}

	protected KommaModule createModule() throws Exception {
		return new KommaModule(getClass().getClassLoader());
	}

	protected boolean enableCaching() {
		return false;
	}

	@After
	public void afterTest() throws Exception {
		try {
			factory.getUnitOfWork().end();
			factory.close();
		} catch (Exception e) {
		}
	}
}