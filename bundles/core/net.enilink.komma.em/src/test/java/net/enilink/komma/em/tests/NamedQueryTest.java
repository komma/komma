/*******************************************************************************
 * Copyright (c) 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.em.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;
import net.enilink.composition.annotations.Iri;
import net.enilink.composition.properties.annotations.Name;
import net.enilink.composition.properties.sparql.Sparql;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.komma.core.IGraph;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.KommaModule;
import net.enilink.komma.core.URIs;

@Ignore
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

		@Sparql(PREFIX + "SELECT ?friend WHERE { $this :friend ?friend . "
				+ "?friend :name $name }")
		Person findFriendByName(@Name("name") String arg1);

		@Sparql(PREFIX + "SELECT ?friend WHERE { $this :friend ?friend . "
				+ "?friend :name $name }")
		Object[] findByName(@Name("name") String arg1);

		@Sparql(PREFIX
				+ "CONSTRUCT { ?friend :name $name } WHERE { $this :friend ?friend . "
				+ "?friend :name $name }")
		IStatement findStatementByName(@Name("name") String arg1);

		@Sparql(PREFIX + "ASK { $this :friend $friend }")
		boolean isFriend(@Name("friend") Person arg1);

		@Sparql(PREFIX + "SELECT ?person WHERE { ?person a :Person }")
		IExtendedIterator<Person> findAllPeople();

		@Sparql(PREFIX + "SELECT ?person ?name "
				+ "WHERE { ?person :name ?name } ORDER BY ?name")
		IExtendedIterator<Object[]> findAllPeopleName();

		@Sparql(PREFIX + "CONSTRUCT { ?person a :Person; :name ?name } "
				+ "WHERE { ?person :name ?name } ORDER BY ?name")
		IExtendedIterator<IStatement> loadAllPeople();

		@Sparql(PREFIX + "CONSTRUCT { ?person a :Person; :name ?name } "
				+ "WHERE { ?person :name ?name } ORDER BY ?name")
		IGraph loadAllPeopleInGraph();

		@Sparql(PREFIX + "SELECT ?person WHERE { ?person a :Person }")
		Set<Person> findFriends();

		@Sparql(PREFIX + "SELECT $age WHERE { $this :age $age }")
		int findAge(@Name("age") int age);

		@Sparql(PREFIX + "SELECT ?nothing WHERE { $this :age $bool }")
		Object findNull(@Name("bool") boolean bool);
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

		@Sparql(PREFIX + "SELECT ?name WHERE { $this :name ?name }")
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
		me = manager.createNamed(URIs.createURI(NS + "me"), Person.class);
		me.setName("james");
		me.setAge(102);
		john = manager
				.createNamed(URIs.createURI(NS + "john"), Person.class);
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
		assertEquals(URIs.createURI(NS + "john"), result[0]);
	}

	@Test
	public void testStatementByName() throws Exception {
		IStatement result = me.findStatementByName("john");
		assertEquals(URIs.createURI(NS + "john"), result.getSubject());
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
		Employee e = manager.createNamed(URIs.createURI(NS + "e"),
				Employee.class);
		e.setName("employee");
		assertEquals("employee", e.findName());
	}
}
