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

import net.enilink.composition.properties.PropertySetFactory;
import net.enilink.composition.properties.komma.KommaPropertySetFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Names;

import net.enilink.komma.em.internal.DecoratingEntityManager;
import net.enilink.komma.em.util.UnitOfWork;
import net.enilink.komma.core.IEntityManager;

public class DecoratingEntityManagerModule extends AbstractModule {
	@Override
	protected void configure() {
		Class<? extends IEntityManager> managerClass = getManagerClass();
		bind(managerClass);
		bind(IEntityManager.class).annotatedWith(Names.named("unmanaged")).to(
				managerClass);

		Class<? extends PropertySetFactory> factoryClass = getPropertySetFactoryClass();
		bind(factoryClass).in(Singleton.class);
		bind(PropertySetFactory.class).to(factoryClass);
	}

	@Provides
	@Singleton
	protected IEntityManager provideEntityManager(final Injector injector,
			final UnitOfWork uow) {
		Binding<IEntityManager> binding = injector.getExistingBinding(Key.get(
				IEntityManager.class, Names.named("shared")));
		if (binding != null) {
			return binding.getProvider().get();
		} else {
			return injector.getInstance(getManagerClass());
		}
	}

	protected Class<? extends IEntityManager> getManagerClass() {
		return DecoratingEntityManager.class;
	}

	protected Class<? extends PropertySetFactory> getPropertySetFactoryClass() {
		return KommaPropertySetFactory.class;
	}
}
