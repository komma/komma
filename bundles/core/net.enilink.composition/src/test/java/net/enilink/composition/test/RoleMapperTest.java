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
package net.enilink.composition.test;

import java.util.Collection;
import java.util.HashSet;

import org.junit.Assert;
import net.enilink.composition.annotations.Iri;

public class RoleMapperTest extends CompositionTestCase {
	@Iri("urn:test:Display")
	public interface Display {
	}

	@Iri("urn:test:SubDisplay")
	public interface SubDisplay extends Display {
	}

	public static class DisplaySupport {
	}

	@org.junit.Test
	public void testSubclasses1() throws Exception {
		getRoleMapper().addConcept(Display.class);
		getRoleMapper().addConcept(SubDisplay.class);
		getRoleMapper().addBehaviour(DisplaySupport.class, "urn:test:Display");

		Assert.assertTrue(findRoles("urn:test:Display").contains(Display.class));
		Assert.assertTrue(findRoles("urn:test:Display").contains(
				DisplaySupport.class));
		Assert.assertTrue(findRoles("urn:test:SubDisplay").contains(
				Display.class));
		Assert.assertTrue(findRoles("urn:test:SubDisplay").contains(
				SubDisplay.class));
		Assert.assertTrue(findRoles("urn:test:SubDisplay").contains(
				DisplaySupport.class));
	}

	@org.junit.Test
	public void testSubclasses2() throws Exception {
		getRoleMapper().addBehaviour(DisplaySupport.class, "urn:test:Display");
		getRoleMapper().addConcept(Display.class);
		getRoleMapper().addConcept(SubDisplay.class);

		Assert.assertTrue(findRoles("urn:test:Display").contains(Display.class));
		Assert.assertTrue(findRoles("urn:test:Display").contains(
				DisplaySupport.class));
		Assert.assertTrue(findRoles("urn:test:SubDisplay").contains(
				Display.class));
		Assert.assertTrue(findRoles("urn:test:SubDisplay").contains(
				SubDisplay.class));
		Assert.assertTrue(findRoles("urn:test:SubDisplay").contains(
				DisplaySupport.class));
	}

	@org.junit.Test
	public void testSubclasses3() throws Exception {
		getRoleMapper().addConcept(Display.class);
		getRoleMapper().addBehaviour(DisplaySupport.class, "urn:test:Display");
		getRoleMapper().addConcept(SubDisplay.class);

		Assert.assertTrue(findRoles("urn:test:Display").contains(Display.class));
		Assert.assertTrue(findRoles("urn:test:Display").contains(
				DisplaySupport.class));
		Assert.assertTrue(findRoles("urn:test:SubDisplay").contains(
				Display.class));
		Assert.assertTrue(findRoles("urn:test:SubDisplay").contains(
				SubDisplay.class));
		Assert.assertTrue(findRoles("urn:test:SubDisplay").contains(
				DisplaySupport.class));
	}

	@org.junit.Test
	public void testSubclasses4() throws Exception {
		getRoleMapper().addConcept(SubDisplay.class);
		getRoleMapper().addConcept(Display.class);
		getRoleMapper().addBehaviour(DisplaySupport.class, "urn:test:Display");

		Assert.assertTrue(findRoles("urn:test:Display").contains(Display.class));
		Assert.assertTrue(findRoles("urn:test:Display").contains(
				DisplaySupport.class));
		Assert.assertTrue(findRoles("urn:test:SubDisplay").contains(
				Display.class));
		Assert.assertTrue(findRoles("urn:test:SubDisplay").contains(
				SubDisplay.class));
		Assert.assertTrue(findRoles("urn:test:SubDisplay").contains(
				DisplaySupport.class));
	}

	@org.junit.Test
	public void testSubclasses5() throws Exception {
		getRoleMapper().addBehaviour(DisplaySupport.class, "urn:test:Display");
		getRoleMapper().addConcept(SubDisplay.class);
		getRoleMapper().addConcept(Display.class);

		Assert.assertTrue(findRoles("urn:test:Display").contains(Display.class));
		Assert.assertTrue(findRoles("urn:test:Display").contains(
				DisplaySupport.class));
		Assert.assertTrue(findRoles("urn:test:SubDisplay").contains(
				Display.class));
		Assert.assertTrue(findRoles("urn:test:SubDisplay").contains(
				SubDisplay.class));
		Assert.assertTrue(findRoles("urn:test:SubDisplay").contains(
				DisplaySupport.class));
	}

	@org.junit.Test
	public void testSubclasses6() throws Exception {
		getRoleMapper().addConcept(SubDisplay.class);
		getRoleMapper().addBehaviour(DisplaySupport.class, "urn:test:Display");
		getRoleMapper().addConcept(Display.class);

		Assert.assertTrue(findRoles("urn:test:Display").contains(Display.class));
		Assert.assertTrue(findRoles("urn:test:Display").contains(
				DisplaySupport.class));
		Assert.assertTrue(findRoles("urn:test:SubDisplay").contains(
				Display.class));
		Assert.assertTrue(findRoles("urn:test:SubDisplay").contains(
				SubDisplay.class));
		Assert.assertTrue(findRoles("urn:test:SubDisplay").contains(
				DisplaySupport.class));
	}

	private Collection<Class<?>> findRoles(String uri) {
		return getRoleMapper().findRoles(uri, new HashSet<Class<?>>());
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();
	}
}
