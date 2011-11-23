/*
 * Copyright (c) 2007, 2010, James Leigh All rights reserved.
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
package net.enilink.komma.core;

import static java.util.Collections.unmodifiableSet;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import net.enilink.composition.annotations.Iri;

/**
 * Defines the Scope of an {@link IEntityManager} and its factory. This includes
 * roles, literals, factories, datasets, and contexts.
 * 
 * @author James Leigh
 * @author Ken Wenzel
 * 
 */
public class KommaModule {
	public static class Association {
		private Class<?> javaClass;

		private String rdfType;

		private Association(Class<?> javaClass, String rdfType) {
			this.javaClass = javaClass;
			this.rdfType = rdfType;
		}

		public Class<?> getJavaClass() {
			return javaClass;
		}

		public String getRdfType() {
			return rdfType;
		}

		public String toString() {
			return javaClass.getName() + "=" + rdfType;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((javaClass == null) ? 0 : javaClass.hashCode());
			result = prime * result
					+ ((rdfType == null) ? 0 : rdfType.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Association other = (Association) obj;
			if (javaClass == null) {
				if (other.javaClass != null)
					return false;
			} else if (!javaClass.equals(other.javaClass))
				return false;
			if (rdfType == null) {
				if (other.rdfType != null)
					return false;
			} else if (!rdfType.equals(other.rdfType))
				return false;
			return true;
		}
	}

	private static class CombinedClassLoader extends ClassLoader {
		private Set<ClassLoader> alternatives;

		public CombinedClassLoader(ClassLoader parent) {
			super(parent);
		}

		public void addAlternative(ClassLoader loader) {
			if (alternatives == null) {
				alternatives = new LinkedHashSet<ClassLoader>();
			}
			if (loader instanceof CombinedClassLoader) {
				if (((CombinedClassLoader) loader).alternatives != null) {
					alternatives
							.addAll(((CombinedClassLoader) loader).alternatives);
				} else {
					alternatives.add(loader.getParent());
				}
			} else {
				alternatives.add(loader);
			}
		}

		@Override
		public URL getResource(String name) {
			URL resource = super.getResource(name);
			if (resource == null && alternatives != null) {
				for (ClassLoader alt : alternatives) {
					resource = alt.getResource(name);
					if (resource != null) {
						break;
					}
				}
			}
			return resource;
		}

		@Override
		public InputStream getResourceAsStream(String name) {
			InputStream stream = super.getResourceAsStream(name);
			if (stream == null && alternatives != null) {
				for (ClassLoader alt : alternatives) {
					stream = alt.getResourceAsStream(name);
					if (stream != null) {
						break;
					}
				}
			}
			return stream;
		}

		@Override
		public Enumeration<URL> getResources(String name) throws IOException {
			Vector<URL> list = new Vector<URL>();
			Enumeration<URL> e = super.getResources(name);
			while (e.hasMoreElements()) {
				list.add(e.nextElement());
			}
			if (alternatives != null) {
				for (ClassLoader alt : alternatives) {
					e = alt.getResources(name);
					while (e.hasMoreElements()) {
						list.add(e.nextElement());
					}
				}
			}
			return list.elements();
		}

		@Override
		public Class<?> loadClass(String name) throws ClassNotFoundException {
			try {
				return super.loadClass(name);
			} catch (ClassNotFoundException e) {
				if (alternatives != null) {
					for (ClassLoader alt : alternatives) {
						try {
							return alt.loadClass(name);
						} catch (ClassNotFoundException e2) {
							// ignore and try next alternative class loader
						}
					}
				}
				throw e;
			}
		}
	}

	private CombinedClassLoader cl;

	private Map<Class<?>, Association> annotations = new HashMap<Class<?>, Association>();

	private Set<Association> datatypes = new HashSet<Association>();

	private Set<Association> concepts = new HashSet<Association>();

	private Set<Association> behaviours = new HashSet<Association>();

	private Set<URI> readableGraphs = new LinkedHashSet<URI>();

	private Set<URI> writableGraphs = new LinkedHashSet<URI>();

	public KommaModule() {
		ClassLoader contextClassLoader = Thread.currentThread()
				.getContextClassLoader();
		if (contextClassLoader != null) {
			cl = new CombinedClassLoader(contextClassLoader);
		}
		if (cl == null) {
			cl = new CombinedClassLoader(getClass().getClassLoader());
		}
	}

	public KommaModule(ClassLoader classLoader) {
		cl = new CombinedClassLoader(classLoader);
		cl.addAlternative(getClass().getClassLoader());
	}

	public synchronized ClassLoader getClassLoader() {
		return cl;
	}

	public Set<URI> getWritableGraphs() {
		return unmodifiableSet(writableGraphs);
	}

	/**
	 * Adds a writable graph this module. This limits the writable and the
	 * readable scope to this and other writable graphs and causes any add
	 * operations to be added to these graphs.
	 * 
	 * @param graph
	 * @return this
	 */
	public KommaModule addWritableGraph(URI graph) {
		writableGraphs.add(graph);
		addReadableGraph(graph);
		return this;
	}

	/**
	 * Adds a readable graph this module. This limits the readable scope to this
	 * and other readable graphs.
	 * 
	 * @param graph
	 * @return this
	 */
	public KommaModule addReadableGraph(URI graph) {
		readableGraphs.add(graph);
		return this;
	}

	/**
	 * Include all the information from the given module in this module.
	 * 
	 * @param module
	 *            to be included
	 * @return this
	 */
	public KommaModule includeModule(KommaModule module) {
		annotations.putAll(module.annotations);
		datatypes.addAll(module.datatypes);
		concepts.addAll(module.concepts);
		behaviours.addAll(module.behaviours);
		readableGraphs.addAll(module.writableGraphs);
		readableGraphs.addAll(module.readableGraphs);
		if (!cl.equals(module.cl)) {
			cl.addAlternative(module.cl);
		}
		return this;
	}

	public Set<URI> getReadableGraphs() {
		return unmodifiableSet(readableGraphs);
	}

	public Collection<Association> getDatatypes() {
		return unmodifiableSet(datatypes);
	}

	/**
	 * Associates this datatype with the given uri within this factory.
	 * 
	 * @param type
	 *            serializable class
	 * @param datatype
	 *            URI
	 */
	public KommaModule addDatatype(Class<?> type, String uri) {
		datatypes.add(new Association(type, uri));
		return this;
	}

	public Collection<Association> getAnnotations() {
		return annotations.values();
	}

	/**
	 * Associates this annotation with its type.
	 * 
	 * @param annotation
	 *            annotation class
	 */
	public KommaModule addAnnotation(Class<?> annotation) {
		if (!annotation.isAnnotationPresent(Iri.class))
			throw new IllegalArgumentException("@Iri annotation required in "
					+ annotation.getSimpleName());
		String uri = annotation.getAnnotation(Iri.class).value();
		addAnnotation(annotation, uri);
		return this;
	}

	/**
	 * Associates the annotation concept with the given type.
	 * 
	 * @param annotation
	 *            annotation class
	 * @param type
	 *            URI
	 */
	public KommaModule addAnnotation(Class<?> annotation, String type) {
		Association registered = annotations.get(annotation);
		if (registered != null && !registered.getRdfType().equals(type)) {
			throw new IllegalArgumentException(
					"annotation is already associated to type "
							+ registered.getRdfType());
		}
		annotations.put(annotation, new Association(annotation, type));
		return this;
	}

	public Collection<Association> getConcepts() {
		return unmodifiableSet(concepts);
	}

	/**
	 * Associates this concept with its default subject type.
	 * 
	 * @param concept
	 *            interface or class
	 */
	public KommaModule addConcept(Class<?> concept) {
		concepts.add(new Association(concept, null));
		return this;
	}

	/**
	 * Associates this concept with the given subject type.
	 * 
	 * @param concept
	 *            interface or class
	 * @param type
	 *            URI
	 */
	public KommaModule addConcept(Class<?> concept, String type) {
		concepts.add(new Association(concept, type));
		return this;
	}

	public Collection<Association> getBehaviours() {
		return unmodifiableSet(behaviours);
	}

	/**
	 * Associates this behaviour with its default subject type.
	 * 
	 * @param behaviour
	 *            class
	 */
	public KommaModule addBehaviour(Class<?> behaviour) {
		behaviours.add(new Association(behaviour, null));
		return this;
	}

	/**
	 * Associates this behaviour with the given subject type.
	 * 
	 * @param behaviour
	 *            class
	 * @param type
	 *            URI
	 */
	public KommaModule addBehaviour(Class<?> behaviour, String type) {
		behaviours.add(new Association(behaviour, type));
		return this;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((annotations == null) ? 0 : annotations.hashCode());
		result = prime * result
				+ ((behaviours == null) ? 0 : behaviours.hashCode());
		result = prime * result + ((cl == null) ? 0 : cl.hashCode());
		result = prime * result
				+ ((concepts == null) ? 0 : concepts.hashCode());
		result = prime * result
				+ ((datatypes == null) ? 0 : datatypes.hashCode());
		result = prime * result
				+ ((readableGraphs == null) ? 0 : readableGraphs.hashCode());
		result = prime * result
				+ ((writableGraphs == null) ? 0 : writableGraphs.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		KommaModule other = (KommaModule) obj;
		if (annotations == null) {
			if (other.annotations != null)
				return false;
		} else if (!annotations.equals(other.annotations))
			return false;
		if (behaviours == null) {
			if (other.behaviours != null)
				return false;
		} else if (!behaviours.equals(other.behaviours))
			return false;
		if (cl == null) {
			if (other.cl != null)
				return false;
		} else if (!cl.equals(other.cl))
			return false;
		if (concepts == null) {
			if (other.concepts != null)
				return false;
		} else if (!concepts.equals(other.concepts))
			return false;
		if (datatypes == null) {
			if (other.datatypes != null)
				return false;
		} else if (!datatypes.equals(other.datatypes))
			return false;
		if (readableGraphs == null) {
			if (other.readableGraphs != null)
				return false;
		} else if (!readableGraphs.equals(other.readableGraphs))
			return false;
		if (writableGraphs == null) {
			if (other.writableGraphs != null)
				return false;
		} else if (!writableGraphs.equals(other.writableGraphs))
			return false;
		return true;
	}

	@Override
	public String toString() {
		if (!writableGraphs.isEmpty())
			return writableGraphs.toString();
		Set<Package> pkg = new LinkedHashSet<Package>();
		for (Association concept : concepts) {
			pkg.add(concept.getJavaClass().getPackage());
		}
		return pkg.toString();
	}
}
