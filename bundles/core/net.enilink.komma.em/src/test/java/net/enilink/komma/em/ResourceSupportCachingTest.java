package net.enilink.komma.em;

import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.KommaModule;
import net.enilink.komma.core.Statement;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIs;
import net.enilink.komma.em.concepts.IClass;
import net.enilink.komma.em.concepts.IResource;
import net.enilink.komma.em.concepts.ResourceSupport;
import net.enilink.komma.em.util.KommaUtil;
import net.enilink.vocab.owl.Class;
import net.enilink.vocab.rdf.RDF;

import org.junit.Assert;
import org.junit.Test;

public class ResourceSupportCachingTest extends EntityManagerTest {
	private static final String NS = "test:";

	protected KommaModule createModule() throws Exception {
		KommaModule module = super.createModule();
		module.includeModule(KommaUtil.getCoreModule());
		return module;
	}


	@Test
	public void testDirectNamedClasses() {
		URI uri = URIs.createURI(NS + "resource1");
		IEntity class3 = manager.find(URIs.createURI("class:3"));
		// add this point class:3 is not known to be an owl:Class
		Assert.assertFalse(class3 instanceof IClass);

		IResource r = manager.createNamed(uri, IResource.class);
		manager.add(new Statement(r, RDF.PROPERTY_TYPE,
			manager.createNamed(URIs.createURI("class:1"), Class.class)));
		manager.add(new Statement(r, RDF.PROPERTY_TYPE, URIs.createURI("class:2")));
		manager.add(new Statement(r, RDF.PROPERTY_TYPE, class3));
		for (Object c : r.getDirectNamedClasses()) {
			// at this point all classes should be viewed as IClass
			// it should not matter if they have rdf:type owl:Class or not
			Assert.assertTrue(c instanceof IClass);
		}
	}

	@Override
	protected boolean enableCaching() {
		return true;
	}
}