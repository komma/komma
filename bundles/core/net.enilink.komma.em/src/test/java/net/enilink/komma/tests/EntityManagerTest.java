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

import org.junit.After;
import org.junit.Before;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Module;

import net.enilink.komma.em.DecoratingEntityManagerModule;
import net.enilink.komma.em.EntityManagerFactoryModule;
import net.enilink.komma.core.IEntityManager;
import net.enilink.komma.core.IEntityManagerFactory;
import net.enilink.komma.core.IUnitOfWork;
import net.enilink.komma.core.KommaModule;
import net.enilink.komma.util.UnitOfWork;

public abstract class EntityManagerTest {
	protected IEntityManagerFactory factory;
	protected IEntityManager manager;
	protected UnitOfWork uow;

	// defines the default module for configuring the storage backend
	private static final String DEFAULT_STORAGE_MODULE = "net.enilink.komma.sesame.SesameTestModule";

	private Module createStorageModule() {
		try {
			return (Module) getClass().getClassLoader()
					.loadClass(DEFAULT_STORAGE_MODULE).newInstance();
		} catch (Exception e) {
			throw new RuntimeException("Unable to instantiate storage module: "
					+ DEFAULT_STORAGE_MODULE, e);
		}
	}

	@Before
	public void beforeTest() throws Exception {
		factory = Guice.createInjector(
				createStorageModule(),
				new EntityManagerFactoryModule(createModule(), null,
						new DecoratingEntityManagerModule()),
				new AbstractModule() {
					@Override
					protected void configure() {
						UnitOfWork uow = new UnitOfWork();
						uow.begin();

						bind(UnitOfWork.class).toInstance(uow);
						bind(IUnitOfWork.class).toInstance(uow);
					}
				}).getInstance(IEntityManagerFactory.class);

		manager = factory.get();
	}

	protected KommaModule createModule() throws Exception {
		return new KommaModule(getClass().getClassLoader());
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