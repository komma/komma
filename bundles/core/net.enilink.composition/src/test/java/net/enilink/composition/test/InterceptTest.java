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

import org.aopalliance.intercept.MethodInvocation;
import org.junit.Assert;
import org.junit.Test;
import net.enilink.composition.annotations.ParameterTypes;
import net.enilink.composition.mappers.RoleMapper;

public class InterceptTest extends CompositionTestCase {
	public static interface CConcept {
		void increment1();

		void increment2();
	}

	public static class CConceptBehaviour1 implements CConcept {
		public static int count;

		public void increment1() {
			count++;
		}

		public void increment2() {
			count++;
		}
	}

	public static class CConceptBehaviour2 {
		public static int count;

		@ParameterTypes({})
		public void increment1(MethodInvocation invocation) throws Throwable {
			count++;
			invocation.proceed();
		}

		@ParameterTypes({})
		public void increment2(MethodInvocation invocation) {
			count++;
		}
	}

	@Override
	protected void initRoleMapper(RoleMapper<String> roleMapper) {
		super.initRoleMapper(roleMapper);

		roleMapper.addConcept(CConcept.class, "urn:test:Concept");
		roleMapper.addBehaviour(CConceptBehaviour1.class, "urn:test:Concept");
		roleMapper.addBehaviour(CConceptBehaviour2.class, "urn:test:Concept");
	}

	@Test
	public void testInterceptBaseMethod() throws Exception {
		CConceptBehaviour1.count = 0;
		CConceptBehaviour2.count = 0;

		CConcept concept = (CConcept) objectFactory
				.createObject("urn:test:Concept");
		concept.increment1();
		Assert.assertEquals(1, CConceptBehaviour1.count);
		Assert.assertEquals(1, CConceptBehaviour2.count);
	}

	@Test
	public void testOverrideBaseMethod() throws Exception {
		CConceptBehaviour1.count = 0;
		CConceptBehaviour2.count = 0;

		CConcept concept = (CConcept) objectFactory
				.createObject("urn:test:Concept");
		concept.increment2();
		Assert.assertEquals(1, CConceptBehaviour2.count);
		Assert.assertEquals(0, CConceptBehaviour1.count);
	}
}
