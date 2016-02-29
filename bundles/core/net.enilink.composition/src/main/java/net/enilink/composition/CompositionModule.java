/*******************************************************************************
 * Copyright (c) 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.composition;

import net.enilink.composition.asm.BehaviourClassProcessor;
import net.enilink.composition.asm.BehaviourMethodProcessor;
import net.enilink.composition.asm.DefaultBehaviourFactory;
import net.enilink.composition.asm.processors.BehaviourConstructorGenerator;
import net.enilink.composition.asm.processors.BehaviourInterfaceImplementor;
import net.enilink.composition.asm.processors.MethodDelegationGenerator;
import net.enilink.composition.mappers.DefaultRoleMapper;
import net.enilink.composition.mappers.RoleMapper;
import net.enilink.composition.mappers.TypeFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;

public class CompositionModule<T> extends AbstractModule {
	private Multibinder<BehaviourFactory> behaviourFactoryBinder;
	private Multibinder<BehaviourClassProcessor> classProcessorBinder;
	private Multibinder<BehaviourMethodProcessor> methodProcessorBinder;

	protected void bindClassDefiner() {
		bind(ClassDefiner.class).in(Singleton.class);
	}

	@Override
	protected void configure() {
		initBindings();
	}

	protected Multibinder<BehaviourClassProcessor> getBehaviourClassProcessorBinder() {
		if (classProcessorBinder == null) {
			classProcessorBinder = Multibinder.newSetBinder(binder(),
					BehaviourClassProcessor.class);
		}
		return classProcessorBinder;
	}

	protected Multibinder<BehaviourFactory> getBehaviourFactoryBinder() {
		if (behaviourFactoryBinder == null) {
			behaviourFactoryBinder = Multibinder.newSetBinder(binder(),
					BehaviourFactory.class);
		}
		return behaviourFactoryBinder;
	}

	protected Multibinder<BehaviourMethodProcessor> getBehaviourMethodProcessorBinder() {
		if (methodProcessorBinder == null) {
			methodProcessorBinder = Multibinder.newSetBinder(binder(),
					BehaviourMethodProcessor.class);
		}
		return methodProcessorBinder;
	}

	protected void initBindings() {
		getBehaviourClassProcessorBinder().addBinding().to(
				BehaviourInterfaceImplementor.class);
		getBehaviourClassProcessorBinder().addBinding().to(
				BehaviourConstructorGenerator.class);

		getBehaviourMethodProcessorBinder().addBinding().to(
				MethodDelegationGenerator.class);

		getBehaviourFactoryBinder().addBinding().to(
				DefaultBehaviourFactory.class);

		bindClassDefiner();
	}
	
	protected RoleMapper<T> createRoleMapper(TypeFactory<T> typeFactory) {
		return new DefaultRoleMapper<T>(typeFactory);
	}

	protected void initRoleMapper(RoleMapper<T> roleMapper,
			TypeFactory<T> typeFactory) {
	}

	@Provides
	@Singleton
	protected RoleMapper<T> provideRoleMapper(TypeFactory<T> typeFactory) {
		RoleMapper<T> roleMapper = createRoleMapper(typeFactory);
		initRoleMapper(roleMapper, typeFactory);
		return roleMapper;
	}
}
