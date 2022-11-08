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
package net.enilink.composition.properties.test;

import net.enilink.composition.mappers.RoleMapper;
import net.enilink.composition.mapping.IPropertyMapper;
import net.enilink.composition.mapping.PropertyAttribute;
import net.enilink.composition.mapping.PropertyDescriptor;
import net.enilink.composition.properties.PropertySet;
import net.enilink.composition.properties.mapper.AbstractPropertyMapper;
import net.enilink.composition.properties.traits.PropertySetOwner;
import net.enilink.composition.properties.traits.Refreshable;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.*;

public class CustomPropertyMapperTest extends
		PropertiesCompositionTestCase {
	private Set<String> refreshed = new HashSet<>();

	public interface Node<T> {
		Node<T> getParent();

		void setParent(Node<T> parent);

		List<T> getChildren();

		void setChildren(List<T> children);

		T sibling();

		void sibling(T sibling);
	}

	class CustomPropertyMapper extends AbstractPropertyMapper {
		@Override
		protected String getPredicate(Method readMethod) {
			return "urn:test:" + decapitalize(readMethod.getName().replaceFirst("^get", ""));
		}

		@Override
		protected boolean isMappedGetter(Method method) {
			return !Void.TYPE.equals(method.getReturnType());
		}

		@Override
		protected List<PropertyAttribute> getAttributes(Method readMethod) {
			return Collections.emptyList();
		}

		@Override
		protected PropertyDescriptor createPropertyDescriptor(Method readMethod) {
			PropertyDescriptor pd = super.createPropertyDescriptor(readMethod);
			// wrap sets as lists if required
			if (List.class.isAssignableFrom(readMethod.getReturnType())) {
				pd.setEnforceList(true);
			}
			return pd;
		}
	}

	@Before
	public void init() {
		refreshed.clear();
	}

	@Override
	protected void initRoleMapper(RoleMapper<String> roleMapper) {
		super.initRoleMapper(roleMapper);

		roleMapper.addConcept(Node.class, "urn:test:Node");
	}

	@Override
	protected IPropertyMapper createPropertyMapper() {
		return new CustomPropertyMapper();
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
		Assert.assertEquals(node.getChildren(),	Arrays.asList("a"));

		node.setChildren(Arrays.asList("a", "b", "c"));
		Assert.assertEquals(node.getChildren(), Arrays.asList("a", "b", "c"));

		((Refreshable)node).refresh();
		Assert.assertTrue(refreshed.contains("urn:test:children"));

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
		Assert.assertEquals(new HashSet<>(node.getChildren()), children);
	}

	@Override
	public <E> PropertySet<E> createPropertySet(Object bean, String uri,
	                                            Class<E> elementType, PropertyAttribute... attributes) {
		return new TestPropertySet<>() {
			@Override
			public void refresh() {
				refreshed.add(uri);
			}
		};
	}
}
