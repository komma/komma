/*
 * Copyright (c) 2009, 2010, James Leigh All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution. 
 * - Neither the name of the openrdf.org nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package net.enilink.composition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.enilink.composition.exceptions.CompositionException;
import net.enilink.composition.helpers.ClassCompositor;
import net.enilink.composition.mappers.RoleMapper;
import net.enilink.composition.mappers.TypeFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

/**
 * Find a proxy class that can be used for a set of rdf:types.
 * 
 */
@Singleton
public class ClassResolver<T> {
	private static final String PKG_PREFIX = "object.proxies._";
	private static final String CLASS_PREFIX = "_EntityProxy";

	private ClassDefiner definer;
	private RoleMapper<T> mapper;
	private ConcurrentMap<Collection<T>, Class<?>> multiples = new ConcurrentHashMap<Collection<T>, Class<?>>();

	private Injector injector;

	private TypeFactory<T> typeFactory;

	@Inject
	Set<BehaviourFactory> behaviourFactories;

	@Inject
	public ClassResolver(RoleMapper<T> mapper, ClassDefiner cp,
			TypeFactory<T> typeFactory, Injector injector) {
		this.mapper = mapper;
		this.definer = cp;
		this.typeFactory = typeFactory;
		this.injector = injector;
	}

	@Inject
	public void setBehaviourFactories(Set<BehaviourFactory> behaviourFactories) {
		this.behaviourFactories = behaviourFactories;
	}

	public Class<?> resolveComposite(T resource, Collection<T> types) {
		Collection<Class<?>> roles = new ArrayList<Class<?>>();
		mapper.findIndividualRoles(resource, roles);
		mapper.findRoles(types, roles);
		return getCompositeClass(roles);
	}

	public Class<?> resolveComposite(Collection<T> types) {
		Class<?> proxy = multiples.get(types);
		if (proxy != null) {
			return proxy;
		}
		Collection<Class<?>> roles = new ArrayList<Class<?>>();
		mapper.findRoles(types, roles);
		proxy = getCompositeClass(roles);
		multiples.putIfAbsent(types, proxy);
		return proxy;
	}

	private Class<?> getCompositeClass(Collection<Class<?>> roles) {
		try {
			String className = getJavaClassName(roles);
			return getCompositeClass(className, roles);
		} catch (Exception e) {
			List<String> roleNames = new ArrayList<String>();
			for (Class<?> f : roles) {
				roleNames.add(f.getSimpleName());
			}
			throw new CompositionException(e.toString()
					+ " for entity with roles: " + roleNames, e);
		}
	}

	private Class<?> getCompositeClass(String className,
			Collection<Class<?>> roles) throws Exception {
		try {
			return Class.forName(className, true, definer);
		} catch (ClassNotFoundException e) {
			synchronized (definer) {
				try {
					return Class.forName(className, true, definer);
				} catch (ClassNotFoundException e1) {
					return composeBehaviours(className, roles);
				}
			}
		}
	}

	private Class<?> composeBehaviours(String className,
			Collection<Class<?>> roles) throws Exception {
		List<Class<?>> types = new ArrayList<Class<?>>(roles.size());
		types.addAll(roles);
		types = removeSuperClasses(types);
		ClassCompositor<T> cc = new ClassCompositor<T>(className, types.size());

		// TODO check what must be done to make this work for TypeFactory and
		// RoleMapper
		injector.injectMembers(cc);

		cc.setTypeFactory(typeFactory);
		cc.setRoleMapper(mapper);

		Set<Class<?>> behaviours = new LinkedHashSet<Class<?>>(types.size());
		for (Class<?> role : types) {
			if (role.isInterface()) {
				cc.addInterface(role);
			} else {
				behaviours.add(role);
			}
		}

		cc.addAllBehaviours(findImplementations(behaviours));
		cc.addAllBehaviours(findImplementations(cc.getInterfaces()));

		return cc.compose();
	}

	public Collection<Class<?>> findImplementations(Collection<Class<?>> classes) {
		try {
			Set<Class<?>> faces = new HashSet<Class<?>>();
			for (Class<?> c : classes) {
				faces.add(c);
				faces = findImplementedClasses(c, faces);
			}
			List<Class<?>> behaviours = new ArrayList<Class<?>>();
			for (Class<?> face : faces) {
				for (BehaviourFactory factory : behaviourFactories) {
					Class<?> implementation = factory.implement(face);
					if (implementation != null) {
						behaviours.add(implementation);
					}
				}
			}
			return behaviours;
		} catch (CompositionException e) {
			throw e;
		} catch (Exception e) {
			throw new CompositionException(e);
		}
	}

	protected Set<Class<?>> findImplementedClasses(Class<?> role,
			Set<Class<?>> implementations) {
		for (Class<?> face : role.getInterfaces()) {
			if (implementations.add(face)) {
				findImplementedClasses(face, implementations);
			}
		}
		if (!role.isInterface()) {
			Class<?> superclass = role.getSuperclass();
			if (superclass != null && !Object.class.equals(superclass)) {
				findImplementedClasses(superclass, implementations);
			}
		}
		return implementations;
	}

	private List<Class<?>> removeSuperClasses(List<Class<?>> classes) {
		for (int i = classes.size() - 1; i >= 0; i--) {
			Class<?> c = classes.get(i);
			for (int j = classes.size() - 1; j >= 0; j--) {
				Class<?> d = classes.get(j);
				if (i != j && c.isAssignableFrom(d)
						&& c.isInterface() == d.isInterface()) {
					classes.remove(i);
					break;
				}
			}
		}
		return classes;
	}

	private String getJavaClassName(Collection<Class<?>> javaClasses) {
		String phex = packagesToHexString(javaClasses);
		String chex = classesToHexString(javaClasses);
		return PKG_PREFIX + phex + "." + CLASS_PREFIX + chex;
	}

	private String packagesToHexString(Collection<Class<?>> javaClasses) {
		TreeSet<String> names = new TreeSet<String>();
		for (Class<?> clazz : javaClasses) {
			if (clazz.getPackage() != null) {
				names.add(clazz.getPackage().getName());
			}
		}
		return toHexString(names);
	}

	private String classesToHexString(Collection<Class<?>> javaClasses) {
		TreeSet<String> names = new TreeSet<String>();
		for (Class<?> clazz : javaClasses) {
			names.add(clazz.getName());
		}
		return toHexString(names);
	}

	private String toHexString(TreeSet<String> names) {
		long hashCode = 0;
		for (String name : names) {
			hashCode = 31 * hashCode + name.hashCode();
		}
		return Long.toHexString(hashCode);
	}

}
