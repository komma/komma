/*
 * Copyright (c) 2008, 2010, James Leigh All rights reserved.
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
package net.enilink.komma.generator;

import static java.lang.Character.toLowerCase;

import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.beans.FeatureDescriptor;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.enilink.composition.annotations.Iri;
import org.openrdf.rio.RDFHandlerException;

import com.google.inject.Inject;

import net.enilink.vocab.owl.OWL;
import net.enilink.vocab.rdf.RDF;
import net.enilink.vocab.rdfs.RDFS;
import net.enilink.vocab.xmlschema.XMLSCHEMA;
import net.enilink.komma.literals.LiteralConverter;
import net.enilink.komma.core.IEntityManager;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.core.Statement;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIImpl;
import net.enilink.komma.core.visitor.IDataVisitor;

/**
 * Reads BeanInfo objects and writes the class and properties into an OWL
 * ontology.
 * 
 */
public class OwlGenerator {
	@Inject
	private LiteralConverter lc;

	@Inject
	IEntityManager manager;

	private Map<String, String> namespaces = new HashMap<String, String>();

	private Map<String, String> prefixes = new HashMap<String, String>();

	private IDataVisitor<?> target;

	private IReference createList(Object[] objects) throws RDFHandlerException {
		IReference list = null, origin = null;
		for (Object obj : objects) {
			if (list == null) {
				origin = list = manager.create();
				handleStatement(list, RDF.PROPERTY_TYPE, RDF.TYPE_LIST);
			} else {
				IReference rest = manager.create();
				handleStatement(list, RDF.PROPERTY_REST, rest);
				list = rest;
				handleStatement(list, RDF.PROPERTY_TYPE, RDF.TYPE_LIST);
			}
			handleStatement(list, RDF.PROPERTY_FIRST, createURI(obj));
		}
		if (list != null) {
			handleStatement(list, RDF.PROPERTY_REST, RDF.NIL);
		}
		return origin;
	}

	private IReference createList(String[] labels, URI datatype)
			throws RDFHandlerException {
		IReference list = null, origin = null;
		for (String label : labels) {
			if (list == null) {
				origin = list = manager.create();
				handleStatement(list, RDF.PROPERTY_TYPE, RDF.TYPE_LIST);
			} else {
				IReference rest = manager.create();
				handleStatement(list, RDF.PROPERTY_REST, rest);
				list = rest;
				handleStatement(list, RDF.PROPERTY_TYPE, RDF.TYPE_LIST);
			}
			handleStatement(list, RDF.PROPERTY_FIRST,
					manager.createLiteral(label, datatype, null));
		}
		if (list != null) {
			handleStatement(list, RDF.PROPERTY_REST, RDF.NIL);
		}
		return origin;
	}

	private URI createURI(Class<?> beanClass) {
		if (beanClass.isAnnotationPresent(Iri.class)) {
			String value = beanClass.getAnnotation(Iri.class).value();
			return URIImpl.createURI(value);
		}
		String ns = findNamespace(beanClass);
		if (ns == null)
			return URIImpl.createURI("java:" + beanClass.getName());
		String localName = beanClass.getSimpleName();
		return URIImpl.createURI(ns + localName);
	}

	private URI createURI(Object obj) {
		if (obj instanceof Class<?>)
			return createURI((Class<?>) obj);
		return URIImpl.createURI(obj.toString());
	}

	public Set<URI> exportOntology(List<Class<?>> beans, IDataVisitor<?> handler)
			throws IntrospectionException, RDFHandlerException {
		if (beans.isEmpty())
			throw new IllegalArgumentException();
		target = handler;
		Set<URI> set = new HashSet<URI>();
		for (Class<?> bean : beans) {
			BeanInfo info = Introspector.getBeanInfo(bean);
			set.add(handleBeanClass(info.getBeanDescriptor()));
		}
		for (Class<?> bean : beans) {
			BeanInfo info = Introspector.getBeanInfo(bean);
			URI domain = createURI(info.getBeanDescriptor().getBeanClass());
			for (PropertyDescriptor desc : info.getPropertyDescriptors()) {
				if (desc.getReadMethod() != null
						&& !desc.getReadMethod().getDeclaringClass()
								.equals(Object.class)) {
					handleBeanProperty(domain, desc);
				}
			}
		}
		return set;
	}

	private String findNamespace(Class<?> beanClass) {
		String packageName = getPackageName(beanClass);
		if (namespaces.containsKey(packageName))
			return namespaces.get(packageName);
		Package pkg = beanClass.getPackage();
		if (pkg != null && pkg.isAnnotationPresent(Iri.class)) {
			String name = pkg.getAnnotation(Iri.class).value();
			return getNamespace(name);
		}
		return null;
	}

	private URI getDatatype(Class<?> range) {
		if (range.equals(Object.class))
			return RDFS.TYPE_RESOURCE;
		if (range.equals(String.class))
			return XMLSCHEMA.TYPE_STRING;
		try {
			net.enilink.komma.core.URI datatype = lc
					.findDatatype(range);
			if (datatype.scheme().equals("java")) {
				return null;
			}
			return datatype;
		} catch (KommaException e) {
			return null;
		}
	}

	private String getNamespace(Class<?> beanClass) {
		String packageName = getPackageName(beanClass);
		String ns = findNamespace(beanClass);
		if (ns != null)
			return ns;
		return "java:" + packageName + '#';
	}

	private String getNamespace(String uri) {
		String ns;
		if (uri.endsWith("/") || uri.endsWith("#")) {
			ns = uri;
		} else if (uri.contains("#")) {
			ns = uri.substring(0, uri.indexOf('#') + 1);
		} else {
			ns = uri + '#';
		}
		return ns;
	}

	private String getPackageName(Class<?> beanClass) {
		if (beanClass.getPackage() != null)
			return beanClass.getPackage().getName();
		String name = beanClass.getName();
		if (name.contains("."))
			return name.substring(0, name.indexOf('.'));
		return "";
	}

	private Class<?> getPropertyType(PropertyDescriptor desc) {
		Class<?> range = desc.getPropertyType();
		if (range.isPrimitive()) {
			if (range.equals(Character.TYPE))
				return Character.class;
			if (range.equals(Byte.TYPE))
				return Byte.class;
			if (range.equals(Short.TYPE))
				return Short.class;
			if (range.equals(Integer.TYPE))
				return Integer.class;
			if (range.equals(Long.TYPE))
				return Long.class;
			if (range.equals(Float.TYPE))
				return Float.class;
			if (range.equals(Double.TYPE))
				return Double.class;
			if (range.equals(Boolean.TYPE))
				return Boolean.class;
		}
		return range;
	}

	private URI handleBeanClass(BeanDescriptor desc) throws RDFHandlerException {
		Class<?> beanClass = desc.getBeanClass();
		URI uri = createURI(beanClass);
		if (beanClass.isAnnotationPresent(Deprecated.class)) {
			handleStatement(uri, RDF.PROPERTY_TYPE, OWL.TYPE_DEPRECATEDCLASS);
		} else {
			handleStatement(uri, RDF.PROPERTY_TYPE, OWL.TYPE_CLASS);
		}
		handleLabel(uri, desc);
		URI ns = URIImpl.createURI(getNamespace(beanClass));
		handleStatement(uri, RDFS.PROPERTY_ISDEFINEDBY, ns);
		Class<?> sup = beanClass.getSuperclass();
		if (sup != null && !sup.equals(Object.class))
			handleStatement(uri, RDFS.PROPERTY_SUBCLASSOF, createURI(sup));
		for (Class<?> face : beanClass.getInterfaces()) {
			handleStatement(uri, RDFS.PROPERTY_SUBCLASSOF, createURI(face));
		}
		// if (beanClass.isAnnotationPresent(rdf.class)) {
		// String[] eq = beanClass.getAnnotation(rdf.class).value();
		// for (int i = 1; i < eq.length; i++) {
		// handleStatement(uri, OWL.EQUIVALENTCLASS, uriFactory
		// .createURI(eq[i]));
		// }
		// }
		// if (beanClass.isAnnotationPresent(disjointWith.class)) {
		// disjointWith dw = beanClass.getAnnotation(disjointWith.class);
		// for (Class<?> c : dw.value()) {
		// handleStatement(uri, OWL.DISJOINTWITH, createURI(c));
		// }
		// }
		// if (beanClass.isAnnotationPresent(intersectionOf.class)) {
		// intersectionOf io = beanClass.getAnnotation(intersectionOf.class);
		// BNode list = createList(io.value());
		// handleStatement(uri, OWL.INTERSECTIONOF, list);
		// }
		// if (beanClass.isAnnotationPresent(complementOf.class)) {
		// complementOf co = beanClass.getAnnotation(complementOf.class);
		// handleStatement(uri, OWL.COMPLEMENTOF, createURI(co.value()));
		// }
		// if (beanClass.isAnnotationPresent(oneOf.class)) {
		// oneOf one = beanClass.getAnnotation(oneOf.class);
		// BNode list = bnodeFactory.createBNode();
		// handleStatement(uri, OWL.ONEOF, list);
		// for (int i = 0, n = one.value().length; i < n; i++) {
		// String of = one.value()[i];
		// handleStatement(list, RDF.FIRST, uriFactory.createURI(of));
		// if (i < n - 1) {
		// BNode rest = bnodeFactory.createBNode();
		// handleStatement(list, RDF.REST, rest);
		// list = rest;
		// } else {
		// handleStatement(list, RDF.REST, RDF.NIL);
		// }
		// }
		// }
		return ns;
	}

	private void handleBeanProperty(URI domain, PropertyDescriptor desc)
			throws RDFHandlerException {
		URI uri;
		if (desc.getReadMethod().isAnnotationPresent(Iri.class)) {
			Iri ann = desc.getReadMethod().getAnnotation(Iri.class);
			uri = URIImpl.createURI(ann.value());
		} else {
			uri = domain.appendLocalPart(desc.getName());
		}
		URI ns = URIImpl.createURI(getNamespace(desc.getReadMethod()
				.getDeclaringClass()));
		Class<?> range = getPropertyType(desc);
		if (range.equals(Set.class)) {
			range = Object.class;
			Type t = desc.getReadMethod().getGenericReturnType();
			if (t instanceof ParameterizedType) {
				ParameterizedType pt = (ParameterizedType) t;
				Type[] args = pt.getActualTypeArguments();
				if (args.length == 1 && args[0] instanceof Class<?>)
					range = (Class<?>) args[0];
			}
		}
		URI datatype = getDatatype(range);
		if (datatype == null) {
			handleStatement(uri, RDF.PROPERTY_TYPE, OWL.TYPE_OBJECTPROPERTY);
			handleStatement(uri, RDFS.PROPERTY_RANGE, createURI(range));
		} else if (datatype.equals(RDFS.TYPE_RESOURCE)) {
			handleStatement(uri, RDF.PROPERTY_TYPE, RDF.TYPE_PROPERTY);
			handleStatement(uri, RDFS.PROPERTY_RANGE, datatype);
		} else {
			handleStatement(uri, RDF.PROPERTY_TYPE, OWL.TYPE_DATATYPEPROPERTY);
			handleStatement(uri, RDFS.PROPERTY_RANGE, datatype);
		}
		if (desc.getReadMethod().isAnnotationPresent(Deprecated.class)) {
			handleStatement(uri, RDF.PROPERTY_TYPE, OWL.TYPE_DEPRECATEDPROPERTY);
		}
		handleStatement(uri, RDFS.PROPERTY_DOMAIN, domain);
		if (!desc.getPropertyType().equals(Set.class)) {
			handleStatement(uri, RDF.PROPERTY_TYPE, OWL.TYPE_FUNCTIONALPROPERTY);
		}
		handleLabel(uri, desc);
		handleStatement(uri, RDFS.PROPERTY_ISDEFINEDBY, ns);
		// Method getter = desc.getReadMethod();
		// if (getter.isAnnotationPresent(rdf.class)) {
		// String[] eq = getter.getAnnotation(rdf.class).value();
		// for (int i = 1; i < eq.length; i++) {
		// handleStatement(uri, RDFS.SUBPROPERTYOF, uriFactory
		// .createURI(eq[i]));
		// }
		// }
		// if (getter.isAnnotationPresent(inverseOf.class)) {
		// inverseOf io = getter.getAnnotation(inverseOf.class);
		// for (String inv : io.value()) {
		// handleStatement(uri, OWL.INVERSEOF, uriFactory.createURI(inv));
		// }
		// }
		// Method setter = desc.getWriteMethod();
		// if (setter != null && setter.isAnnotationPresent(oneOf.class)) {
		// oneOf oo = setter.getAnnotation(oneOf.class);
		// String dt = oo.datatype();
		// BNode list = createList(oo.label(), dt);
		// if (list != null) {
		// handleStatement(uri, OWL.ONEOF, list);
		// }
		// list = createList(oo.value());
		// if (list != null) {
		// handleStatement(uri, OWL.ONEOF, list);
		// }
		// }
	}

	private void handleLabel(URI uri, FeatureDescriptor desc)
			throws RDFHandlerException {
		String prefix = prefixes.get(uri.namespace().toString());
		String label = removePrefix(prefix, desc.getDisplayName());
		String comment = removePrefix(prefix, desc.getShortDescription());
		handleStatement(uri, RDFS.PROPERTY_LABEL, label);
		if (!label.equals(comment)) {
			handleStatement(uri, RDFS.PROPERTY_COMMENT, comment);
		}
	}

	private void handleStatement(IReference subj, URI pred, Object obj) {
		target.visitStatement(new Statement(subj, pred, obj));
	}

	private String removePrefix(String prefix, String label) {
		if (prefix == null)
			return label;
		int l = prefix.length();
		if (label.startsWith(prefix) && label.length() > l) {
			label = toLowerCase(label.charAt(l)) + label.substring(l + 1);
		}
		return label;
	}

	public void setNamespace(String pkgName, String prefix, String namespace) {
		namespaces.put(pkgName, namespace);
		prefixes.put(namespace, prefix);
	}

}
