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
package net.enilink.komma.em.tests;

import static org.junit.Assert.assertEquals;

import java.util.Set;

import org.junit.Test;
import net.enilink.composition.annotations.Iri;

import net.enilink.komma.core.KommaModule;
import net.enilink.komma.core.URIImpl;

public class BasicManagerTest extends EntityManagerTest {
	private static final String NS = "urn:test:";
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
	public void testFriendName() throws Exception {
		assertEquals("james", me.getName());
	}
}
