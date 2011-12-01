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
package net.enilink.composition.mappers;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import net.enilink.composition.annotations.Iri;
import net.enilink.composition.exceptions.ConfigException;
import net.enilink.composition.vocabulary.OBJ;
import net.enilink.composition.vocabulary.OWL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks the annotation, concept, and behaviour classes and what rdf:type they
 * should be used with.
 * 
 */
public class DefaultRoleMapper<T> implements Cloneable, RoleMapper<T> {
	private static Logger logger = LoggerFactory
			.getLogger(DefaultRoleMapper.class);

	private Map<Class<?>, T> annotations = new HashMap<Class<?>, T>();
	private Map<Class<?>, Class<?>> complements = new ConcurrentHashMap<Class<?>, Class<?>>();
	private Map<T, List<Class<?>>> instances = new ConcurrentHashMap<T, List<Class<?>>>(
			256);
	private Map<Class<?>, List<Class<?>>> intersections = new ConcurrentHashMap<Class<?>, List<Class<?>>>();
	private RoleMatcher matches = new RoleMatcher();
	private HierarchicalRoleMapper<T> roleMapper = new HierarchicalRoleMapper<T>();
	private TypeFactory<T> typeFactory;

	public DefaultRoleMapper(TypeFactory<T> typeFactory) {
		this.typeFactory = typeFactory;
		roleMapper.setTypeFactory(typeFactory);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.enilink.composition.mappers.RoleMapper#addAnnotation(java.lang.Class)
	 */
	@Override
	public void addAnnotation(Class<?> annotation) {
		if (!annotation.isAnnotationPresent(Iri.class))
			throw new IllegalArgumentException("@Iri annotation required in "
					+ annotation.getSimpleName());
		String uri = annotation.getAnnotation(Iri.class).value();
		addAnnotation(annotation, typeFactory.createType(uri));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.enilink.composition.mappers.RoleMapper#addAnnotation(java.lang.Class,
	 * T)
	 */
	@Override
	public void addAnnotation(Class<?> annotation, T uri) {
		annotations.put(annotation, uri);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.enilink.composition.mappers.RoleMapper#addBehaviour(java.lang.Class)
	 */
	@Override
	public void addBehaviour(Class<?> role) throws ConfigException {
		assertBehaviour(role);

		// behaviour is mapped explicitly with an annotation
		if (recordRole(role, role, null, false)) {
			return;
		}

		// behaviour is mapped by implementing a concept
		boolean hasType = false;
		for (Class<?> face : role.getInterfaces()) {
			boolean recorded = recordRole(role, face, null, false);
			if (recorded && hasType) {
				throw new ConfigException(role.getSimpleName()
						+ " can only implement one concept");
			} else {
				hasType |= recorded;
			}
		}
		if (!hasType)
			throw new ConfigException(role.getSimpleName()
					+ " must implement a concept or mapped explicitly");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.enilink.composition.mappers.RoleMapper#addBehaviour(java.lang.Class,
	 * T)
	 */
	@Override
	public void addBehaviour(Class<?> role, T type) throws ConfigException {
		assertBehaviour(role);
		recordRole(role, null, type, false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.enilink.composition.mappers.RoleMapper#addConcept(java.lang.Class)
	 */
	@Override
	public void addConcept(Class<?> role) throws ConfigException {
		recordRole(role, role, null, true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.enilink.composition.mappers.RoleMapper#addConcept(java.lang.Class, T)
	 */
	@Override
	public void addConcept(Class<?> role, T type) throws ConfigException {
		recordRole(role, role, type, false);
	}

	private void addInterfaces(Set<Class<?>> set, Class<?>... list) {
		for (Class<?> c : list) {
			if (c != null && set.add(c)) {
				addInterfaces(set, c.getSuperclass());
				addInterfaces(set, c.getInterfaces());
			}
		}
	}

	private void addIntersectionsAndComplements(Collection<Class<?>> roles) {
		for (Map.Entry<Class<?>, List<Class<?>>> e : intersections.entrySet()) {
			Class<?> inter = e.getKey();
			List<Class<?>> of = e.getValue();
			if (!roles.contains(inter) && intersects(roles, of)) {
				roles.add(inter);
			}
		}
		boolean complementAdded = false;
		for (Map.Entry<Class<?>, Class<?>> e : complements.entrySet()) {
			Class<?> comp = e.getKey();
			Class<?> of = e.getValue();
			if (!roles.contains(comp) && !contains(roles, of)) {
				complementAdded = true;
				roles.add(comp);
			}
		}
		if (complementAdded) {
			for (Map.Entry<Class<?>, List<Class<?>>> e : intersections
					.entrySet()) {
				Class<?> inter = e.getKey();
				List<Class<?>> of = e.getValue();
				if (!roles.contains(inter) && intersects(roles, of)) {
					roles.add(inter);
				}
			}
		}
	}

	private void assertBehaviour(Class<?> role) throws ConfigException {
		if (role.isInterface())
			throw new ConfigException(role.getSimpleName()
					+ " is an interface and not a behaviour");
//		for (Method method : role.getDeclaredMethods()) {
//			if (isAnnotationPresent(method)
//					&& method.getName().startsWith("get"))
//				throw new ConfigException(role.getSimpleName()
//						+ " cannot have a property annotation");
//		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.composition.mappers.RoleMapper#clone()
	 */
	@Override
	public DefaultRoleMapper<T> clone() {
		try {
			@SuppressWarnings("unchecked")
			DefaultRoleMapper<T> cloned = (DefaultRoleMapper<T>) super.clone();
			cloned.roleMapper = roleMapper.clone();
			cloned.matches = matches.clone();
			cloned.annotations = new HashMap<Class<?>, T>(annotations);
			cloned.complements = new ConcurrentHashMap<Class<?>, Class<?>>(
					complements);
			cloned.intersections = clone(intersections);
			return cloned;
		} catch (CloneNotSupportedException e) {
			throw new AssertionError();
		}
	}

	private <K, V> Map<K, List<V>> clone(Map<K, List<V>> map) {
		Map<K, List<V>> cloned = new ConcurrentHashMap<K, List<V>>(map);
		for (Map.Entry<K, List<V>> e : cloned.entrySet()) {
			e.setValue(new CopyOnWriteArrayList<V>(e.getValue()));
		}
		return cloned;
	}

	private boolean contains(Collection<Class<?>> roles, Class<?> of) {
		for (Class<?> type : roles) {
			if (of.isAssignableFrom(type))
				return true;
		}
		return false;
	}

	private void findAdditionalRoles(Collection<Class<?>> classes) {
		if (complements.isEmpty()) {
			return;
		}
		addIntersectionsAndComplements(classes);
	}

	private Collection<Class<?>> findAllRoles(T type) {
		Set<Class<?>> set = new HashSet<Class<?>>();
		for (Class<?> role : findRoles(type, new HashSet<Class<?>>())) {
			if (set.add(role)) {
				addInterfaces(set, role.getSuperclass());
				addInterfaces(set, role.getInterfaces());
			}
		}
		return set;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.enilink.composition.mappers.RoleMapper#findAnnotation(java.lang.Class
	 * )
	 */
	@Override
	public T findAnnotation(Class<?> type) {
		return annotations.get(type);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.enilink.composition.mappers.RoleMapper#findAnnotationString(java.
	 * lang.Class)
	 */
	@Override
	public String findAnnotationString(Class<?> type) {
		T annotation = findAnnotation(type);
		return annotation != null ? typeFactory.toString(annotation) : null;
	}

	private T findDefaultType(Class<?> role, AnnotatedElement element) {
		if (element.isAnnotationPresent(Iri.class)) {
			String value = element.getAnnotation(Iri.class).value();
			if (value != null) {
				return typeFactory.createType(value);
			}
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.composition.mappers.RoleMapper#findIndividualRoles(T,
	 * java.util.Collection)
	 */
	@Override
	public Collection<Class<?>> findIndividualRoles(T instance,
			Collection<Class<?>> classes) {
		List<Class<?>> list = instances.get(instance);
		if (list != null) {
			classes.addAll(list);
		}
		matches.findRoles(typeFactory.toString(instance), classes);
		return classes;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.composition.mappers.RoleMapper#findInterfaceConcept(T)
	 */
	@Override
	public Class<?> findInterfaceConcept(T uri) {
		Class<?> concept = null;
		Class<?> mapped = null;
		Collection<Class<?>> rs = findAllRoles(uri);
		for (Class<?> r : rs) {
			T type = findType(r);
			if (r.isInterface() && type != null) {
				concept = r;
				if (uri.equals(type)) {
					mapped = r;

					break;
				}
			}
		}
		if (mapped != null)
			return mapped;
		if (concept != null)
			return concept;
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.enilink.composition.mappers.RoleMapper#findRoles(java.util.Collection
	 * , java.util.Collection)
	 */
	@Override
	public Collection<Class<?>> findRoles(Collection<T> types,
			Collection<Class<?>> roles) {
		roleMapper.findRoles(types, roles);
		for (T type : types) {
			matches.findRoles(typeFactory.toString(type), roles);
		}
		findAdditionalRoles(roles);
		return roles;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.composition.mappers.RoleMapper#findRoles(T)
	 */
	@Override
	public Collection<Class<?>> findRoles(T type, Collection<Class<?>> roles) {
		roleMapper.findRoles(type, roles);
		matches.findRoles(typeFactory.toString(type), roles);
		findAdditionalRoles(roles);
		return roles;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.enilink.composition.mappers.RoleMapper#findSubTypes(java.lang.Class,
	 * java.util.Collection)
	 */
	@Override
	public Collection<T> findSubTypes(Class<?> role, Collection<T> rdfTypes) {
		return roleMapper.findSubTypes(role, rdfTypes);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.composition.mappers.RoleMapper#findType(java.lang.Class)
	 */
	@Override
	public T findType(Class<?> concept) {
		return roleMapper.findType(concept);
	}

	private boolean intersects(Collection<Class<?>> roles, List<Class<?>> ofs) {
		for (Class<?> of : ofs) {
			if (!contains(roles, of))
				return false;
		}
		return true;
	}

	private boolean isAnnotationPresent(AnnotatedElement role)
			throws ConfigException {
		return role.isAnnotationPresent(Iri.class);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.enilink.composition.mappers.RoleMapper#isIndividualRolesPresent(T)
	 */
	@Override
	public boolean isIndividualRolesPresent(T instance) {
		return !matches.isEmpty() || !instances.isEmpty()
				&& instances.containsKey(instance);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.composition.mappers.RoleMapper#isRecordedConcept(T)
	 */
	@Override
	public boolean isRecordedConcept(T type) {
		if (roleMapper.isTypeRecorded(type)) {
			for (Class<?> role : findAllRoles(type)) {
				if (findType(role) != null) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean recordAnonymous(Class<?> role, Class<?> elm)
			throws ConfigException {
		boolean recorded = false;
		for (Annotation ann : elm.getAnnotations()) {
			try {
				T name = findAnnotation(ann.annotationType());
				if (name == null
						&& ann.annotationType().isAnnotationPresent(Iri.class)) {
					addAnnotation(ann.annotationType());
					name = findAnnotation(ann.annotationType());
				}
				if (name == null) {
					continue;
				}

				String nameStr = typeFactory.toString(name);

				Object value = ann.getClass().getMethod("value").invoke(ann);
				if (OBJ.MATCHES.equals(nameStr)) {
					String[] values = (String[]) value;
					for (String pattern : values) {
						matches.addRoles(pattern, role);
						recorded = true;
					}
				}
				if (OWL.ONEOF.equals(nameStr)) {
					String[] values = (String[]) value;
					for (String instance : values) {
						T uri = typeFactory.createType(instance);
						List<Class<?>> list = instances.get(uri);
						if (list == null) {
							list = new CopyOnWriteArrayList<Class<?>>();
							instances.put(uri, list);
						}
						list.add(role);
						recorded = true;
					}
				}
				if (OWL.COMPLEMENTOF.equals(nameStr)) {
					if (value instanceof Class<?>) {
						Class<?> concept = (Class<?>) value;
						recordRole(concept, concept, null, true);
						complements.put(role, concept);
						recorded = true;
					} else {
						for (Class<?> concept : findRoles(
								typeFactory.createType((String) value),
								new HashSet<Class<?>>())) {
							complements.put(role, concept);
							recorded = true;
						}
					}
				}
				if (OWL.INTERSECTIONOF.equals(nameStr)) {
					List<Class<?>> ofs = new ArrayList<Class<?>>();
					for (Object v : (Object[]) value) {
						if (v instanceof Class<?>) {
							Class<?> concept = (Class<?>) v;
							recordRole(concept, concept, null, true);
							ofs.add(concept);
						} else {
							ofs.addAll(findRoles(
									typeFactory.createType((String) v),
									new HashSet<Class<?>>()));
						}
					}
					intersections.put(role, ofs);
					recorded = true;
				}
				if (OWL.UNIONOF.equals(nameStr)) {
					for (Object v : (Object[]) value) {
						if (v instanceof Class<?>) {
							Class<?> concept = (Class<?>) v;
							recordRole(concept, concept, null, true);
							recorded |= recordRole(role, concept, null, true);
						} else {
							for (Class<?> concept : findRoles(
									typeFactory.createType((String) v),
									new HashSet<Class<?>>())) {
								if (!role.equals(concept)) {
									recorded |= recordRole(role, concept, null,
											true);
								}
							}
						}
					}
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				continue;
			}
		}
		return recorded;
	}

	private boolean recordRole(Class<?> role, Class<?> element, T rdfType,
			boolean base) throws ConfigException {
		T defType = element == null ? null : findDefaultType(role, element);
		boolean hasType = false;
		if (rdfType != null) {
			if (role.isInterface()) {
				roleMapper.recordConcept(role, rdfType);
			} else {
				roleMapper.recordBehaviour(role, rdfType);
			}
			hasType = true;
		} else if (defType != null) {
			if (role.isInterface()) {
				roleMapper.recordConcept(role, defType);
			} else {
				roleMapper.recordBehaviour(role, defType);
			}
			hasType = true;
		} else if (element != null) {
			hasType = recordAnonymous(role, element);
		}
		if (!hasType && element != null) {
			for (Class<?> face : element.getInterfaces()) {
				hasType |= recordRole(role, face, null, false);
			}
		}
		if (!hasType && base) {
			throw new ConfigException(role.getSimpleName()
					+ " does not have an RDF type mapping");
		}
		return hasType;
	}
}
