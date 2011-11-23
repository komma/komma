/*******************************************************************************
 * Copyright (c) 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Test;
import net.enilink.composition.annotations.Iri;
import net.enilink.composition.properties.annotations.name;
import net.enilink.composition.properties.sparql.sparql;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.komma.core.IGraph;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.KommaModule;
import net.enilink.komma.core.URIImpl;

public class NamedQueryTest extends EntityManagerTest {
	private static final String NS = "urn:test:";
	private static final String PREFIX = "PREFIX :<" + NS + ">\n";
	private Person me;
	private Person john;

	@Iri(NS + "Person")
	public interface Person {
		@Iri(NS + "name")
		String getName();

		void setName(String name);

		@Iri(NS + "age")
		int getAge();

		void setAge(int age);

		@Iri(NS + "friend")
		Set<Person> getFriends();

		void setFriends(Set<Person> friends);

		@sparql(PREFIX + "SELECT ?friend WHERE { $this :friend ?friend . "
				+ "?friend :name $name }")
		Person findFriendByName(@name("name") String arg1);

		@sparql(PREFIX + "SELECT ?friend WHERE { $this :friend ?friend . "
				+ "?friend :name $name }")
		Object[] findByName(@name("name") String arg1);

		@sparql(PREFIX
				+ "CONSTRUCT { ?friend :name $name } WHERE { $this :friend ?friend . "
				+ "?friend :name $name }")
		IStatement findStatementByName(@name("name") String arg1);

		@sparql(PREFIX + "ASK { $this :friend $friend }")
		boolean isFriend(@name("friend") Person arg1);

		@sparql(PREFIX + "SELECT ?person WHERE { ?person a :Person }")
		IExtendedIterator<Person> findAllPeople();

		@sparql(PREFIX + "SELECT ?person ?name "
				+ "WHERE { ?person :name ?name } ORDER BY ?name")
		IExtendedIterator<Object[]> findAllPeopleName();

		@sparql(PREFIX + "CONSTRUCT { ?person a :Person; :name ?name } "
				+ "WHERE { ?person :name ?name } ORDER BY ?name")
		IExtendedIterator<IStatement> loadAllPeople();

		@sparql(PREFIX + "CONSTRUCT { ?person a :Person; :name ?name } "
				+ "WHERE { ?person :name ?name } ORDER BY ?name")
		IGraph loadAllPeopleInGraph();

		@sparql(PREFIX + "SELECT ?person WHERE { ?person a :Person }")
		Set<Person> findFriends();

		@sparql(PREFIX + "SELECT $age WHERE { $this :age $age }")
		int findAge(@name("age") int age);

		@sparql(PREFIX + "SELECT ?nothing WHERE { $this :age $bool }")
		Object findNull(@name("bool") boolean bool);
	}

	@Iri(NS + "Employee")
	public static class Employee {
		@Iri(NS + "name")
		String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@sparql(PREFIX + "SELECT ?name WHERE { $this :name ?name }")
		public String findName() {
			return "not overriden";
		}
	}

	protected KommaModule createModule() throws Exception {
		KommaModule module = super.createModule();
		module.addConcept(Person.class);
		module.addConcept(Employee.class);
		return module;
	}

	@Override
	public void beforeTest() throws Exception {
		super.beforeTest();
		me = manager.createNamed(URIImpl.createURI(NS + "me"), Person.class);
		me.setName("james");
		me.setAge(102);
		john = manager
				.createNamed(URIImpl.createURI(NS + "john"), Person.class);
		john.setName("john");
		me.getFriends().add(john);
	}
	
	@Test
	public void testFriendByName() throws Exception {
		assertEquals(john, me.findFriendByName("john"));
	}

	@Test
	public void testBindingSetByName() throws Exception {
		Object[] result = me.findByName("john");
		assertEquals(URIImpl.createURI(NS + "john"), result[0]);
	}

	@Test
	public void testStatementByName() throws Exception {
		IStatement result = me.findStatementByName("john");
		assertEquals(URIImpl.createURI(NS + "john"), result.getSubject());
	}
	
	@Test
	public void testIsFriend() throws Exception {
		assertTrue(me.isFriend(john));
	}

	@Test
	public void testFindAllPeople() throws Exception {
		IExtendedIterator<Person> result = me.findAllPeople();
		assertTrue(result.hasNext());
		Set<Person> set = result.toSet();
		assertTrue(set.contains(me));
		assertTrue(set.contains(john));
	}

	@Test
	public void testTupleResult() throws Exception {
		IExtendedIterator<Object[]> result = me.findAllPeopleName();
		assertTrue(result.hasNext());
		assertEquals("james", result.next()[1].toString());
		result.close();
	}

	@Test
	public void testConstruct() throws Exception {
		IExtendedIterator<?> result = me.loadAllPeople();
		assertTrue(result.hasNext());
		result.close();
	}

	@Test
	public void testModel() throws Exception {
		IGraph result = me.loadAllPeopleInGraph();
		assertFalse(result.isEmpty());
	}

	@Test
	public void testSet() throws Exception {
		Set<Person> set = me.findFriends();
		assertEquals(2, set.size());
		assertTrue(set.contains(me));
		assertTrue(set.contains(john));
	}

	@Test
	public void testInt() throws Exception {
		int age = me.getAge();
		assertEquals(age, me.findAge(age));
	}

	@Test
	public void testBool() throws Exception {
		me.findNull(true);
	}

	@Test
	public void testOverride() throws Exception {
		Employee e = manager.createNamed(URIImpl.createURI(NS + "e"),
				Employee.class);
		e.setName("employee");
		assertEquals("employee", e.findName());
	}
}
