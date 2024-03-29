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
package net.enilink.komma.em;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import net.enilink.komma.core.KommaModule;
import net.enilink.komma.core.URIs;
import net.enilink.komma.em.concepts.Person;

public class BasicManagerTest extends EntityManagerTest {
	private static final String NS = "test:";
	private Person max;
	private Person moritz;

	protected KommaModule createModule() throws Exception {
		KommaModule module = super.createModule();
		module.addConcept(Person.class);
		return module;
	}

	@Override
	public void beforeTest() throws Exception {
		super.beforeTest();
		max = manager.createNamed(URIs.createURI(NS + "max"), Person.class);
		max.setName("max");
		max.setAge(11);
		moritz = manager.createNamed(URIs.createURI(NS + "moritz"), Person.class);
		moritz.setName("moritz");
		moritz.setAge(12);
		max.getFriends().add(moritz);
	}

	@Test
	public void testSimpleGetters() throws Exception {
		assertEquals("max", max.getName());
		assertEquals(11, max.getAge());
		assertEquals("moritz", moritz.getName());
		assertEquals(12, moritz.getAge());
	}

	@Test
	public void testSetGetters() throws Exception {
		assertTrue(String.format("%s friends contains %s", max, moritz), max.getFriends().contains(moritz));
		assertFalse(String.format("%s friends contains %s", moritz, max), moritz.getFriends().contains(max));
		assertEquals(1, max.getFriends().size());
	}
}