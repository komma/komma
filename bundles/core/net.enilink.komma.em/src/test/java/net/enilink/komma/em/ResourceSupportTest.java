package net.enilink.komma.em;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import net.enilink.komma.core.ITransaction;
import net.enilink.komma.core.KommaModule;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIs;
import net.enilink.komma.em.KommaPropertySetTest.Concept;
import net.enilink.komma.em.concepts.IClass;
import net.enilink.komma.em.concepts.IResource;
import net.enilink.komma.em.util.KommaUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

public class ResourceSupportTest extends EntityManagerTest {
	private static final String NS = "test:";

	protected KommaModule createModule() throws Exception {
		KommaModule module = super.createModule();
		module.includeModule(KommaUtil.getCoreModule());
		return module;
	}

	@Test
	public void testSetAndGetMethods() {
		URI uri = URIs.createURI(NS + "resource1");
		IResource r = manager.createNamed(uri, IResource.class);

		URI flag = URIs.createURI(NS + "flag");

		r.set(flag, true);
		assertEquals(Boolean.TRUE, r.getSingle(flag));
		assertEquals(Arrays.asList(Boolean.TRUE), new ArrayList<>((Collection<?>)r.get(flag)));

		r.set(flag, null);
		assertEquals(null, r.getSingle(flag));
		assertTrue(((Collection<?>) r.get(flag)).isEmpty());
	}

	@Test
	public void testSetAndGetMethodsWithThreading() throws InterruptedException {
		URI uri = URIs.createURI(NS + "resource1");
		IResource r = manager.createNamed(uri, IResource.class);

		URI name = URIs.createURI(NS + "name");
		URI flag = URIs.createURI(NS + "flag");

		r.set(name, "a");
		r.set(flag, true);

		ITransaction tx = manager.getTransaction();
		tx.begin();
		r.set(name, "b");
		r.set(flag, null);

		Thread thread = new Thread(() -> {
			uow.begin();
			try {
				assertEquals("a", r.getSingle(name));
				assertEquals(true, r.getSingle(flag));

				// test if new or cached been has the same values
				IResource r2 = manager.find(uri, IResource.class);
				assertEquals("a", r2.getSingle(name));
				assertEquals(true, r2.getSingle(flag));
			} finally {
				uow.end();
			}
		});
		thread.start();
		thread.join();

		assertEquals("b", r.getSingle(name));
		assertEquals(null, r.getSingle(flag));

		tx.commit();
		assertEquals("b", r.getSingle(name));
		assertEquals(null, r.getSingle(flag));

		// test if new or cached been has the same values
		IResource r2 = manager.find(uri, IResource.class);
		assertEquals("b", r2.getSingle(name));
		assertEquals(null, r2.getSingle(flag));
	}
}