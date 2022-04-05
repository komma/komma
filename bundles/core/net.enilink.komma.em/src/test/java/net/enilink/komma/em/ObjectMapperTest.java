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
package net.enilink.komma.em;

import net.enilink.komma.core.*;
import net.enilink.komma.em.concepts.ObjectWithTypeEnum;
import net.enilink.komma.em.concepts.Type;
import org.junit.Test;

import java.util.function.Consumer;

import static net.enilink.komma.em.concepts.Concepts.NS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ObjectMapperTest extends EntityManagerTest {
	protected KommaModule createModule() throws Exception {
		KommaModule module = super.createModule();
		module.addConcept(ObjectWithTypeEnum.class);
		module.addObjectMapper(Type.class, new IObjectMapper() {
			@Override
			public IReference getReference(Object object) {
				return URIs.createURI("type:" + ((Type)object).name());
			}

			@Override
			public Object readObject(IReference reference, IStatementSource source) {
				return Type.valueOf(reference.getURI().localPart());
			}

			@Override
			public void writeObject(Object object, Consumer<IStatement> sink) {
				// nothing to do here
			}
		});
		return module;
	}

	@Test
	public void testSerializeEnum() {
		ObjectWithTypeEnum o1 = manager.createNamed(URIs.createURI(NS + "o1"), ObjectWithTypeEnum.class);
		o1.setType(Type.A);
		assertTrue(manager.hasMatch((IReference)o1, URIs.createURI(NS + "type"), URIs.createURI("type:" + Type.A.name())));
	}

	@Test
	public void testDeserializeEnum() {
		URI o1Uri = URIs.createURI(NS + "o1");
		manager.add(new Statement(o1Uri, URIs.createURI(NS + "type"), URIs.createURI("type:" + Type.A.name())));
		ObjectWithTypeEnum o1 = manager.find(o1Uri, ObjectWithTypeEnum.class);
		assertEquals(Type.A, o1.getType());
	}
}