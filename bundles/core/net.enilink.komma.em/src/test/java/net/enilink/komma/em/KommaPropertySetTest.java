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
package net.enilink.komma.em;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import net.enilink.composition.annotations.Iri;
import net.enilink.composition.mapping.PropertyAttribute;
import net.enilink.composition.properties.PropertySet;
import net.enilink.composition.properties.PropertySetFactory;
import net.enilink.composition.properties.komma.KommaPropertySet;
import net.enilink.composition.properties.komma.KommaPropertySetFactory;
import net.enilink.komma.core.*;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class KommaPropertySetTest extends EntityManagerTest {
	private static final String NS = "test:";

	protected KommaModule createModule() throws Exception {
		KommaModule module = super.createModule();
		module.addConcept(Concept.class);
		module.addBehaviour(ConceptSupport.class);
		return module;
	}

	@Override
	protected boolean enableCaching() {
		return true;
	}

	@Override
	protected Module createEntityManagerModule() {
		return new CachingEntityManagerModule() {
			@Override
			protected Class<? extends PropertySetFactory> getPropertySetFactoryClass() {
				return TestKommaPropertySetFactory.class;
			}
		};
	}

	@Iri(NS + "Concept")
	public interface Concept {
		@Iri(NS + "name")
		String getName();

		void setName(String name);

		Injector getInjector();
	}

	public static abstract class ConceptSupport implements Concept {
		@Inject
		Injector injector;

		public Injector getInjector() {
			return injector;
		}
	}

	public static class TestKommaPropertySetFactory extends KommaPropertySetFactory {
		@Override
		protected <E> KommaPropertySet<E> createPropertySetInternal(boolean localized, IReference subject, IReference property,
		                                                            Class<E> valueType, URI rdfValueType) {
			if (localized) {
				throw new IllegalArgumentException("Not supported by this test");
			}
			return new TestKommaPropertySet<E>(this, subject, property, valueType, rdfValueType);
		}
	}

	public static class TestKommaPropertySet<E> extends KommaPropertySet<E> {
		public TestKommaPropertySet(KommaPropertySetFactory factory, IReference subject, IReference property,
		                            Class valueType, URI rdfValueType) {
			super(factory, subject, property, valueType, rdfValueType);
		}
	}

	/**
	 * Tests that no stale values are cached when multiple threads
	 * with transactions are used.
	 */
	@Test
	public void testCachingMultipleThreads() throws InterruptedException {
		URI uri = URIs.createURI(NS + "one");
		Concept a = manager.createNamed(uri, Concept.class);
		a.setName("a");

		assertEquals("a", a.getName());

		ITransaction tx = manager.getTransaction();
		tx.begin();
		a.setName("b");
		Thread thread = new Thread(() -> {
			uow.begin();
			try {
				assertEquals("a", a.getName());
			} finally {
				uow.end();
			}
		});
		thread.start();
		thread.join();

		assertEquals("b", a.getName());

		tx.commit();
		assertEquals("b", a.getName());
	}

	/**
	 * Tests if property sets are unique throughout all beans.
	 * This is achieved by using weak reference based caching.
	 */
	@Test
	public void testUniquePropertySets() {
		URI uri = URIs.createURI(NS + "one");

		Concept a = manager.createNamed(uri, Concept.class);
		a.setName("name");
		Concept b = manager.createNamed(uri, Concept.class);

		assertSame(a.getName(), b.getName());

		// ---

		uri = URIs.createURI(NS + "two");

		a = manager.createNamed(uri, Concept.class);
		// initialize property set for a
		a.getName();
		b = manager.createNamed(uri, Concept.class);
		// set b's name
		b.setName("name");

		assertSame(a.getName(), b.getName());

		// ---

		uri = URIs.createURI(NS + "three");

		a = manager.createNamed(uri, Concept.class);
		// initialize property set for a
		a.getName();
		// clear cache of property sets
		((KommaPropertySetFactory) a.getInjector()
				.getInstance(PropertySetFactory.class)).getPropertySetCache().clear();
		b = manager.createNamed(uri, Concept.class);
		// set b's name
		b.setName("name");

		assertSame(null, a.getName());
		assertSame("name", b.getName());

		// reinitialize a with new property set
		a = manager.createNamed(uri, Concept.class);
		// initialize property set for a
		a.getName();

		Concept c = manager.createNamed(uri, Concept.class);
		// set c's name -> should also set a's and b's name
		b.setName("otherName");

		assertSame(a.getName(), b.getName());
		assertSame(b.getName(), c.getName());
	}
}
