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
package net.enilink.composition.test;

import org.junit.Before;
import net.enilink.composition.ClassResolver;
import net.enilink.composition.CompositionModule;
import net.enilink.composition.DefaultObjectFactory;
import net.enilink.composition.ObjectFactory;
import net.enilink.composition.mappers.RoleMapper;
import net.enilink.composition.mappers.TypeFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;

public abstract class CompositionTestCase {
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
			protected void configure() {
				super.configure();

				bind(new Key<ObjectFactory<String>>() {
				}).to(new TypeLiteral<DefaultObjectFactory<String>>() {
				});
				bind(new TypeLiteral<ClassResolver<String>>() {
				});
			}

			@Override
			protected void initRoleMapper(RoleMapper<String> roleMapper,
					TypeFactory<String> typeFactory) {
				CompositionTestCase.this.roleMapper = roleMapper;

				super.initRoleMapper(roleMapper, typeFactory);

				CompositionTestCase.this.initRoleMapper(roleMapper);
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
		};
	}

	protected void initRoleMapper(RoleMapper<String> roleMapper) {

	}

	protected RoleMapper<String> getRoleMapper() {
		return roleMapper;
	}
}
