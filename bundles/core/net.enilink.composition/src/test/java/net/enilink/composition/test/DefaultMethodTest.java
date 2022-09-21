/*******************************************************************************
 * Copyright (c) 2022 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.composition.test;

import net.enilink.composition.annotations.Iri;
import net.enilink.composition.annotations.ParameterTypes;
import net.enilink.composition.mappers.RoleMapper;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for composition with default interface methods.
 */
public class DefaultMethodTest extends CompositionTestCase {
	@Iri("urn:test:Value")
	public interface Value {
	}

	@Iri("urn:test:ConcreteType")
	public interface Concept {
		Value getValue();

		void setValue(Value value);

		default boolean hasValue() {
			return getValue() != null;
		}

		default int testAbstractMethod() {
			return 1;
		}

		default int testIntercept() {
			return 0;
		}

		default String testOnlyDefaultNoImpl() {
			return "a";
		}
	}

	public static abstract class ConceptBehaviour implements Concept {
		Value value;

		@Override
		public Value getValue() {
			return value;
		}

		@Override
		public void setValue(Value value) {
			this.value = value;
		}
	}

	public static abstract class ConceptBehaviour2 implements Concept {
		@Override
		public boolean hasValue() {
			return Concept.super.hasValue();
		}

		@Override
		public int testIntercept() {
			return 1;
		}
	}

	public static abstract class ConceptBehaviour3 implements Concept {
		@Override
		public abstract int testAbstractMethod();
	}

	public static abstract class ConceptBehaviour4 implements Concept {
		@Override
		public abstract int testAbstractMethod();

		@ParameterTypes({})
		public int testIntercept(MethodInvocation invocation) {
			return 3;
		}
	}

	@Override
	protected void initRoleMapper(RoleMapper<String> roleMapper) {
		super.initRoleMapper(roleMapper);

		roleMapper.addConcept(Value.class);
		roleMapper.addConcept(Concept.class);
		roleMapper.addBehaviour(ConceptBehaviour.class);
		roleMapper.addBehaviour(ConceptBehaviour2.class);
		roleMapper.addBehaviour(ConceptBehaviour3.class);
		roleMapper.addBehaviour(ConceptBehaviour4.class);
	}

	@Test
	public void testCreateAndSet() throws Exception {
		Concept object = objectFactory.createObject(Concept.class);
		Assert.assertEquals(object.getValue(), null);
		Assert.assertFalse(object.hasValue());
		Value value = objectFactory.createObject(Value.class);
		object.setValue(value);
		Assert.assertEquals(object.getValue(), value);
		Assert.assertTrue(object.hasValue());
		Assert.assertEquals(1, object.testAbstractMethod());
		Assert.assertEquals(3, object.testIntercept());
		Assert.assertEquals("a", object.testOnlyDefaultNoImpl());
	}
}
