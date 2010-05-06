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

import org.junit.Assert;
import org.junit.Test;
import net.enilink.composition.annotations.matches;
import net.enilink.composition.annotations.Iri;
import net.enilink.composition.mappers.RoleMapper;

public class MatchesTest extends CompositionTestCase {
	@matches("urn:test:*")
	public interface TestResource {
	}

	@matches("urn:test:something")
	public interface TestSomething {
	}

	@matches("*")
	public interface Anything {
	}

	@matches("/*")
	public interface AnyPath {
	}

	@matches("/path")
	public interface Path {
	}

	@matches("/path/*")
	public interface AnySubPath {
	}

	@Iri("urn:test:Something")
	public interface Something {
	}

	public void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void initRoleMapper(RoleMapper<String> roleMapper) {
		super.initRoleMapper(roleMapper);

		roleMapper.addConcept(TestResource.class);
		roleMapper.addConcept(TestSomething.class);
		roleMapper.addConcept(Anything.class);
		roleMapper.addConcept(AnyPath.class);
		roleMapper.addConcept(Path.class);
		roleMapper.addConcept(AnySubPath.class);
		roleMapper.addConcept(Something.class);
	}

	@Test
	public void testOthers() throws Exception {
		Object o = objectFactory.createObject("urn:nothing");
		Assert.assertFalse(o instanceof TestResource);
		Assert.assertFalse(o instanceof TestSomething);
		Assert.assertTrue(o instanceof Anything);
		Assert.assertFalse(o instanceof AnyPath);
		Assert.assertFalse(o instanceof Path);
		Assert.assertFalse(o instanceof AnySubPath);
	}

	@Test
	public void testMatch() throws Exception {
		Object o = objectFactory.createObject("urn:test:something");
		Assert.assertTrue(o instanceof TestResource);
		Assert.assertTrue(o instanceof TestSomething);
		Assert.assertTrue(o instanceof Anything);
		Assert.assertFalse(o instanceof AnyPath);
		Assert.assertFalse(o instanceof Path);
		Assert.assertFalse(o instanceof AnySubPath);
	}

	// public void testMatchWithTypes() throws Exception {
	// Object o = con.getObject("urn:test:something");
	// o = con.addDesignation(o, Something.class);
	// Assert.assertTrue(o instanceof TestResource);
	// Assert.assertTrue(o instanceof TestSomething);
	// Assert.assertTrue(o instanceof Anything);
	// Assert.assertFalse(o instanceof AnyPath);
	// Assert.assertFalse(o instanceof Path);
	// Assert.assertFalse(o instanceof AnySubPath);
	// }

	@Test
	public void testMatchPath() throws Exception {
		Object o = objectFactory.createObject("file:///path");
		Assert.assertFalse(o instanceof TestResource);
		Assert.assertFalse(o instanceof TestSomething);
		Assert.assertTrue(o instanceof Anything);
		Assert.assertTrue(o instanceof AnyPath);
		Assert.assertTrue(o instanceof Path);
		Assert.assertFalse(o instanceof AnySubPath);
	}

	@Test
	public void testMatchPathSlash() throws Exception {
		Object o = objectFactory.createObject("file:///path/");
		Assert.assertFalse(o instanceof TestResource);
		Assert.assertFalse(o instanceof TestSomething);
		Assert.assertTrue(o instanceof Anything);
		Assert.assertTrue(o instanceof AnyPath);
		Assert.assertFalse(o instanceof Path);
		Assert.assertTrue(o instanceof AnySubPath);
	}

	@Test
	public void testMatchSubPath() throws Exception {
		Object o = objectFactory.createObject("file:///path/sub");
		Assert.assertFalse(o instanceof TestResource);
		Assert.assertFalse(o instanceof TestSomething);
		Assert.assertTrue(o instanceof Anything);
		Assert.assertTrue(o instanceof AnyPath);
		Assert.assertFalse(o instanceof Path);
		Assert.assertTrue(o instanceof AnySubPath);
	}

}
