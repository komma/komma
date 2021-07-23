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

import net.enilink.composition.mapping.IPropertyMapper;

import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

/**
 * Defines the scope of an {@link IEntityManager} and its factory. This includes
 * concepts, behaviours, literals and graphs.
 */
public class KommaModule {
	private static final String IRI_CLASS = "net.enilink.composition.annotations.Iri";
	private final CombinedClassLoader cl;
	private Map<Class<?>, Association> annotations = new HashMap<Class<?>, Association>();
	private Set<Association> datatypes = new HashSet<Association>();
	private Set<Association> concepts = new HashSet<Association>();
	private Set<Association> behaviours = new HashSet<Association>();
	private Set<URI> readableGraphs = new LinkedHashSet<URI>();
	private Set<URI> writableGraphs = new LinkedHashSet<URI>();
	private Set<INamespace> namespaces = new HashSet<INamespace>();
	private Map<Class<?>, IObjectMapper> objectMappers = new HashMap<>();
	private Map<Class<?>, IPropertyMapper> propertyMappers = new HashMap<>();
	private Map<String, ILiteralMapper> literalMappers = new HashMap<>();

	public KommaModule() {
		cl = new CombinedClassLoader(getClass().getClassLoader());
	}

	public KommaModule(ClassLoader classLoader) {
		cl = new CombinedClassLoader(classLoader);
		cl.addAlternative(getClass().getClassLoader());
	}

	/**
	 * Associates this annotation with its type.
	 *
	 * @param annotation
	 *            annotation class
	 */
	public KommaModule addAnnotation(Class<?> annotation) {
		String uri = null;
		for (Method m : annotation.getDeclaredMethods()) {
			for (Annotation methodAnnotation : m.getAnnotations()) {
				for (Class<?> itf : methodAnnotation.getClass().getInterfaces()) {
					if (itf.getName().equals(IRI_CLASS)) {
						try {
							uri = (String) methodAnnotation.getClass().getMethod("value").invoke(methodAnnotation);
						} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
								| NoSuchMethodException | SecurityException e) {
							throw new RuntimeException(e);
						}
						break;
					}
				}
			}
		}
		if (uri == null) {
			throw new IllegalArgumentException("@Iri annotation required on method of " + annotation.getSimpleName());
		}
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
			throw new IllegalArgumentException("annotation is already associated to type " + registered.getRdfType());
		}
		annotations.put(annotation, new Association(annotation, type));
		return this;
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

	/**
	 * Associates this datatype with the given uri within this factory.
	 *
	 * @param type
	 *            serializable class
	 * @param datatype
	 *            URI
	 */
	public KommaModule addDatatype(Class<?> type, String datatype) {
		datatypes.add(new Association(type, datatype));
		return this;
	}

	/**
	 * Adds a prefix for an URI.
	 *
	 * @param prefix
	 *            The prefix.
	 * @param uri
	 *            The URI that the prefix maps to.
	 */
	public KommaModule addNamespace(String prefix, URI uri) {
		namespaces.add(new Namespace(prefix, uri));
		return this;
	}

	/**
	 * Adds a namespace definition.
	 *
	 * @param namespace
	 *            The namespace definition.
	 */
	public KommaModule addNamespace(INamespace namespace) {
		namespaces.add(namespace);
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
	 * Adds an object mapper for the given concept class.
	 *
	 * @param concept the concept class
	 * @param objectMapper an object mapper
	 * @return this
	 */
	public KommaModule addObjectMapper(Class<?> concept, IObjectMapper objectMapper) {
		objectMappers.put(concept, objectMapper);
		return this;
	}

	/**
	 * Adds a property mapper for the given concept class.
	 *
	 * @param concept the concept class
	 * @param propertyMapper a property mapper
	 * @return this
	 */
	public KommaModule addPropertyMapper(Class<?> concept, IPropertyMapper propertyMapper) {
		propertyMappers.put(concept, propertyMapper);
		return this;
	}

	/**
	 * Adds a literal mapper for the given literal class.
	 *
	 * @param literalClass  the literal class
	 * @param literalMapper a literal mapper
	 * @return this
	 */
	public KommaModule addLiteralMapper(Class<?> literalClass, ILiteralMapper literalMapper) {
		literalMappers.put(literalClass.getName(), literalMapper);
		return this;
	}

	/**
	 * Adds a literal mapper for the given literal class.
	 * <p>
	 * The class is represented as a string to allow the implementation
	 * of converters without having the concrete classes or interfaces on the classpath.
	 * An example are Groovy strings that are implemented by the class
	 * <pre>org.codehaus.groovy.runtime.GStringImpl</pre>
	 *
	 * @param literalClassName the literal class name
	 * @param literalMapper    a literal mapper
	 * @return this
	 */
	public KommaModule addLiteralMapper(String literalClassName, ILiteralMapper literalMapper) {
		literalMappers.put(literalClassName, literalMapper);
		return this;
	}

	public Collection<Association> getAnnotations() {
		return annotations.values();
	}

	public Collection<Association> getBehaviours() {
		return unmodifiableSet(behaviours);
	}

	public synchronized ClassLoader getClassLoader() {
		return cl;
	}

	public Collection<Association> getConcepts() {
		return unmodifiableSet(concepts);
	}

	public Collection<Association> getDatatypes() {
		return unmodifiableSet(datatypes);
	}

	public Set<INamespace> getNamespaces() {
		return unmodifiableSet(namespaces);
	}

	public Set<URI> getReadableGraphs() {
		return unmodifiableSet(readableGraphs);
	}

	public Set<URI> getWritableGraphs() {
		return unmodifiableSet(writableGraphs);
	}

	public Map<Class<?>, IObjectMapper> getObjectMappers() { return unmodifiableMap(objectMappers); }

	public Map<Class<?>, IPropertyMapper> getPropertyMappers() { return unmodifiableMap(propertyMappers); }

	public Map<String, ILiteralMapper> getLiteralMappers() { return unmodifiableMap(literalMappers); }

	/**
	 * Include all the information from the given module in this module.
	 *
	 * @param module
	 *            to be included
	 * @return this
	 */
	public KommaModule includeModule(KommaModule module) {
		return includeModule(module, true);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof KommaModule)) return false;
		KommaModule that = (KommaModule) o;
		return Objects.equals(cl, that.cl) && Objects.equals(annotations, that.annotations) &&
				Objects.equals(datatypes, that.datatypes) && Objects.equals(concepts, that.concepts) &&
				Objects.equals(behaviours, that.behaviours) && Objects.equals(readableGraphs, that.readableGraphs) &&
				Objects.equals(writableGraphs, that.writableGraphs) && Objects.equals(namespaces, that.namespaces) &&
				Objects.equals(objectMappers, that.objectMappers) &&
				Objects.equals(propertyMappers, that.propertyMappers) &&
				Objects.equals(literalMappers, that.literalMappers);
	}

	@Override
	public int hashCode() {
		return Objects.hash(cl, annotations, datatypes, concepts, behaviours, readableGraphs,
				writableGraphs, namespaces, objectMappers, propertyMappers, literalMappers);
	}

	/**
	 * Include the information from the given module in this module, but allow
	 * for the graphs (read/write) to be ignored upon request.
	 *
	 * @param module
	 *            to be included
	 * @param includeGraphsAndNamespaces
	 *            flag to indicate whether to include the readable and writable graphs
	 *            as well as the namespaces
	 * @return this
	 */
	public KommaModule includeModule(KommaModule module, boolean includeGraphsAndNamespaces) {
		annotations.putAll(module.annotations);
		datatypes.addAll(module.datatypes);
		concepts.addAll(module.concepts);
		behaviours.addAll(module.behaviours);
		objectMappers.putAll(module.objectMappers);
		propertyMappers.putAll(module.propertyMappers);
		literalMappers.putAll(module.literalMappers);
		if (includeGraphsAndNamespaces) {
			writableGraphs.addAll(module.writableGraphs);
			readableGraphs.addAll(module.readableGraphs);
			namespaces.addAll(module.namespaces);
		}
		if (!cl.equals(module.cl)) {
			cl.addAlternative(module.cl);
		}
		return this;
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

	public static class Association {
		private Class<?> javaClass;

		private String rdfType;

		private Association(Class<?> javaClass, String rdfType) {
			this.javaClass = javaClass;
			this.rdfType = rdfType;
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

		public Class<?> getJavaClass() {
			return javaClass;
		}

		public String getRdfType() {
			return rdfType;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((javaClass == null) ? 0 : javaClass.hashCode());
			result = prime * result + ((rdfType == null) ? 0 : rdfType.hashCode());
			return result;
		}

		public String toString() {
			return javaClass.getName() + "=" + rdfType;
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
				alternatives.add(loader.getParent());
				if (((CombinedClassLoader) loader).alternatives != null) {
					alternatives.addAll(((CombinedClassLoader) loader).alternatives);
				}
			} else {
				alternatives.add(loader);
			}
			// do not include parent class loader in alternatives
			alternatives.remove(getParent());
		}

		@Override
		protected Class<?> findClass(String name) throws ClassNotFoundException {
			if (alternatives != null) {
				for (ClassLoader alt : alternatives) {
					try {
						return alt.loadClass(name);
					} catch (ClassNotFoundException e2) {
						// ignore and try next alternative class loader
					}
				}
			}
			throw new ClassNotFoundException(name);
		}

		@Override
		protected URL findResource(String name) {
			URL resource = null;
			if (alternatives != null) {
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
		protected Enumeration<URL> findResources(String name) throws IOException {
			if (alternatives != null) {
				Vector<URL> list = new Vector<URL>();
				for (ClassLoader alt : alternatives) {
					Enumeration<URL> e = alt.getResources(name);
					while (e.hasMoreElements()) {
						list.add(e.nextElement());
					}
				}
				return list.elements();
			}
			return Collections.emptyEnumeration();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((getParent() == null) ? 0 : getParent().hashCode());
			result = prime * result + ((alternatives == null) ? 0 : alternatives.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof CombinedClassLoader))
				return false;
			CombinedClassLoader other = (CombinedClassLoader) obj;
			if (getParent() == null) {
				if (other.getParent() != null)
					return false;
			} else if (!getParent().equals(other.getParent()))
				return false;
			if (alternatives == null) {
				if (other.alternatives != null)
					return false;
			} else if (!alternatives.equals(other.alternatives))
				return false;
			return true;
		}
	}
}
