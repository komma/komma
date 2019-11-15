/*******************************************************************************
 * Copyright (c) 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.composition.cache.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import net.enilink.composition.ClassResolver;
import net.enilink.composition.CompositionModule;
import net.enilink.composition.DefaultObjectFactory;
import net.enilink.composition.ObjectFactory;
import net.enilink.composition.cache.IPropertyCache;
import net.enilink.composition.cache.behaviours.CacheBehaviourMethodProcessor;
import net.enilink.composition.mappers.RoleMapper;
import net.enilink.composition.mappers.TypeFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;

public abstract class CacheTestCase {
	ObjectFactory<String> objectFactory;
	ClassResolver<String> classResolver;
	private RoleMapper<String> roleMapper;

	@Before
	public void setUp() throws Exception {
		Injector injector = Guice.createInjector(createModule());
		objectFactory = injector.getInstance(new Key<ObjectFactory<String>>() {
		});
		classResolver = injector.getInstance(new Key<ClassResolver<String>>() {
		});
	}

	protected Module createModule() {
		return new CompositionModule<String>() {
			@Override
			protected void initRoleMapper(RoleMapper<String> roleMapper,
					TypeFactory<String> typeFactory) {
				CacheTestCase.this.roleMapper = roleMapper;

				super.initRoleMapper(roleMapper, typeFactory);

				CacheTestCase.this.initRoleMapper(roleMapper);
			}

			@Override
			protected void configure() {
				super.configure();

				bind(new Key<ObjectFactory<String>>() {
				}).to(new TypeLiteral<DefaultObjectFactory<String>>() {
				});
				bind(new TypeLiteral<ClassResolver<String>>() {
				});

				getBehaviourMethodProcessorBinder().addBinding()
						.to(CacheBehaviourMethodProcessor.class)
						.in(Singleton.class);
			}

			@Provides
			@Singleton
			protected TypeFactory<String> provideTypeFactory() {
				return new TypeFactory<String>() {
					@Override
					public String createType(String type) {
						return type;
					}

					@Override
					public String toString(String type) {
						return type;
					}
				};
			}

			@Provides
			@Singleton
			protected IPropertyCache provideCache() {
				final Map<Object, Object> map = new HashMap<Object, Object>();
				return new IPropertyCache() {
					@Override
					public Object put(Object entity, Object property,
							Object[] parameters, Object value) {
						List<Object> key = new ArrayList<Object>(
								2 + parameters.length);
						key.add(entity);
						key.add(property);
						for (Object parameter : parameters) {
							key.add(parameter);
						}
						map.put(key, value);

						return value;
					}

					@Override
					public Object get(Object entity, Object property,
							Object[] parameters) {
						List<Object> key = new ArrayList<Object>(
								2 + parameters.length);
						key.add(entity);
						key.add(property);
						for (Object parameter : parameters) {
							key.add(parameter);
						}
						return map.get(key);
					}
				};
			}
		};
	}

	protected void initRoleMapper(RoleMapper<String> roleMapper) {

	}

	protected RoleMapper<String> getRoleMapper() {
		return roleMapper;
	}
}
