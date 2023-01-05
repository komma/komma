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

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.commons.iterator.NiceIterator;
import net.enilink.commons.iterator.WrappedIterator;
import net.enilink.composition.annotations.Iri;
import net.enilink.composition.mapping.PropertyAttribute;
import net.enilink.composition.properties.PropertySet;
import net.enilink.composition.properties.PropertySetFactory;
import net.enilink.composition.properties.komma.KommaPropertySet;
import net.enilink.composition.properties.komma.KommaPropertySetFactory;
import net.enilink.komma.core.*;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

import java.util.AbstractList;
import java.util.List;
import java.util.Set;

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

		@Iri(NS + "flag")
		Boolean getFlag();

		void setFlag(Boolean flag);

		@Iri(NS + "anyProperty")
		Set<Object> getAnyProperty();

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
			return new TestKommaPropertySet<E>(subject, property, valueType, rdfValueType);
		}
	}

	public static class TestKommaPropertySet<E> extends KommaPropertySet<E> {
		public TestKommaPropertySet(IReference subject, IReference property, Class valueType, URI rdfValueType) {
			super(subject, property, valueType, rdfValueType);
		}

		@Override
		protected IExtendedIterator<E> createElementsIterator(String filterPattern, int limit) {
			return new WrappedIterator<>(super.createElementsIterator(filterPattern, limit)) {
				@Override
				public boolean hasNext() {
					while (waitInIteration) {
						try {
							Thread.sleep(5);
						} catch (InterruptedException ex) {
							ex.printStackTrace();
						}
					}
					return super.hasNext();
				}
			};
		}
	}

	volatile static boolean waitInIteration = false;

	@BeforeClass
	public static void before() {
		waitInIteration = false;
	}

	/**
	 * Tests that no stale values are cached when multiple threads
	 * with transactions are used.
	 */
	@Test
	public void testCachingTxAndThreads() throws InterruptedException {
		URI uri = URIs.createURI(NS + "one");
		Concept a = manager.createNamed(uri, Concept.class);
		a.setName("a");
		a.setFlag(true);

		ITransaction tx = manager.getTransaction();
		tx.begin();
		a.setName("b");
		a.setFlag(false);

		Thread thread = new Thread(() -> {
			uow.begin();
			try {
				assertEquals("a", a.getName());
				assertEquals(true, a.getFlag());
			} finally {
				uow.end();
			}
		});
		thread.start();
		thread.join();

		assertEquals("b", a.getName());
		assertEquals(false, a.getFlag());

		tx.commit();
		assertEquals("b", a.getName());
		assertEquals(false, a.getFlag());
	}

	/**
	 * Tests that no stale values are cached when multiple threads
	 * with transactions are used.
	 */
	@Test
	public void testCachingMultipleThreadsReadInBetween() throws InterruptedException {
		URI uri = URIs.createURI(NS + "one");
		Concept a = manager.createNamed(uri, Concept.class);
		a.setName("a");
		a.setFlag(true);

		assertEquals("a", a.getName());

		ITransaction tx = manager.getTransaction();
		tx.begin();
		a.setName("b");
		a.setFlag(null);
		assertEquals(null, a.getFlag());
		assertEquals("b", a.getName());

		Thread thread = new Thread(() -> {
			uow.begin();
			try {
				assertEquals("a", a.getName());
				assertEquals(true, a.getFlag());
			} finally {
				uow.end();
			}
		});
		thread.start();
		thread.join();

		assertEquals("b", a.getName());
		assertEquals(null, a.getFlag());

		tx.commit();
		assertEquals("b", a.getName());
		assertEquals(null, a.getFlag());
	}

	@Test
	public void testCachingMultipleThreadsWithoutTx() throws InterruptedException {
		URI uri = URIs.createURI(NS + "one");
		Concept a = manager.createNamed(uri, Concept.class);
		a.setName("a");
		a.setFlag(true);

		assertEquals("a", a.getName());

		Thread thread = new Thread(() -> {
			uow.begin();
			try {
				a.setFlag(null);
			} finally {
				uow.end();
			}
			waitInIteration = false;
		});

		Thread thread2 = new Thread(() -> {
			waitInIteration = true;
			uow.begin();
			try {
				a.getFlag();
			} finally {
				uow.end();
			}
		});

		thread2.start();
		thread.start();
		thread.join();
		thread2.join();

		waitInIteration = false;
		assertEquals(null, a.getFlag());
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

    /**
     * Tests that also properties of type {@link java.lang.Object} are working correctly.
     */
	@Test
	public void testObjectValuedProperty() {
		URI uri = URIs.createURI(NS + "one");
		URI anyProperty = URIs.createURI(NS + "anyProperty");
		Concept a = manager.createNamed(uri, Concept.class);
		a.setName("name");
		manager.add(new Statement(uri, anyProperty, "string"));
		assertEquals("string", a.getAnyProperty().toArray()[0]);
		manager.remove(new Statement(uri, anyProperty, null));
		manager.add(new Statement(uri, anyProperty, 1337));
		manager.refresh(a);
		assertEquals(1337, a.getAnyProperty().toArray()[0]);
	}
}
