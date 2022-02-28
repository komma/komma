package net.enilink.komma.em;

import net.enilink.komma.core.KommaModule;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIs;
import net.enilink.komma.em.concepts.IClass;
import net.enilink.komma.em.util.KommaUtil;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ClassSupportTest extends EntityManagerTest {
	private static final String NS = "test:";

	protected KommaModule createModule() throws Exception {
		KommaModule module = super.createModule();
		module.includeModule(KommaUtil.getCoreModule());
		return module;
	}

	protected void computeAllSubClasses(String name, Map<String, String[]> classes, Set<String> subClasses) {
		String[] directSubClasses = classes.get(name);
		if (directSubClasses != null) {
			for (String directSubClass : directSubClasses) {
				if (subClasses.add(directSubClass)) {
					computeAllSubClasses(directSubClass, classes, subClasses);
				}
			}
		}
	}

	@Test
	public void testSubClasses() {
		Map<String, String[]> classes = new HashMap<>();
		classes.put("c1", new String[] {"c1.1", "c1.2"});
		classes.put("c1.1", new String[] {"c1.1.1", "c1.1.2"});

		classes.forEach((name, directSubClasses) -> {
			URI uri = URIs.createURI(NS + name);
			IClass c = manager.createNamed(uri, IClass.class);
			for (String subClass : directSubClasses) {
				uri = URIs.createURI(NS + subClass);
				manager.createNamed(uri, IClass.class).getRdfsSubClassOf().add(c);
			}
		});

		classes.keySet().forEach(name -> {
			Set<String> allSubClasses = new HashSet<>();
			computeAllSubClasses(name, classes, allSubClasses);

			IClass c = manager.find(URIs.createURI(NS + name), IClass.class);
			c.getNamedSubClasses().forEach(subClass -> {
				assertTrue(subClass + " should be sub class of " + c,
						allSubClasses.contains(subClass.getURI().localPart()));
			});

			assertSame(allSubClasses.size(), c.getNamedSubClasses().toSet().size());
		});

		classes.forEach((name, directSubClasses) -> {
			Set<String> directNamedSubClasses = new HashSet<>(Arrays.asList(directSubClasses));

			IClass c = manager.find(URIs.createURI(NS + name), IClass.class);
			c.getDirectNamedSubClasses().forEach(subClass -> {
				assertTrue(subClass + " should be a direct sub class of " + c,
						directNamedSubClasses.contains(subClass.getURI().localPart()));
			});

			assertSame(directSubClasses.length, c.getDirectNamedSubClasses().toSet().size());
		});

		classes.keySet().forEach(name -> {
			IClass c = manager.find(URIs.createURI(NS + name), IClass.class);
			assertTrue(c.hasNamedSubClasses(true));
			assertTrue(c.hasSubClasses(true));
		});

		classes.forEach((name, directSubClasses) -> {
			for (String subClass : directSubClasses) {
				IClass c = manager.find(URIs.createURI(NS + subClass), IClass.class);
				assertTrue(c.getNamedSuperClasses().toList().size() == 1);
			}
		});
	}
}