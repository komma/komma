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
package net.enilink.composition.properties.test;

import net.enilink.composition.mapping.IPropertyMapper;
import net.enilink.composition.mapping.PropertyAttribute;
import net.enilink.composition.properties.PropertySet;
import org.junit.Before;
import net.enilink.composition.ClassResolver;
import net.enilink.composition.CompositionModule;
import net.enilink.composition.DefaultObjectFactory;
import net.enilink.composition.ObjectFactory;
import net.enilink.composition.mappers.RoleMapper;
import net.enilink.composition.mappers.TypeFactory;
import net.enilink.composition.properties.mapper.IriAnnotationPropertyMapper;
import net.enilink.composition.properties.PropertySetFactory;
import net.enilink.composition.properties.behaviours.PropertyMapperProcessor;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;

public abstract class PropertiesCompositionTestCase implements PropertySetFactory {
	ObjectFactory<String> objectFactory;
	ClassResolver<String> classResolver;
	private RoleMapper<String> roleMapper;

	@Before
	public void setUp() throws Exception {
		Injector injector = Guice.createInjector(createModule());
		objectFactory = injector.getInstance(new Key<>() {
		});
		classResolver = injector.getInstance(new Key<>() {
		});
	}

	protected Module createModule() {
		return new CompositionModule<String>() {
			@Override
			protected void initRoleMapper(RoleMapper<String> roleMapper,
					TypeFactory<String> typeFactory) {
				PropertiesCompositionTestCase.this.roleMapper = roleMapper;

				super.initRoleMapper(roleMapper, typeFactory);

				PropertiesCompositionTestCase.this.initRoleMapper(roleMapper);
			}

			@Override
			protected void configure() {
				super.configure();

				bind(new Key<ObjectFactory<String>>() {
				}).to(new TypeLiteral<DefaultObjectFactory<String>>() {
				});
				bind(new TypeLiteral<ClassResolver<String>>() {
				});
				bind(PropertySetFactory.class).toInstance(PropertiesCompositionTestCase.this);

				bind(PropertyMapperProcessor.class).in(Singleton.class);
				getBehaviourClassProcessorBinder().addBinding().to(
						PropertyMapperProcessor.class);
			}

			@Provides
			@Singleton
			protected TypeFactory<String> provideTypeFactory() {
				return new TypeFactory<>() {
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
			protected @Singleton
			IPropertyMapper providePropertyMapper() {
				return createPropertyMapper();
			}
		};
	}

	protected void initRoleMapper(RoleMapper<String> roleMapper) {

	}

	protected IPropertyMapper createPropertyMapper() {
		return new IriAnnotationPropertyMapper();
	}

	protected RoleMapper<String> getRoleMapper() {
		return roleMapper;
	}

	@Override
	public <E> PropertySet<E> createPropertySet(Object bean, String uri,
	                                            Class<E> elementType, PropertyAttribute... attributes) {
		boolean localized = false;
		for (PropertyAttribute attribute : attributes) {
			if (PropertyAttribute.LOCALIZED.equals(attribute.getName())) {
				localized = true;
			}
		}
		return new TestPropertySet<E>();
	}
}
