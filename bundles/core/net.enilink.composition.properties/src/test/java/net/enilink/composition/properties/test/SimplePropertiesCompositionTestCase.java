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
package net.enilink.composition.properties.test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import net.enilink.composition.annotations.Iri;
import net.enilink.composition.mappers.RoleMapper;
import net.enilink.composition.properties.traits.PropertySetOwner;

import org.junit.Assert;
import org.junit.Test;

public class SimplePropertiesCompositionTestCase extends
		PropertiesCompositionTestCase {
	@Iri("urn:test:Node")
	public interface Node<T> {
		@Iri("urn:test:parent")
		Node<T> getParent();

		void setParent(Node<T> parent);

		@Iri("urn:test:children")
		Set<T> getChildren();

		void setChildren(Set<T> children);

		@Iri("urn:test:sibling")
		T sibling();

		void sibling(T sibling);
	}

	@Override
	protected void initRoleMapper(RoleMapper<String> roleMapper) {
		super.initRoleMapper(roleMapper);

		roleMapper.addConcept(Node.class);
		roleMapper.addConcept(Chain.class);
		roleMapper.addConcept(WrongSetter.class);
	}

	@Test
	public void testSingle() throws Exception {
		@SuppressWarnings("unchecked")
		Node<String> node = objectFactory.createObject(Node.class);
		Assert.assertEquals(node.sibling(), null);
		node.sibling("a");
		Assert.assertEquals(node.sibling(), "a");

		Assert.assertEquals(node.getParent(), null);
		@SuppressWarnings("unchecked")
		Node<String> parent = objectFactory.createObject(Node.class);
		node.setParent(parent);
		Assert.assertEquals(node.getParent(), parent);
		// remove parent
		node.setParent(null);
		Assert.assertEquals(node.getParent(), null);
	}

	@Test
	public void testMultiple() throws Exception {
		@SuppressWarnings("unchecked")
		Node<String> node = objectFactory.createObject(Node.class);

		node.getChildren().add("a");
		Assert.assertEquals(node.getChildren(),
				new HashSet<String>(Arrays.asList("a")));

		node.setChildren(new HashSet<String>(Arrays.asList("a", "b", "c")));
		Assert.assertEquals(node.getChildren(),
				new HashSet<String>(Arrays.asList("a", "b", "c")));

		node.getChildren().clear();
		Assert.assertTrue(node.getChildren().isEmpty());
	}

	@Test
	public void testPropertySetOwner() throws Exception {
		@SuppressWarnings("unchecked")
		Node<String> node = objectFactory.createObject(Node.class);

		node.sibling("a");
		String sibling = ((PropertySetOwner) node).<String> getPropertySet(
				"urn:test:sibling").getSingle();
		Assert.assertEquals(node.sibling(), sibling);

		node.getChildren().addAll(Arrays.asList("a", "b"));
		Set<String> children = ((PropertySetOwner) node)
				.<String> getPropertySet("urn:test:children").getAll();
		Assert.assertEquals(node.getChildren(), children);
	}

	public interface SuperChain {

	}

	@Iri("urn:test:Chain")
	public interface Chain extends SuperChain {
		@Iri("urn:test:valueSetterInterfaceA")
		int valueSetterInterfaceA();

		Chain valueSetterInterfaceA(int value);

		@Iri("urn:test:valueSetterInterfaceB")
		int valueSetterInterfaceB();

		Chain valueSetterInterfaceB(int value);

		@Iri("urn:test:valueSetterSuperInterface")
		int valueSetterSuperInterface();

		SuperChain valueSetterSuperInterface(int value);

		@Iri("urn:test:valueSetterVoid")
		int valueSetterVoid();

		void valueSetterVoid(int value);
	}

	@Iri("urn:test:WrongSetter")
	public interface WrongSetter {
		@Iri("urn:test:valueSetterNotMatching")
		int valueSetterNotMatching();

		String valueSetterNotMatching(int value);
	}

	@Test
	public void testSetterInterface() {
		Chain chain = objectFactory.createObject(Chain.class);
		chain.valueSetterInterfaceA(5).valueSetterInterfaceB(7);
		Assert.assertEquals(chain.valueSetterInterfaceA(), 5);
		Assert.assertEquals(chain.valueSetterInterfaceB(), 7);
	}

	@Test
	public void testSetterSuperInterface() {
		Chain chain = objectFactory.createObject(Chain.class);
		chain.valueSetterSuperInterface(15);
		Assert.assertEquals(chain.valueSetterSuperInterface(), 15);
	}

	@Test
	public void testSetterVoid() {
		Chain chain = objectFactory.createObject(Chain.class);
		chain.valueSetterVoid(8);
		Assert.assertEquals(chain.valueSetterVoid(), 8);
	}

	@Test(expected = AssertionError.class)
	public void testSetterWrong() {
		objectFactory.createObject(WrongSetter.class);
	}
}
