package net.enilink.komma.em;

import static org.junit.Assert.assertSame;

import org.junit.Test;

import com.google.inject.Inject;
import com.google.inject.Injector;

import net.enilink.composition.annotations.Iri;
import net.enilink.composition.properties.PropertySetFactory;
import net.enilink.composition.properties.komma.KommaPropertySetFactory;
import net.enilink.komma.core.KommaModule;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIs;

public class PropertySetsTest extends EntityManagerTest {
	private static final String NS = "test:";

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

	protected KommaModule createModule() throws Exception {
		KommaModule module = super.createModule();
		module.addConcept(Concept.class);
		module.addBehaviour(ConceptSupport.class);
		return module;
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

		/* --- */

		uri = URIs.createURI(NS + "two");

		a = manager.createNamed(uri, Concept.class);
		// initialize property set for a
		a.getName();
		b = manager.createNamed(uri, Concept.class);
		// set b's name
		b.setName("name");

		assertSame(a.getName(), b.getName());

		/* --- */

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

		assertSame(a.getName(), null);
		assertSame(b.getName(), "name");

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
