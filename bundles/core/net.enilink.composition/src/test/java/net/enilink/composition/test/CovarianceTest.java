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

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import net.enilink.composition.annotations.Iri;
import net.enilink.composition.mappers.RoleMapper;

public class CovarianceTest extends CompositionTestCase {
	@Iri("urn:test:Base")
	public interface Base<B> {
		B getParent();

		void setParent(B parent);

		B[] getChildren();

		void setChildren(B[] children);

		@Iri("urn:test:sibling")
		B getSibling();

		void setSibling(B sibling);

		@Iri("urn:test:self")
		B getSelf();

		void setSelf(B self);
	}

	public abstract static class BaseSupport<B> implements Base<B> {
		private B sibling, self;

		public B getSelf() {
			return self;
		}

		public void setSelf(B self) {
			this.self = self;
		}

		public B getSibling() {
			return sibling;
		}

		public void setSibling(B sibling) {
			this.sibling = sibling;
		}
	}

	@Iri("urn:test2:Covariance")
	public interface Covariance extends Base<Covariance> {
		@Iri("urn:test2:sibling")
		Covariance getSibling();

		void setSibling(Covariance sibling);
	}

	public abstract static class CovarianceSupport implements Covariance {
		private Covariance parent, sibling;
		private Covariance[] children;

		public Covariance getParent() {
			return parent;
		}

		public void setParent(Covariance parent) {
			this.parent = parent;
		}

		public Covariance[] getChildren() {
			return children;
		}

		public void setChildren(Covariance[] children) {
			this.children = children;
		}

		public Covariance getSibling() {
			return sibling;
		}

		public void setSibling(Covariance sibling) {
			this.sibling = sibling;
		}
	}

	public void testNumberOfBaseMethods() throws Exception {
		Assert.assertEquals(8, Base.class.getDeclaredMethods().length);
		Base<?> obj = objectFactory.createObject(Base.class);
		Assert.assertEquals(1, findMethods(obj, "getParent").size());
		Assert.assertEquals(1, findMethods(obj, "setParent").size());
		Assert.assertEquals(1, findMethods(obj, "getChildren").size());
		Assert.assertEquals(1, findMethods(obj, "setChildren").size());
		Assert.assertEquals(1, findMethods(obj, "getSibling").size());
		Assert.assertEquals(1, findMethods(obj, "setSibling").size());
		Assert.assertEquals(1, findMethods(obj, "getSelf").size());
		Assert.assertEquals(1, findMethods(obj, "setSelf").size());
	}

	public void testNumberOfCovarianceMethods() throws Exception {
		Assert.assertEquals(2, Covariance.class.getDeclaredMethods().length);
		Covariance obj = objectFactory.createObject(Covariance.class);
		// support class with bridges
		Assert.assertEquals(2, findMethods(obj, "getParent").size());
		Assert.assertEquals(2, findMethods(obj, "setParent").size());
		Assert.assertEquals(2, findMethods(obj, "getChildren").size());
		Assert.assertEquals(2, findMethods(obj, "setChildren").size());
		// two properties
		Assert.assertEquals(2, findMethods(obj, "getSibling").size());
		Assert.assertEquals(2, findMethods(obj, "setSibling").size());
		// only one property
		Assert.assertEquals(1, findMethods(obj, "getSelf").size());
		Assert.assertEquals(1, findMethods(obj, "setSelf").size());
	}

	public void testCovariance() throws Exception {
		Covariance obj = objectFactory.createObject(Covariance.class);
		Covariance parent = objectFactory.createObject(Covariance.class);
		obj.setParent(parent);
		Assert.assertEquals(parent, Covariance.class.getMethod("getParent")
				.invoke(obj));
		Assert.assertEquals(parent, obj.getParent());
		Base<Covariance> base = obj;
		base.setParent(parent);
		Assert.assertEquals(parent, Base.class.getMethod("getParent").invoke(
				obj));
		Assert.assertEquals(parent, base.getParent());
	}

	@SuppressWarnings("unchecked")
	public void testArrayCovariance() throws Exception {
		Covariance obj = objectFactory.createObject(Covariance.class);
		Covariance child = objectFactory.createObject(Covariance.class);
		Covariance[] children = new Covariance[] { child };
		obj.setChildren(children);
		Assert.assertArrayEquals(children, obj.getChildren());
		Base base = obj;
		base.setChildren(children);
		Assert.assertArrayEquals(children, base.getChildren());
	}

	public void testDifferentProperties() throws Exception {
		Covariance obj = objectFactory.createObject(Covariance.class);
		Covariance sibling = objectFactory.createObject(Covariance.class);
		Base<Covariance> base = obj;
		base.setSibling(sibling);
		Assert.assertEquals(sibling, base.getSibling());
		Assert.assertEquals(sibling, obj.getSibling());
		obj.setSibling(null);
		// base property should remain unchanged
		Assert.assertEquals(sibling, base.getSibling());
		base.setSibling(null);
		Assert.assertEquals(null, base.getSibling());
		obj.setSibling(sibling);
		Assert.assertEquals(sibling, obj.getSibling());
		Assert.assertEquals(sibling, base.getSibling());
	}

	public void testSameProperty() throws Exception {
		Covariance obj = objectFactory.createObject(Covariance.class);
		Covariance self = objectFactory.createObject(Covariance.class);
		Base<Covariance> base = obj;
		base.setSelf(self);
		Assert.assertEquals(self, base.getSelf());
		Assert.assertEquals(self, obj.getSelf());
		obj.setSelf(null);
		Assert.assertEquals(null, obj.getSelf());
		Assert.assertEquals(null, base.getSelf());
		obj.setSelf(self);
		Assert.assertEquals(self, obj.getSelf());
		Assert.assertEquals(self, base.getSelf());
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void initRoleMapper(RoleMapper<String> roleMapper) {
		super.initRoleMapper(roleMapper);

		roleMapper.addConcept(Base.class);
		roleMapper.addConcept(Covariance.class);
		roleMapper.addBehaviour(BaseSupport.class);
		roleMapper.addBehaviour(CovarianceSupport.class);
	}

	private Set<Method> findMethods(Object obj, String name) {
		Set<Method> methods = new HashSet<Method>();
		for (Method m : obj.getClass().getMethods()) {
			if (m.getName().equals(name))
				methods.add(m);
		}
		System.out.println(methods);
		return methods;
	}

}
