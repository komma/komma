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

import junit.framework.Assert;

import org.junit.Test;
import net.enilink.composition.annotations.Iri;
import net.enilink.composition.mappers.RoleMapper;

public class SimplePropertiesCompositionTestCase extends
		PropertiesCompositionTestCase {
	@Iri("urn:test:Node")
	public interface Node<T> {
		@Iri("urn:test:parent")
		T getParent();

		void setParent(T parent);

		@Iri("urn:test:children")
		Set<T> getChildren();

		void setChildren(Set<T> children);

		@Iri("urn:test:sibling")
		T getSibling();

		void setSibling(T sibling);
	}

	@Override
	protected void initRoleMapper(RoleMapper<String> roleMapper) {
		super.initRoleMapper(roleMapper);

		roleMapper.addConcept(Node.class);
	}

	@Test
	public void testSingle() throws Exception {
		@SuppressWarnings("unchecked")
		Node<String> node = objectFactory.createObject(Node.class);

		Assert.assertEquals(node.getSibling(), null);

		node.setSibling("a");
		Assert.assertEquals(node.getSibling(), "a");
	}

	@Test
	public void testMultiple() throws Exception {
		@SuppressWarnings("unchecked")
		Node<String> node = objectFactory.createObject(Node.class);

		node.getChildren().add("a");
		Assert.assertEquals(node.getChildren(), new HashSet<String>(Arrays
				.asList("a")));

		node.setChildren(new HashSet<String>(Arrays.asList("a", "b", "c")));
		Assert.assertEquals(node.getChildren(), new HashSet<String>(Arrays
				.asList("a", "b", "c")));

		node.getChildren().clear();
		Assert.assertTrue(node.getChildren().isEmpty());
	}
}
