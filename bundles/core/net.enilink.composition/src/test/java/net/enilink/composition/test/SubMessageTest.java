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
package net.enilink.composition.test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.Set;

import org.junit.Assert;
import net.enilink.composition.annotations.ParameterTypes;
import net.enilink.composition.annotations.Iri;
import net.enilink.composition.concepts.Message;
import net.enilink.composition.mappers.RoleMapper;

public class SubMessageTest extends CompositionTestCase {
	@Iri("http://www.w3.org/2000/01/rdf-schema#" + "subClassOf")
	@Retention(RetentionPolicy.RUNTIME)
	public @interface subMessageOf {
		public String[] value();
	}

	@Iri("urn:test:Concept")
	public interface Concept {
		void msg1();

		void msg2();

		void msg3();

		void msg4();

		void msg5();

		void msg6();

		void msg7();

		Object msg8();

		String msg9();

		Set<?> msg10();

		@subMessageOf("urn:test:msg10")
		String msg11();
	}

	@Iri("urn:test:msg6")
	public interface Msg6 extends Message {
	}

	public static abstract class Behaviour implements Concept {
		public static int base;
		public static int message;

		@Iri("urn:test:base")
		public void base() {
			base++;
		}

		@subMessageOf("urn:test:base")
		@ParameterTypes( {})
		public void msg1(Message msg) throws Exception {
			message++;
			msg.proceed();
		}

		@subMessageOf("urn:test:base")
		public void msg2() {
			message++;
		}

		@subMessageOf("urn:test:base")
		@Iri("urn:test:msg3")
		public void msg3() {
			message++;
		}

		@subMessageOf("urn:test:msg3")
		public void msg4() {
			message++;
		}

		@Iri("urn:test:msg5")
		public void msg5(Message msg) {
			if (msg instanceof Msg6) {
				message++;
			}
		}

		@subMessageOf("urn:test:msg5")
		@Iri("urn:test:msg6")
		public void msg6() {
			message++;
		}

		@Iri("urn:test:msg8")
		public Object msg8() {
			message++;
			return "msg8";
		}

		@subMessageOf("urn:test:msg8")
		@Iri("urn:test:msg9")
		public String msg9() {
			message++;
			return null;
		}

		@Iri("urn:test:msg10")
		public Set<?> msg10() {
			message++;
			return Collections.singleton("msg10");
		}
	}

	public static abstract class Behaviour2 implements Concept {
		public static int message;

		@subMessageOf("urn:test:base")
		public void msg7() {
			message++;
		}
	}

	Concept concept;

	public void setUp() throws Exception {
		super.setUp();

		Behaviour.base = 0;
		Behaviour.message = 0;
		Behaviour2.message = 0;

		concept = (Concept) objectFactory.createObject("urn:test:Concept");
	}

	@Override
	protected void initRoleMapper(RoleMapper<String> roleMapper) {
		super.initRoleMapper(roleMapper);

		roleMapper.addAnnotation(subMessageOf.class);
		roleMapper.addConcept(Concept.class);
		roleMapper.addConcept(Msg6.class);
		roleMapper.addBehaviour(Behaviour.class);
		roleMapper.addBehaviour(Behaviour2.class);
	}

	@org.junit.Test
	public void testBaseMessage() throws Exception {
		concept.msg1();
		Assert.assertEquals(1, Behaviour.message);
		Assert.assertEquals(1, Behaviour.base);
	}

	@org.junit.Test
	public void testMsg2() throws Exception {
		concept.msg2();
		Assert.assertEquals(1, Behaviour.message);
		Assert.assertEquals(1, Behaviour.base);
	}

	@org.junit.Test
	public void testMsg4() throws Exception {
		concept.msg4();
		Assert.assertEquals(2, Behaviour.message);
		Assert.assertEquals(1, Behaviour.base);
	}

	@org.junit.Test
	public void testMsg6() throws Exception {
		concept.msg6();
		Assert.assertEquals(2, Behaviour.message);
	}

	@org.junit.Test
	public void testMsg7() throws Exception {
		concept.msg7();
		Assert.assertEquals(1, Behaviour2.message);
		Assert.assertEquals(1, Behaviour.base);
	}

	@org.junit.Test
	public void testMsg9() throws Exception {
		Assert.assertEquals("msg8", concept.msg9());
		Assert.assertEquals(2, Behaviour.message);
	}

	@org.junit.Test
	public void testMsg10() throws Exception {
		Assert.assertEquals("msg10", concept.msg11());
		Assert.assertEquals(1, Behaviour.message);
	}
}
