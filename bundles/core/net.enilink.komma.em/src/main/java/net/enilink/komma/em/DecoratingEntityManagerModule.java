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
import net.enilink.komma.core.IEntityManager;
import net.enilink.komma.em.internal.DecoratingEntityManager;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.name.Names;

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

	protected Class<? extends IEntityManager> getManagerClass() {
		return DecoratingEntityManager.class;
	}

	protected Class<? extends PropertySetFactory> getPropertySetFactoryClass() {
		return KommaPropertySetFactory.class;
	}
}
