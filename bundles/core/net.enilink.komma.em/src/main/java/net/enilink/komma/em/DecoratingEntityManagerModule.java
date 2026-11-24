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

import net.enilink.composition.cache.IPropertyCache;
import net.enilink.composition.properties.PropertySetFactory;
import net.enilink.composition.properties.komma.KommaPropertySetFactory;
import net.enilink.komma.core.IEntityManager;
import net.enilink.komma.dm.change.IDataChangeSupport;
import net.enilink.komma.em.internal.DecoratingEntityManager;
import net.enilink.komma.em.internal.PropertySetRefresher;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.name.Names;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

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
		bindPropertySetRefresher();

		bindNoopPropertyCache();
	}

	/**
	 * Ensures that the shared property sets of a {@link KommaPropertySetFactory}
	 * are refreshed if the underlying statements are changed by other means.
	 */
	protected void bindPropertySetRefresher() {
		bindListener(new AbstractMatcher<TypeLiteral<?>>() {
			@Override
			public boolean matches(TypeLiteral<?> type) {
				return KommaPropertySetFactory.class.isAssignableFrom(type.getRawType());
			}
		}, new TypeListener() {
			@Override
			public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
				Provider<IDataChangeSupport> changeSupport = encounter.getProvider(IDataChangeSupport.class);
				encounter.register((InjectionListener<I>) injectee -> PropertySetRefresher
						.register((KommaPropertySetFactory) injectee, changeSupport.get()));
			}
		});
	}

	protected Class<? extends IEntityManager> getManagerClass() {
		return DecoratingEntityManager.class;
	}

	protected Class<? extends PropertySetFactory> getPropertySetFactoryClass() {
		return KommaPropertySetFactory.class;
	}

	/**
	 * Installs a no-op property cache to support beans that expect an existing property cache.
	 */
	protected void bindNoopPropertyCache() {
		bind(IPropertyCache.class).toInstance(
			new IPropertyCache() {
				@Override
				public Object put(Object entity, Object property, Object[] parameters, Object value) {
					return value;
				}

				@Override
				public Object get(Object entity, Object property, Object[] parameters) {
					return null;
				}
			});
	}
}
