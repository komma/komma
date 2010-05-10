/*******************************************************************************
 * Copyright (c) 2009, 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.internal.sesame.roles;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import net.enilink.composition.annotations.Iri;
import net.enilink.composition.annotations.matches;
import net.enilink.composition.mappers.RoleMapper;
import net.enilink.composition.mappers.TypeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.commons.iterator.WrappedIterator;
import net.enilink.komma.core.KommaException;

public class RoleClassLoader<URI> {
	private static final String ANNOTATIONS = "META-INF/org.openrdf.annotations";
	private static final String BEHAVIOURS = "META-INF/org.openrdf.behaviours";
	private static final String CONCEPTS = "META-INF/org.openrdf.concepts";

	private ClassLoader cl;

	private final Logger logger = LoggerFactory
			.getLogger(RoleClassLoader.class);

	private RoleMapper<URI> roleMapper;

	private TypeFactory<URI> typeFactory;

	private boolean isAnnotationPresent(Class<?> clazz) {
		for (Annotation ann : clazz.getAnnotations()) {
			String name = ann.annotationType().getName();
			if (Iri.class.getName().equals(name)) {
				return true;
			}
			if (matches.class.getName().equals(name)) {
				return true;
			}
		}
		return false;
	}

	public void load(Collection<? extends URL> libraries) {
		URLClassLoader urlCl = new URLClassLoader(libraries
				.toArray(new URL[libraries.size()]));

		Set<URL> seenUrls = new HashSet<URL>();
		for (String roles : Arrays.asList(CONCEPTS, BEHAVIOURS, ANNOTATIONS)) {
			try {
				IExtendedIterator<URL> resources = WrappedIterator
						.create(getClass().getClassLoader().getResources(roles))
						.andThen(WrappedIterator.create(cl.getResources(roles)))
						.andThen(
								WrappedIterator.create(urlCl
										.getResources(roles)));

				load(resources, !BEHAVIOURS.equals(roles), seenUrls);
			} catch (Exception e) {
				throw new KommaException(e);
			}
		}
	}

	public void load(Iterator<URL> resources, boolean concept, Set<URL> exclude)
			throws IOException, ClassNotFoundException {
		while (resources.hasNext()) {
			URL url = resources.next();
			if (!exclude.contains(url)) {
				exclude.add(url);
				logger.info("Reading roles from {}", url);
				try {
					Properties p = new Properties();
					p.load(url.openStream());

					load(p, cl, concept);
				} catch (IOException e) {
					String msg = e.getMessage() + " in: " + url;
					IOException ioe = new IOException(msg);
					ioe.initCause(e);
					throw ioe;
				}
			}
		}
	}

	private void load(Properties p, ClassLoader cl, boolean concept)
			throws IOException {
		for (Map.Entry<Object, Object> e : p.entrySet()) {
			String role = (String) e.getKey();

			String types = (String) e.getValue();
			try {
				Class<?> clazz = Class.forName(role, true, cl);
				for (String rdf : types.split("\\s+")) {
					recordRole(clazz, rdf, concept);
				}
			} catch (Throwable exc) {
				logger.error("Could not load " + role, exc);
			}
		}
	}

	private void recordRole(Class<?> clazz, String uri, boolean concept) {
		boolean uriMapped = uri != null && uri.length() > 0;
		if (!uriMapped || isAnnotationPresent(clazz)) {
			if (clazz.isAnnotation()) {
				roleMapper.addAnnotation(clazz);
			} else if (concept) {
				roleMapper.addConcept(clazz);
			} else {
				roleMapper.addBehaviour(clazz);
			}
		}

		if (uriMapped) {
			if (clazz.isAnnotation()) {
				roleMapper.addAnnotation(clazz, typeFactory.createType(uri));
			} else if (concept) {
				roleMapper.addConcept(clazz, typeFactory.createType(uri));
			} else {
				roleMapper.addBehaviour(clazz, typeFactory.createType(uri));
			}
		}
	}

	public void setClassLoader(ClassLoader cl) {
		this.cl = cl;
	}

	public void setRoleMapper(RoleMapper<URI> roleMapper) {
		this.roleMapper = roleMapper;
	}

	public void setTypeFactory(TypeFactory<URI> typeFactory) {
		this.typeFactory = typeFactory;
	}
}
