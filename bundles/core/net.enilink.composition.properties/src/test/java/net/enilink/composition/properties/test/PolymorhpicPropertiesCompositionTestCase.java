/*******************************************************************************
 * Copyright (c) 2022 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.composition.properties.test;

import net.enilink.composition.annotations.Iri;
import net.enilink.composition.mappers.RoleMapper;
import net.enilink.composition.properties.traits.PropertySetOwner;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class PolymorhpicPropertiesCompositionTestCase extends PropertiesCompositionTestCase {
	public interface AbstractValue {
	}

	@Iri("urn:test:ConcreteValue")
	public interface ConcreteValue extends AbstractValue {
	}

	public interface AbstractType {
		@Iri("urn:test:value")
		AbstractValue getValue();

		void setValue(AbstractValue value);
	}

	@Iri("urn:test:ConcreteType")
	public interface ConcreteType extends AbstractType {
		@Iri("urn:test:value")
		ConcreteValue getValue();

		void setValue(ConcreteValue value);
	}

	public abstract class ConcreteTypeBehaviour implements ConcreteType {
	}

	@Override
	protected void initRoleMapper(RoleMapper<String> roleMapper) {
		super.initRoleMapper(roleMapper);

		roleMapper.addConcept(ConcreteValue.class);
		roleMapper.addConcept(ConcreteType.class);
		roleMapper.addBehaviour(ConcreteTypeBehaviour.class);
	}

	@Test
	public void testCreateAndSet() throws Exception {
		ConcreteType object = objectFactory.createObject(ConcreteType.class);
		Assert.assertEquals(object.getValue(), null);
		ConcreteValue value = objectFactory.createObject(ConcreteValue.class);
		object.setValue(value);
		Assert.assertEquals(object.getValue(), value);
	}
}
