package net.enilink.komma.em;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import net.enilink.komma.core.*;
import net.enilink.komma.em.concepts.Person;
import net.enilink.vocab.rdf.RDF;
import net.enilink.vocab.xmlschema.XMLSCHEMA;
import org.junit.Test;

import java.util.Locale;
import java.util.Set;

import static net.enilink.komma.em.concepts.Concepts.NS;
import static org.junit.Assert.assertTrue;

public class SerializerTest {
	class SimplePerson implements Person {
		String name;
		int age;
		Set<Person> friends;

		@Override
		public String getName() {
			return name;
		}

		@Override
		public void setName(String name) {
			this.name = name;
		}

		@Override
		public int getAge() {
			return age;
		}

		@Override
		public void setAge(int age) {
			this.age = age;
		}

		@Override
		public Set<Person> getFriends() {
			return friends;
		}

		@Override
		public void setFriends(Set<Person> friends) {
			this.friends = friends;
		}
	}

	@Test
	public void testSerializer() {
		KommaModule kommaModule = new KommaModule();
		kommaModule.addConcept(Person.class);
		Injector injector = Guice.createInjector(new ManagerCompositionModule(kommaModule), new AbstractModule() {
			@Override
			protected void configure() {
				bind(Locale.class).toInstance(Locale.getDefault());
			}
		});
		Serializer serializer = injector.getInstance(Serializer.class);
		Person p = new SimplePerson();
		p.setName("Karl");
		p.setAge(12);
		IGraph graph = new LinkedHashGraph();
		serializer.serialize(p, stmt -> graph.add(stmt));

		assertTrue(graph.contains(null, RDF.PROPERTY_TYPE, URIs.createURI(NS + "Person")));
		assertTrue(graph.contains(null, URIs.createURI(NS + "name"), new Literal("Karl")));
		assertTrue(graph.contains(null, URIs.createURI(NS + "age"), new Literal("12", XMLSCHEMA.TYPE_INT)));
	}
}
