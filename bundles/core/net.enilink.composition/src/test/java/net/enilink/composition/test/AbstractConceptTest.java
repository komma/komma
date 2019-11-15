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
package net.enilink.composition.test;

import net.enilink.composition.annotations.Iri;
import net.enilink.composition.mappers.RoleMapper;
import net.enilink.composition.traits.Behaviour;

import org.junit.Assert;
import org.junit.Test;

public class AbstractConceptTest extends CompositionTestCase {
	public static interface Person {
		@Iri("urn:test:name")
		String getName();

		void setName(String name);

		String getFirstName();
	}

	public static abstract class PersonSupport {
		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	public static abstract class FirstNameSupport implements Person,
			Behaviour<Person> {
		public String getFirstName() {
			return getName().split(" ")[0];
		}
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void initRoleMapper(RoleMapper<String> roleMapper) {
		super.initRoleMapper(roleMapper);

		roleMapper.addConcept(Person.class, "urn:test:Person");
		roleMapper.addBehaviour(FirstNameSupport.class, "urn:test:Person");
		roleMapper.addBehaviour(PersonSupport.class, "urn:test:Person");
	}

	@Test
	public void testAbstractConcept() throws Exception {
		Person person = (Person) objectFactory.createObject("urn:test:Person");

		person.setName("James Leigh");
		Assert.assertEquals("James", person.getFirstName());
	}
}
