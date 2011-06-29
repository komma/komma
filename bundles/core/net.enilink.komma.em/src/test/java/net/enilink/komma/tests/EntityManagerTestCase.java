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
package net.enilink.komma.tests;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.openrdf.repository.Repository;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;
import org.openrdf.store.StoreException;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import net.enilink.komma.em.DecoratingEntityManagerModule;
import net.enilink.komma.em.EntityManagerFactoryModule;
import net.enilink.komma.core.IEntityManager;
import net.enilink.komma.core.IEntityManagerFactory;
import net.enilink.komma.core.IUnitOfWork;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.core.KommaModule;
import net.enilink.komma.sesame.SesameModule;
import net.enilink.komma.util.UnitOfWork;

public abstract class EntityManagerTestCase extends KommaTestCase {
	protected IEntityManagerFactory factory;
	protected IEntityManager manager;
	protected UnitOfWork uow;

	@Override
	protected void setUp() throws Exception {
		uow = new UnitOfWork();
		uow.begin();
		factory = Guice.createInjector(
				new SesameModule(),
				new EntityManagerFactoryModule(createModule(), null,
						new DecoratingEntityManagerModule()),
				new AbstractModule() {
					@Override
					protected void configure() {
						bind(IUnitOfWork.class).toInstance(uow);
						bind(UnitOfWork.class).toInstance(uow);
					}

					@SuppressWarnings("unused")
					@Singleton
					@Provides
					Repository provideRepository() {
						Repository repository = new SailRepository(
								new MemoryStore());
						try {
							repository.initialize();
						} catch (StoreException e) {
							throw new KommaException(e);
						}

						return repository;
					}
				}).getInstance(IEntityManagerFactory.class);

		manager = factory.get();
	}

	protected KommaModule createModule() throws Exception {
		return new KommaModule(getClass().getClassLoader());
	}

	@Override
	protected void tearDown() throws Exception {
		try {
			uow.end();
			factory.close();
		} catch (Exception e) {
		}
	}

	public static Test suite() throws Exception {
		return new TestSuite();
	}
}