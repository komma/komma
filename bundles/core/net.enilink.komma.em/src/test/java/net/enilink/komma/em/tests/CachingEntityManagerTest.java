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
package net.enilink.komma.em.tests;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.KommaModule;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIs;
import net.enilink.komma.em.tests.concepts.Person;

public class CachingEntityManagerTest extends EntityManagerTest {
	private static final String NS = "test:";

	protected KommaModule createModule() throws Exception {
		KommaModule module = super.createModule();
		module.addConcept(Person.class);
		return module;
	}

	@Override
	public void beforeTest() throws Exception {
		super.beforeTest();
	}

	@Override
	protected boolean enableCaching() {
		return true;
	}

	@Test
	public void testCacheInvalidation() throws Exception {
		URI uriMax = URIs.createURI(NS + "max");
		IEntity entity = manager.find(uriMax);

		assertTrue(uriMax + " must be in the cache", entity == manager.find(uriMax));
		assertTrue(uriMax + " must not be a person", !(entity instanceof Person));

		Person max = manager.createNamed(uriMax, Person.class);
		max.setName("max");
		max.setAge(11);

		entity = manager.find(uriMax);
		assertTrue(uriMax + " must be a person", entity instanceof Person);
	}
}