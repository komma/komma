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

import net.enilink.composition.annotations.Iri;
import net.enilink.composition.mappers.RoleMapper;

import org.junit.Assert;
import org.junit.Test;

public class BridgeMethodTest extends CompositionTestCase {
	Object concept;

	@Iri("urn:test:Concept")
	public interface Concept {
		Concept getThis();

		Concept getThat();
	}

	@Iri("urn:test:Concept")
	public interface Sub1 extends Concept {
		Sub1 getThis();

		Sub1 getThat();
	}

	@Iri("urn:test:Concept")
	public interface Sub2 extends Concept {
		Sub2 getThis();

		Sub2 getThat();
	}

	public static abstract class Stub1 implements Sub1 {
		public static int count;

		public Stub1 getThis() {
			count++;
			return this;
		}

		public Stub1 getThat() {
			count++;
			return null;
		}
	}

	public static abstract class Stub2 implements Sub2 {
		public static int count;

		public Stub2 getThis() {
			count++;
			return this;
		}

		public Stub2 getThat() {
			count++;
			return null;
		}
	}

	public void setUp() throws Exception {
		super.setUp();

		Stub1.count = 0;
		Stub2.count = 0;

		concept = objectFactory.createObject("urn:test:Concept");
	}

	@Override
	protected void initRoleMapper(RoleMapper<String> roleMapper) {
		super.initRoleMapper(roleMapper);

		roleMapper.addConcept(Concept.class);
		roleMapper.addConcept(Sub1.class);
		roleMapper.addConcept(Sub2.class);
		roleMapper.addBehaviour(Stub1.class);
		roleMapper.addBehaviour(Stub2.class);
	}

	@Test
	public void testBridgeSub1() throws Exception {
		Assert.assertNull(((Concept) concept).getThat());
		Assert.assertEquals(1, Stub1.count);
		Assert.assertEquals(1, Stub2.count);
	}

	@Test
	public void testBridgeSub2() throws Exception {
		Assert.assertNull(((Concept) concept).getThat());
		Assert.assertEquals(1, Stub1.count);
		Assert.assertEquals(1, Stub2.count);
	}

}
