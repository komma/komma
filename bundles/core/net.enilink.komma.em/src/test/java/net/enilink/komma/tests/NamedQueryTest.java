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

import java.util.Set;

import junit.framework.Test;

import net.enilink.composition.annotations.Iri;
import net.enilink.composition.properties.annotations.name;
import net.enilink.composition.properties.sparql.sparql;
import org.openrdf.model.Model;
import org.openrdf.model.Statement;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.BindingSet;
import org.openrdf.result.GraphResult;
import org.openrdf.result.Result;
import org.openrdf.result.TupleResult;

import net.enilink.komma.core.KommaModule;
import net.enilink.komma.core.URIImpl;

public class NamedQueryTest extends KommaManagerTestCase {
	private static final String NS = "urn:test:";
	private static final String PREFIX = "PREFIX :<" + NS + ">\n";
	private Person me;
	private Person john;

	public static Test suite() throws Exception {
		return NamedQueryTest.suite(NamedQueryTest.class);
	}

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
		BindingSet findBindingSetByName(@name("name") String arg1);

		@sparql(PREFIX
				+ "CONSTRUCT { ?friend :name $name } WHERE { $this :friend ?friend . "
				+ "?friend :name $name }")
		Statement findStatementByName(@name("name") String arg1);

		@sparql(PREFIX + "ASK { $this :friend $friend }")
		boolean isFriend(@name("friend") Person arg1);

		@sparql(PREFIX + "SELECT ?person WHERE { ?person a :Person }")
		Result<Person> findAllPeople();

		@sparql(PREFIX + "SELECT ?person ?name "
				+ "WHERE { ?person :name ?name } ORDER BY ?name")
		TupleResult findAllPeopleName();

		@sparql(PREFIX + "CONSTRUCT { ?person a :Person; :name ?name } "
				+ "WHERE { ?person :name ?name } ORDER BY ?name")
		GraphResult loadAllPeople();

		@sparql(PREFIX + "CONSTRUCT { ?person a :Person; :name ?name } "
				+ "WHERE { ?person :name ?name } ORDER BY ?name")
		Model loadAllPeopleInModel();

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
	public void setUp() throws Exception {
		super.setUp();
		me = manager.createNamed(URIImpl.createURI(NS + "me"), Person.class);
		me.setName("james");
		me.setAge(102);
		john = manager.createNamed(URIImpl.createURI(NS + "john"), Person.class);
		john.setName("john");
		me.getFriends().add(john);
	}

	public void testFriendByName() throws Exception {
		assertEquals(john, me.findFriendByName("john"));
	}

	public void testBindingSetByName() throws Exception {
		ValueFactory vf = manager.getConnection().getValueFactory();
		BindingSet result = me.findBindingSetByName("john");
		assertEquals(vf.createURI(NS, "john"), result.getValue("friend"));
	}

	public void testStatementByName() throws Exception {
		ValueFactory vf = manager.getConnection().getValueFactory();
		Statement result = me.findStatementByName("john");
		assertEquals(vf.createURI(NS, "john"), result.getSubject());
	}

	public void testIsFriend() throws Exception {
		assertTrue(me.isFriend(john));
	}

	public void testFindAllPeople() throws Exception {
		Result<Person> result = me.findAllPeople();
		assertTrue(result.hasNext());
		Set<Person> set = result.asSet();
		assertTrue(set.contains(me));
		assertTrue(set.contains(john));
	}

	public void testTupleResult() throws Exception {
		TupleResult result = me.findAllPeopleName();
		assertTrue(result.hasNext());
		assertEquals("james", result.next().getValue("name").stringValue());
		result.close();
	}

	public void testConstruct() throws Exception {
		GraphResult result = me.loadAllPeople();
		assertTrue(result.hasNext());
		result.close();
	}

	public void testModel() throws Exception {
		Model result = me.loadAllPeopleInModel();
		assertFalse(result.isEmpty());
	}

	public void testSet() throws Exception {
		Set<Person> set = me.findFriends();
		assertEquals(2, set.size());
		assertTrue(set.contains(me));
		assertTrue(set.contains(john));
	}

	public void testInt() throws Exception {
		int age = me.getAge();
		assertEquals(age, me.findAge(age));
	}

	public void testBool() throws Exception {
		me.findNull(true);
	}

	public void testOveride() throws Exception {
		Employee e = manager
				.createNamed(URIImpl.createURI(NS + "e"), Employee.class);
		e.setName("employee");
		assertEquals("employee", e.findName());
	}
}
