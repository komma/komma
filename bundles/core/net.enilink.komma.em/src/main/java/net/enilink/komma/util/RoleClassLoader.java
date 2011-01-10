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
package net.enilink.komma.util;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import net.enilink.composition.annotations.Iri;
import net.enilink.composition.annotations.matches;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.commons.iterator.WrappedIterator;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.core.KommaModule;

public class RoleClassLoader {
	private static final String ANNOTATIONS = "META-INF/org.openrdf.annotations";
	private static final String BEHAVIOURS = "META-INF/org.openrdf.behaviours";
	private static final String CONCEPTS = "META-INF/org.openrdf.concepts";

	private final Logger logger = LoggerFactory
			.getLogger(RoleClassLoader.class);

	private KommaModule module;

	public RoleClassLoader(KommaModule module) {
		this.module = module;
	}

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

	public void load(Iterable<? extends URL> libraries) {
		List<URL> libraryList = new ArrayList<URL>();
		for (URL library : libraries) {
			libraryList.add(library);
		}

		URLClassLoader urlCl = new URLClassLoader(
				libraryList.toArray(new URL[libraryList.size()]));

		Set<URL> seenUrls = new HashSet<URL>();
		for (String roles : Arrays.asList(CONCEPTS, BEHAVIOURS, ANNOTATIONS)) {
			try {
				IExtendedIterator<URL> resources = WrappedIterator.create(
						module.getClassLoader().getResources(roles)).andThen(
						WrappedIterator.create(urlCl.getResources(roles)));

				load(resources, seenUrls);
			} catch (Exception e) {
				throw new KommaException(e);
			}
		}
	}

	private void load(Iterator<URL> resources, Set<URL> exclude)
			throws IOException, ClassNotFoundException {
		while (resources.hasNext()) {
			URL url = resources.next();
			if (!exclude.contains(url)) {
				exclude.add(url);
				logger.info("Reading roles from {}", url);
				try {
					Properties p = new Properties();
					p.load(url.openStream());

					load(p);
				} catch (IOException e) {
					String msg = e.getMessage() + " in: " + url;
					IOException ioe = new IOException(msg);
					ioe.initCause(e);
					throw ioe;
				}
			}
		}
	}

	private void load(Properties p) throws IOException {
		for (Map.Entry<Object, Object> e : p.entrySet()) {
			String role = (String) e.getKey();

			String types = (String) e.getValue();
			try {
				Class<?> clazz = Class.forName(role, true,
						module.getClassLoader());
				for (String rdf : types.split("\\s+")) {
					recordRole(clazz, rdf);
				}
			} catch (Throwable exc) {
				logger.error("Could not load " + role, exc);
			}
		}
	}

	private void recordRole(Class<?> clazz, String uri) {
		boolean uriMapped = uri != null && uri.length() > 0;
		if (!uriMapped || isAnnotationPresent(clazz)) {
			if (clazz.isAnnotation()) {
				module.addAnnotation(clazz);
			} else if (clazz.isInterface()) {
				module.addConcept(clazz);
			} else {
				module.addBehaviour(clazz);
			}
		}

		if (uriMapped) {
			if (clazz.isAnnotation()) {
				module.addAnnotation(clazz, uri);
			} else if (clazz.isInterface()) {
				module.addConcept(clazz, uri);
			} else {
				module.addBehaviour(clazz, uri);
			}
		}
	}
}
