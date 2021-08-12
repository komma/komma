package net.enilink.komma.em.tests;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.MembersInjector;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.KommaModule;
import net.enilink.komma.core.visitor.IDataVisitor;
import net.enilink.komma.em.ManagerCompositionModule;
import net.enilink.komma.em.Serializer;
import net.enilink.komma.em.tests.concepts.Person;
import org.junit.Test;

import java.util.Locale;
import java.util.Set;

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
		serializer.serialize(p, new IDataVisitor<Void>() {
			@Override
			public Void visitBegin() {
				return null;
			}

			@Override
			public Void visitStatement(IStatement stmt) {
				System.out.println(stmt);
				return null;
			}

			@Override
			public Void visitEnd() {
				return null;
			}
		});
	}
}
