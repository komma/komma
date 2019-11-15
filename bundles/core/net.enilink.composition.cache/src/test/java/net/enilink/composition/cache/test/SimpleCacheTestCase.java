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
package net.enilink.composition.cache.test;

import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import net.enilink.composition.annotations.Iri;
import net.enilink.composition.cache.annotations.Cacheable;
import net.enilink.composition.mappers.RoleMapper;

public class SimpleCacheTestCase extends CacheTestCase {
	@Iri("urn:test:Node")
	public interface Node<T> {
		@Iri("urn:test:children")
		@Cacheable
		Set<T> getChildren(boolean x);
	}

	public static abstract class NodeSupport<T> implements Node<T> {
		static int count = 0;

		@Override
		@Cacheable
		public Set<T> getChildren(boolean x) {
			count += x ? 1 : 2;
			return new HashSet<T>();
		}
	}

	@Override
	protected void initRoleMapper(RoleMapper<String> roleMapper) {
		super.initRoleMapper(roleMapper);

		roleMapper.addConcept(Node.class);
		roleMapper.addBehaviour(NodeSupport.class);
	}

	@Before
	public void init() {
		NodeSupport.count = 0;
	}

	@Test
	public void testCached() throws Exception {
		@SuppressWarnings("unchecked")
		Node<String> node = objectFactory.createObject(Node.class);

		for (int i = 0; i < 3; i++) {
			node.getChildren(true);
		}

		Assert.assertEquals(1, NodeSupport.count);
	}

	@Test
	public void testNotCached() throws Exception {
		@SuppressWarnings("unchecked")
		Node<String> node = objectFactory.createObject(Node.class);

		for (int i = 0; i < 3; i++) {
			node.getChildren(false);
		}

		Assert.assertEquals(2, NodeSupport.count);
	}
}
