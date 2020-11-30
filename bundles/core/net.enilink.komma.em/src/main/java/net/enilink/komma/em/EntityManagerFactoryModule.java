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

import java.util.Locale;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;

import net.enilink.komma.dm.IDataManager;
import net.enilink.komma.dm.change.DataChangeSupport;
import net.enilink.komma.dm.change.IDataChangeSupport;
import net.enilink.komma.core.IEntityDecorator;
import net.enilink.komma.core.IEntityManagerFactory;
import net.enilink.komma.core.IProvider;
import net.enilink.komma.core.KommaModule;

public class EntityManagerFactoryModule extends AbstractModule {
	IProvider<Locale> locale;

	Module managerModule;

	KommaModule module;

	public EntityManagerFactoryModule(KommaModule module,
			IProvider<Locale> locale) {
		this(module, locale, null);
	}

	public EntityManagerFactoryModule(KommaModule module,
			IProvider<Locale> locale, Module managerModule) {
		this.module = module;
		this.locale = locale;
		this.managerModule = managerModule;
	}

	@Override
	protected void configure() {
		Multibinder.newSetBinder(binder(), IEntityDecorator.class);
		bind(DataChangeSupport.class).in(Singleton.class);
		bind(IDataChangeSupport.class).to(DataChangeSupport.class);

		// provide a global shared data manager that is implemented via a thread-local
		// if this data manager is used then a unit of work is required
		bind(Key.get(IDataManager.class, Names.named("thread-local"))).to(ThreadLocalDataManager.class)
				.in(Singleton.class);

		install(new EntityVarModule());
	}

	@Singleton
	@Provides
	IEntityManagerFactory provideFactory(Injector injector) {
		IEntityManagerFactory factory = new EntityManagerFactory(module,
				locale, managerModule);
		injector.injectMembers(factory);
		return factory;
	}
}
