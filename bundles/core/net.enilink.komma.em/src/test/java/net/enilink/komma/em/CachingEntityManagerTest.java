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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.KommaModule;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIs;
import net.enilink.komma.em.concepts.Person;
import net.enilink.vocab.rdfs.Resource;

public class CachingEntityManagerTest extends EntityManagerTest {
	private static final String NS = "test:";

	protected KommaModule createModule() throws Exception {
		KommaModule module = super.createModule();
		module.addConcept(Person.class);
		module.addConcept(Resource.class);
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
	public void testCacheInvalidation() {
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

	@Test
	public void testAdHocConversion() {
		URI uriMoritz = URIs.createURI(NS + "moritz");
		// this line does not yet add the bean to the cache because createNamed interprets types as "restricted" to the given ones
		IEntity moritz = manager.createNamed(uriMoritz, Resource.class);
		assertNotNull(moritz);
		// the following statement adds the bean to the cache
		moritz = manager.find(uriMoritz, Resource.class);
		assertNotNull(moritz);
		assertTrue(uriMoritz + " must be converted to a person",
			manager.toInstance(uriMoritz, Person.class, null) instanceof Person);
	}
}