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

public class MethodChainingTest extends CompositionTestCase {
	Object bean;

	@Iri("urn:test:Concept")
	public interface Concept {
		void chained();
	}

	public static abstract class Stub1 implements Concept {
		public static int count;

		public void chained() {
			count++;
		}
	}

	public static abstract class Stub2 implements Concept {
		public static int count;

		public void chained() {
			count++;
		}
	}

	public void setUp() throws Exception {
		super.setUp();

		Stub1.count = 0;
		Stub2.count = 0;

		bean = objectFactory.createObject("urn:test:Concept");
	}

	@Override
	protected void initRoleMapper(RoleMapper<String> roleMapper) {
		super.initRoleMapper(roleMapper);

		roleMapper.addConcept(Concept.class);
		roleMapper.addBehaviour(Stub1.class);
		roleMapper.addBehaviour(Stub2.class);
	}

	@Test
	public void testChaining() throws Exception {
		((Concept) bean).chained();
		Assert.assertEquals(1, Stub1.count);
		Assert.assertEquals(1, Stub2.count);

		((Concept) bean).chained();
		Assert.assertEquals(2, Stub1.count);
		Assert.assertEquals(2, Stub2.count);
	}
}
