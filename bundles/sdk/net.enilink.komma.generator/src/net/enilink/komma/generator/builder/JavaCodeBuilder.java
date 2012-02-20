/*
 * Copyright (c) 2008, 2010, Zepheira All rights reserved.
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
package net.enilink.komma.generator.builder;

import static java.util.Collections.singletonList;

import java.io.FileNotFoundException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import net.enilink.composition.annotations.Iri;

import net.enilink.vocab.owl.DataRange;
import net.enilink.vocab.owl.DeprecatedClass;
import net.enilink.vocab.owl.DeprecatedProperty;
import net.enilink.vocab.owl.OwlProperty;
import net.enilink.vocab.owl.Restriction;
import net.enilink.vocab.owl.SymmetricProperty;
import net.enilink.vocab.owl.Thing;
import net.enilink.vocab.rdf.Property;
import net.enilink.vocab.rdfs.Class;
import net.enilink.vocab.rdfs.Resource;
import net.enilink.komma.generator.JavaNameResolver;
import net.enilink.komma.generator.concepts.CodeClass;
import net.enilink.komma.generator.concepts.CodeOntology;
import net.enilink.komma.generator.concepts.CodeProperty;
import net.enilink.komma.generator.source.JavaClassBuilder;
import net.enilink.komma.generator.source.JavaCommentBuilder;
import net.enilink.komma.generator.source.JavaMethodBuilder;
import net.enilink.komma.generator.source.JavaPropertyBuilder;
import net.enilink.komma.generator.source.JavaSourceBuilder;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIImpl;

public class JavaCodeBuilder {
	private static final String OWL = "http://www.w3.org/2002/07/owl#";
	private static final String RDFS = "http://www.w3.org/2000/01/rdf-schema#";
	private static final QName NOTHING = new QName(OWL, "Nothing");
	private static final QName RESOURCE = new QName(RDFS, "Resource");
	private static final QName LITERAL = new QName(RDFS, "Literal");
	private JavaClassBuilder out;
	private JavaNameResolver resolver;

	public JavaCodeBuilder(JavaClassBuilder builder, JavaNameResolver resolver)
			throws FileNotFoundException {
		this.out = builder;
		this.resolver = resolver;
	}

	public void close() {
		out.close();
	}

	public void packageInfo(CodeOntology ontology, String namespace) {
		comment(out, ontology);
		out.annotateStrings(Iri.class, singletonList(namespace));
		out.pkg(resolver.getPackageName(URIImpl.createURI(namespace)));
	}

	public void interfaceHeader(CodeClass concept) {
		String pkg = resolver.getPackageName(concept.getURI());
		String simple = resolver.getSimpleName(concept.getURI());
		if (pkg != null) {
			out.pkg(pkg);
		}
		comment(out, concept);
		if (concept instanceof DeprecatedClass) {
			out.annotate(Deprecated.class);
		}
		List<URI> list = new ArrayList<URI>();
		URI type = resolver.getType(concept.getURI());
		if (type != null) {
			list.add(type);
		}
		for (Class eq : concept.getOwlEquivalentClasses()) {
			type = resolver.getType(eq.getURI());
			if (type != null) {
				list.add(type);
			}
		}
		out.annotateURIs(Iri.class, list);
		List<URI> oneOf = new ArrayList<URI>();
		if (concept.getOwlOneOf() != null) {
			for (Object o : concept.getOwlOneOf()) {
				if (o instanceof IEntity) {
					oneOf.add(((IEntity) o).getURI());
				}
			}
		}

		// out.annotateQNames(oneOf.class, oneOf);
		// annotate(intersectionOf.class, concept.getOwlIntersectionOf());
		// annotate(complementOf.class, concept.getOwlComplementOf());
		// annotate(disjointWith.class, concept.getOwlDisjointWith());

		out.interfaceName(simple);
		for (Class sups : concept.getRdfsSubClassOf()) {
			if (sups.getURI() == null || sups.equals(concept))
				continue;
			out.extend(resolver.getClassName(sups.getURI()));
		}
	}

	public void classHeader(CodeClass datatype) {
		String pkg = resolver.getPackageName(datatype.getURI());
		String simple = resolver.getSimpleName(datatype.getURI());
		if (pkg != null) {
			out.pkg(pkg);
		}
		comment(out, datatype);
		URI type = resolver.getType(datatype.getURI());
		out.annotateURI(Iri.class, type);
		out.className(simple);
		for (Class sups : datatype.getRdfsSubClassOf()) {
			if (sups.getURI() == null || sups.equals(datatype))
				continue;
			out.extend(resolver.getClassName(sups.getURI()));
		}
	}

	public JavaCodeBuilder constants(CodeClass concept) {
		List<Object> oneOf = concept.getOwlOneOf();
		if (oneOf != null) {
			List<String> names = new ArrayList<String>(oneOf.size());
			for (Object one : oneOf) {
				if (one instanceof IEntity) {
					URI uri = ((IEntity) one).getURI();
					String localPart = uri.localPart();
					String name = localPart.replaceAll("^[^a-zA-Z]", "_")
							.replaceAll("\\W", "_").toUpperCase();
					names.add(name);
					out.staticURIField(name, uri);
				}
			}
			if (!names.isEmpty()) {
				out.staticURIArrayField("URIS", names);
			}
		}
		return this;
	}

	public JavaCodeBuilder stringConstructor(CodeClass datatype) {
		String cn = resolver.getClassName(datatype.getURI());
		String simple = resolver.getSimpleName(datatype.getURI());
		JavaMethodBuilder method = out.staticMethod("valueOf");
		method.returnType(cn);
		method.param(String.class.getName(), "value");
		method.code("return new ").code(simple).code("(value);").end();
		JavaMethodBuilder code = out.constructor();
		code.param(String.class.getName(), "value");
		code.code("super(value);").end();
		return this;
	}

	public JavaCodeBuilder property(CodeClass dec, Property property) {
		JavaPropertyBuilder prop = out.property(getPropertyName(dec, property));
		comment(prop, property);

		if (property instanceof DeprecatedProperty) {
			prop.annotate(Deprecated.class);
		}
		List<URI> list = new ArrayList<URI>();
		URI type = resolver.getType(property.getURI());
		if (type != null) {
			list.add(type);
		}
		if (property instanceof OwlProperty) {
			OwlProperty p = (OwlProperty) property;
			for (Property eq : p.getOwlEquivalentProperties()) {
				type = resolver.getType(eq.getURI());
				if (type != null) {
					list.add(type);
				}
			}
		}
		prop.annotateURIs(Iri.class, list);
		list.clear();
		if (property instanceof SymmetricProperty) {
			type = resolver.getType(property.getURI());
			if (type != null) {
				list.add(type);
			}
		}
		if (property instanceof CodeProperty) {
			CodeProperty p = (CodeProperty) property;
			for (Property eq : p.findAllInverseOfProperties()) {
				type = resolver.getType(eq.getURI());
				if (type != null) {
					list.add(type);
				}
			}
		}
		// prop.annotateQNames(inverseOf.class, list);
		String className = getRangeClassName(dec, property);
		if (dec.isFunctional(property)) {
			prop.type(className);
		} else {
			prop.setOf(className);
		}
		prop.getter();
		comment(prop, property);
		prop.end();
		return this;
	}

	private void comment(JavaSourceBuilder out, Resource concept) {
		JavaCommentBuilder comment = out.comment(concept.getRdfsComment());
		for (Object see : concept.getRdfsSeeAlso()) {
			if (see instanceof Class) {
				URI name = ((Class) see).getURI();
				comment.seeAlso(resolver.getClassName(name));
			} else if (see instanceof Property) {
				Property property = (Property) see;
				for (Class domain : property.getRdfsDomains()) {
					CodeClass cc = (CodeClass) domain;
					String cn = resolver.getClassName(domain.getURI());
					String name = getPropertyName(cc, property);
					String range = getRangeClassName(cc, property);
					if ("boolean".equals(range)) {
						comment.seeBooleanProperty(cn, name);
					} else {
						comment.seeProperty(cn, name);
					}
				}
			} else {
				comment.seeAlso(see.toString());
			}
		}
		if (concept instanceof Thing) {
			for (Object version : ((Thing) concept).getOwlVersionInfo()) {
				comment.version(version.toString());
			}
		}
		comment.generated();
		comment.end();
	}

	private String getPropertyName(CodeClass code, Property param) {
		String propertyName;
		if (code.isFunctional(param)) {
			propertyName = resolver.getPropertyName(param.getURI());
		} else {
			propertyName = resolver.getPluralPropertyName(param.getURI());
		}
		// this ensures that getters and setters are renamed if the property has
		// a different range as for its super classes
		Collection<Class> overriddenRanges = code.getOverriddenRanges(param);
		Class range = code.getRange(param);
		if (range != null) {
			overriddenRanges.remove(range);
		}
		if (range != null && range.getURI() != null
				&& !overriddenRanges.isEmpty()) {
			propertyName += "As" + resolver.getSimpleName(range.getURI());
		}
		return propertyName;
	}

	private String getRangeClassName(CodeClass code, Property property) {
		CodeClass range = code.getRange(property);
		if (range == null) {
			return Object.class.getName();
		}
		String type = null;
		if (range instanceof DataRange) {
			// TODO support XML Schema facets
			for (Object data : range.getOwlOneOf()) {
				type = data.getClass().getName();
			}
		} else if (NOTHING.equals(range.getURI())) {
			return "void";
		} else if (LITERAL.equals(range.getURI())) {
			return Object.class.getName();
		} else if (RESOURCE.equals(range.getURI())) {
			return Object.class.getName();
		} else if (range.getURI() != null) {
			type = resolver.getClassName(range.getURI());
		} else {
			return Object.class.getName();
		}
		BigInteger one = BigInteger.valueOf(1);
		for (Class c : code.getRdfsSubClassOf()) {
			if (c instanceof Restriction) {
				Restriction r = (Restriction) c;
				if (property.equals(r.getOwlOnProperty())) {
					if (one.equals(r.getOwlMaxCardinality())
							&& one.equals(r.getOwlMinCardinality())
							|| one.equals(r.getOwlCardinality())) {
						type = unwrap(type);
					}
				}
			}
		}
		return type;
	}

	private String unwrap(String type) {
		if (type.equals("java.lang.Character"))
			return "char";
		if (type.equals("java.lang.Byte"))
			return "byte";
		if (type.equals("java.lang.Short"))
			return "short";
		if (type.equals("java.lang.Integer"))
			return "int";
		if (type.equals("java.lang.Long"))
			return "long";
		if (type.equals("java.lang.Float"))
			return "float";
		if (type.equals("java.lang.Double"))
			return "double";
		if (type.equals("java.lang.Boolean"))
			return "boolean";
		return type;
	}

}
