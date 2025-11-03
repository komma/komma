package net.enilink.komma.model;

import com.google.inject.Guice;
import net.enilink.komma.core.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ModelNamespaceTest {
	IModelSet modelSet;
	IModel model;
	IEntityManager manager;

	@BeforeEach
	void setUp() {
		KommaModule module = ModelPlugin.createModelSetModule(getClass().getClassLoader());
		IModelSetFactory factory = Guice.createInjector(new ModelSetModule(module)).getInstance(IModelSetFactory.class);
		modelSet = factory.createModelSet(MODELS.NAMESPACE_URI.appendLocalPart("MemoryModelSet"));
		model = modelSet.createModel(URIs.createURI("http://test/model"));
		manager = model.getManager();
	}

	@AfterEach
	void tearDown() {
		if (modelSet != null) modelSet.dispose();
	}

	@Test
	void testLoadTurtleWithPrefixes() throws Exception {
		String turtle = """
				@prefix ex: <http://example.org/ns#> .
				@prefix foo: <http://foo.org/> .
				<http://example.org/ns#s> <http://example.org/ns#p> <http://example.org/ns#o> .""";
		// Load the Turtle content into the model
		Map<String, Object> options = new HashMap<>();
		options.put(IModel.OPTION_MIME_TYPE, "text/turtle");
		model.load(new ByteArrayInputStream(turtle.getBytes()), options);

		// Check that the prefixes exist in the model's namespace mappings
		INamespace exNs = null, fooNs = null;
		for (Iterator<INamespace> it = model.getManager().getNamespaces(); it.hasNext(); ) {
			INamespace ns = it.next();
			if ("ex".equals(ns.getPrefix()) && "http://example.org/ns#".equals(ns.getURI().toString())) {
				exNs = ns;
			}
			if ("foo".equals(ns.getPrefix()) && "http://foo.org/".equals(ns.getURI().toString())) {
				fooNs = ns;
			}
		}
		assertNotNull(exNs, "Prefix 'ex' should be present after loading Turtle");
		assertNotNull(fooNs, "Prefix 'foo' should be present after loading Turtle");
	}

	@Test
	void testNamespaceAddRetrieveRemove() {
		// Add at least 5 namespaces
		Map<String, URI> namespaces = new LinkedHashMap<>();
		namespaces.put("ex1", URIs.createURI("http://example.org/ns1#"));
		namespaces.put("ex2", URIs.createURI("http://example.org/ns2#"));
		namespaces.put("ex3", URIs.createURI("http://example.org/ns3#"));
		namespaces.put("ex4", URIs.createURI("http://example.org/ns4#"));
		namespaces.put("ex5", URIs.createURI("http://example.org/ns5#"));

		// Add all namespaces
		for (Map.Entry<String, URI> entry : namespaces.entrySet()) {
			manager.setNamespace(entry.getKey(), entry.getValue());
		}
		// Verify all are retrievable
		for (Map.Entry<String, URI> entry : namespaces.entrySet()) {
			assertEquals(entry.getValue(), manager.getNamespace(entry.getKey()),
					"Namespace URI should be retrievable by prefix: " + entry.getKey());
		}
		// Verify all are present in getNamespaces
		Set<String> foundPrefixes = new HashSet<>();
		Set<URI> foundUris = new HashSet<>();
		for (Iterator<INamespace> it = manager.getNamespaces(); it.hasNext(); ) {
			INamespace ns = it.next();
			if (namespaces.containsKey(ns.getPrefix()) && namespaces.get(ns.getPrefix()).equals(ns.getURI())) {
				foundPrefixes.add(ns.getPrefix());
				foundUris.add(ns.getURI());
			}
		}
		assertEquals(namespaces.keySet(), foundPrefixes, "All prefixes should be present in getNamespaces()");
		assertEquals(new HashSet<>(namespaces.values()), foundUris, "All namespace URIs should be present in getNamespaces()");

		// Test overriding a prefix with a new URI
		String overridePrefix = "ex3";
		URI newUri = URIs.createURI("http://example.org/override#");
		manager.setNamespace(overridePrefix, newUri);
		assertEquals(newUri, manager.getNamespace(overridePrefix), "Prefix should now map to new URI");
		// Ensure old URI is not mapped to the prefix anymore
		assertNotEquals(namespaces.get(overridePrefix), manager.getNamespace(overridePrefix), "Old URI should not be mapped to prefix after override");
		// Check getNamespaces reflects the override
		boolean overrideFound = false;
		for (Iterator<INamespace> it = manager.getNamespaces(); it.hasNext(); ) {
			INamespace ns = it.next();
			if (overridePrefix.equals(ns.getPrefix()) && newUri.equals(ns.getURI())) {
				overrideFound = true;
				break;
			}
		}
		assertTrue(overrideFound, "Overridden namespace should be present in getNamespaces()");

		// Remove all namespaces and verify removal
		for (String prefix : namespaces.keySet()) {
			manager.removeNamespace(prefix);
			assertNull(manager.getNamespace(prefix), "Namespace should be removed: " + prefix);
		}
		// Remove the overridden prefix as well
		manager.removeNamespace(overridePrefix);
		assertNull(manager.getNamespace(overridePrefix), "Overridden namespace should be removed");
	}
}

