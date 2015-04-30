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
package net.enilink.komma.generator;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IEntityManager;
import net.enilink.komma.core.ILiteral;
import net.enilink.komma.core.INamespace;
import net.enilink.komma.core.IQuery;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.IValue;
import net.enilink.komma.core.Statement;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIs;
import net.enilink.komma.em.concepts.IResource;
import net.enilink.vocab.owl.Class;
import net.enilink.vocab.owl.OWL;
import net.enilink.vocab.owl.Ontology;
import net.enilink.vocab.owl.Thing;
import net.enilink.vocab.rdf.Property;
import net.enilink.vocab.rdf.RDF;
import net.enilink.vocab.rdfs.Datatype;
import net.enilink.vocab.rdfs.RDFS;
import net.enilink.vocab.xmlschema.XMLSCHEMA;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

/**
 * Applies a series of rules against the ontology, making it easier to convert
 * into Java classes. This includes applying some OWL reasoning on properties,
 * renaming anonymous and foreign classes.
 * 
 */
public class OwlNormalizer {
	private final Logger log = LoggerFactory.getLogger(OwlNormalizer.class);

	private static final String PREFIX = "PREFIX rdf: <" + RDF.NAMESPACE
			+ "> PREFIX rdfs: <" + RDFS.NAMESPACE + "> PREFIX owl: <"
			+ OWL.NAMESPACE + "> ";

	private static final String FIND_PROPERTIES_WO_DOMAIN = PREFIX
			+ "SELECT ?prop "
			+ "WHERE {  { ?prop a rdf:Property } "
			+ "UNION { ?prop a owl:ObjectProperty } UNION { ?prop a owl:DatatypeProperty } "
			+ "UNION { ?prop a owl:DeprecatedProperty } UNION { ?prop a owl:AnnotationProperty } "
			+ "OPTIONAL { ?prop rdfs:domain ?domain } "
			+ "OPTIONAL { ?restriction owl:onProperty ?prop }"
			+ "FILTER (!(bound(?domain) || bound(?restriction))) }";

	private static final String SELECT_DEFINED = PREFIX + "SELECT ?bean "
			+ "WHERE { ?bean rdfs:isDefinedBy ?ont "
			+ "FILTER ( ?bean != ?ont ) } ";

	private static final String FIND_ORPHANS_ONTOLOGY = PREFIX
			+ "SELECT DISTINCT ?bean ?ontology"
			+ " WHERE { GRAPH ?graph { ?bean a ?type. "
			+ " ?ontology a owl:Ontology "
			+ " OPTIONAL { ?bean rdfs:isDefinedBy ?ont }"
			+ " FILTER ( isURI(?bean) && ! bound(?ont) ) }}";

	private static final String SELECT_ORPHANS = PREFIX
			+ "SELECT DISTINCT ?bean WHERE { ?bean rdf:type ?type"
			+ " OPTIONAL { ?bean rdfs:isDefinedBy ?ont }"
			+ " FILTER ( isURI(?bean) && ! bound(?ont) ) }";

	private static final String BEAN_DEFINED_BY = PREFIX
			+ "SELECT ?bean WHERE { ?bean rdfs:isDefinedBy ?ont }";

	private static final String SELECT_C_INTERSECTION_OF = PREFIX
			+ "SELECT ?c WHERE { ?c owl:intersectionOf ?l }";

	private static final String SELECT_C_ONE_OF = PREFIX
			+ "SELECT ?c WHERE { ?c owl:oneOf ?l }";

	private IEntityManager manager;

	private Set<IReference> anonymousClasses = new HashSet<IReference>();

	private Map<URI, URI> aliases = new HashMap<URI, URI>();

	private Map<URI, Ontology> ontologies;

	private Set<String> commonNS = new HashSet<String>(Arrays.asList(
			RDF.NAMESPACE, RDFS.NAMESPACE, OWL.NAMESPACE));

	private static final Pattern NS_PREFIX = Pattern
			.compile("^.*[/#](\\w+)[/#]?$");

	private void addBaseClass(Class base, java.lang.Class<Class> type) {
		IQuery<?> query = manager.createQuery(BEAN_DEFINED_BY);
		for (Object ont : base.getRdfsIsDefinedBy()) {
			query.setParameter("ont", ont);
			for (Object o : query.getResultList()) {
				if (o instanceof Class && !o.equals(base)) {
					Class c = (Class) o;
					boolean isBase = true;
					for (net.enilink.vocab.rdfs.Class e : c
							.getRdfsSubClassOf()) {
						if (e.getRdfsIsDefinedBy().contains(ont)) {
							isBase = false;
						}
					}
					if (isBase) {
						log.debug("extending " + c + " " + base);
						c.getRdfsSubClassOf().add(base);
					}
				}
			}
		}
	}

	private void assignOrphansToNewOntology(Map<URI, Ontology> ontologies) {
		IExtendedIterator<IResource> results = manager.createQuery(
				SELECT_ORPHANS).evaluate(IResource.class);

		try {
			while (results.hasNext()) {
				IResource bean = results.next();

				URI ns = bean.getURI().namespace();
				Ontology ont = findOntology(ns, ontologies);
				log.debug("assigning " + bean + " " + ont);
				bean.getRdfsIsDefinedBy().add(ont);
			}
		} finally {
			results.close();
		}
	}

	private void assignOrphansToTheirOntology(Map<URI, Ontology> ontologies) {
		IExtendedIterator<Object[]> results = manager.createQuery(
				FIND_ORPHANS_ONTOLOGY).evaluate(Object[].class);
		try {
			while (results.hasNext()) {
				Object[] result = results.next();

				IResource bean = (IResource) result[0];
				if (bean.getRdfsIsDefinedBy().isEmpty()) {
					IReference ontology = (IReference) result[1];
					log.debug("assigning " + bean + " " + ontology);
					bean.getRdfsIsDefinedBy().add(ontology);
				}
			}
		} finally {
			results.close();
		}
	}

	private void checkNamespacePrefixes() throws Exception {
		IExtendedIterator<IReference> results = manager.createQuery(
				SELECT_DEFINED).evaluate(IReference.class);
		try {
			while (results.hasNext()) {
				IReference bean = results.next();
				if (bean.getURI() == null)
					continue;
				URI ns = bean.getURI().namespace();
				String prefix = getPrefix(ns);
				if (prefix == null) {
					Matcher matcher = NS_PREFIX.matcher(ns.toString());
					if (matcher.find()) {
						prefix = matcher.group(1);
						log.debug("creating prefix " + prefix + " " + ns);
						manager.setNamespace(prefix, ns);
					}
				}
			}
		} finally {
			results.close();
		}
	}

	private void checkPropertyDomains() {
		IQuery<?> query = manager.createQuery(FIND_PROPERTIES_WO_DOMAIN);
		java.util.List<?> list = query.getResultList();
		for (Object result : list) {
			Property p = (Property) result;

			boolean found = false;
			for (Property sup : p.getRdfsSubPropertyOf()) {
				if (!sup.getRdfsDomains().isEmpty()) {
					found = true;
					p.getRdfsDomains().addAll(sup.getRdfsDomains());
				}
			}
			if (!found) {
				Class res = manager.createNamed(
						net.enilink.vocab.rdfs.RDFS.TYPE_RESOURCE,
						Class.class);
				p.getRdfsDomains().add(res);
			}
		}
	}

	private URI createLocalClass(URI obj, Ontology ont) {
		String localName = obj.localPart();
		String prefix = findPrefix(ont);
		if (prefix != null) {
			localName = initcap(prefix) + initcap(localName);
		}
		URI nc = URIs.createURI(findNamespace(ont)).appendLocalPart(
				localName);
		aliases.put(nc, obj);
		if (obj.equals(RDFS.TYPE_RESOURCE)) {
			Class base = manager.createNamed(nc, Class.class);
			base.getRdfsIsDefinedBy().add(ont);
			addBaseClass(base, Class.class);
		}
		return nc;
	}

	private void distributeEquivalentClasses() {
		for (Class klass : manager.findAll(Class.class)) {
			for (Class equiv : klass.getOwlEquivalentClasses()) {
				klass.getOwlEquivalentClasses().addAll(
						equiv.getOwlEquivalentClasses());
			}
			klass.getOwlEquivalentClasses().remove(klass);
		}
		for (Class c : manager.findAll(Class.class)) {
			for (Class e : c.getOwlEquivalentClasses()) {
				c.getOwlDisjointWith().addAll(e.getOwlDisjointWith());
				List<Class> inter = e.getOwlIntersectionOf();
				if (inter != null) {
					if (c.getOwlIntersectionOf() == null) {
						c.setOwlIntersectionOf(inter);
					} else if (!inter.equals(c.getOwlIntersectionOf())) {
						java.util.List<Class> list = new ArrayList<Class>(inter);
						list.removeAll(c.getOwlIntersectionOf());
						c.getOwlIntersectionOf().addAll(list);
					}
				}
				if (e.getOwlOneOf() != null) {
					if (c.getOwlOneOf() == null) {
						c.setOwlOneOf(e.getOwlOneOf());
					} else if (!e.getOwlOneOf().equals(c.getOwlOneOf())) {
						java.util.List<Object> list = new ArrayList<Object>(
								e.getOwlOneOf());
						list.removeAll(c.getOwlOneOf());
						c.getOwlOneOf().addAll(list);
					}
				}
				if (e.getOwlUnionOf() != null) {
					if (c.getOwlUnionOf() == null) {
						c.setOwlUnionOf(e.getOwlUnionOf());
					} else if (!e.getOwlUnionOf().equals(c.getOwlUnionOf())) {
						java.util.List<Class> list = new ArrayList<Class>(
								e.getOwlUnionOf());
						list.removeAll(c.getOwlUnionOf());
						c.getOwlUnionOf().addAll(list);
					}
				}
				if (e.getOwlComplementOf() != null) {
					if (c.getOwlComplementOf() == null) {
						c.setOwlComplementOf(e.getOwlComplementOf());
					}
				}
				if (!e.getOwlDisjointWith().isEmpty()) {
					c.getOwlDisjointWith().addAll(e.getOwlDisjointWith());
				}
			}
		}
	}

	private Set<IReference> findClasses(Collection<IReference> classes) {
		Set<IReference> set = new HashSet<IReference>(classes);
		for (IReference c : classes) {
			IExtendedIterator<IStatement> stmts = manager.match(null,
					RDFS.PROPERTY_SUBCLASSOF, c);
			try {
				while (stmts.hasNext()) {
					IStatement stmt = stmts.next();
					IReference subj = stmt.getSubject();
					set.add(subj);
				}
			} finally {
				stmts.close();
			}
		}
		if (set.size() > classes.size()) {
			return findClasses(set);
		} else {
			return set;
		}
	}

	private Class findCommon(
			Collection<? extends net.enilink.vocab.rdfs.Class> common,
			List<? extends IEntity> unionOf) {
		Class result = null;
		for (IEntity e : unionOf) {
			if (common.contains(e)) {
				result = (Class) e;
			}
		}
		return result;
	}

	private Collection<? extends net.enilink.vocab.rdfs.Class> findCommonSupers(
			java.util.List<?> unionOf) {
		Set<net.enilink.vocab.rdfs.Class> common = null;
		for (Object of : unionOf) {
			Set<net.enilink.vocab.rdfs.Class> supers = findSuperClasses((Class) of);
			if (common == null) {
				common = new HashSet<net.enilink.vocab.rdfs.Class>(
						supers);
			} else {
				common.retainAll(supers);
			}
		}
		if (common == null)
			return Collections.emptySet();
		Iterator<? extends IEntity> iter = common.iterator();
		while (iter.hasNext()) {
			if (iter.next().getURI() == null) {
				iter.remove();
			}
		}
		return common;
	}

	private String findNamespace(Ontology ont) {
		String prefix = findPrefix(ont);
		if (prefix != null) {
			String ns = manager.getNamespace(prefix).toString();
			if (ns.endsWith("#") || ns.endsWith("/") || ns.endsWith(":"))
				return ns;
			if (ns.contains("#"))
				return ns.substring(0, ns.indexOf('#') + 1);
			return ns + "#";
		}
		String ns = ont.toString();
		if (ns.contains("#"))
			return ns.substring(0, ns.indexOf('#') + 1);
		return ont.toString() + '#';
	}

	private void findNamespacesOfOntologies(Map<URI, Ontology> ontologies) {
		IQuery<?> query = manager.createQuery(BEAN_DEFINED_BY);
		for (Ontology ont : manager.findAll(Ontology.class)) {
			log.debug("found ontology " + ont);
			ontologies.put(ont.getURI(), ont);
			ontologies.put(ont.getURI().trimFragment(), ont);
			ontologies.put(ont.getURI().appendFragment(""), ont);
			Set<URI> spaces = new HashSet<URI>();
			query.setParameter("ont", ont);
			IExtendedIterator<IReference> result = query
					.evaluate(IReference.class);
			try {
				while (result.hasNext()) {
					IReference bean = result.next();
					if (bean.getURI() != null) {
						spaces.add(bean.getURI().namespace());
					}
				}
			} finally {
				result.close();
			}
			if (spaces.size() > 0) {
				for (URI ns : spaces) {
					ontologies.put(ns, ont);
				}
			} else {
				ontologies.put(guessNamespace(ont), ont);
			}
		}
	}

	private Map<URI, Ontology> findOntologies() {
		Map<URI, Ontology> ontologies = new HashMap<URI, Ontology>();
		assignOrphansToTheirOntology(ontologies);
		findNamespacesOfOntologies(ontologies);
		assignOrphansToNewOntology(ontologies);
		return ontologies;
	}

	private Ontology findOntology(URI ns, Map<URI, Ontology> ontologies) {
		if (ontologies.containsKey(ns)) {
			return ontologies.get(ns);
		}
		for (Map.Entry<URI, Ontology> e : ontologies.entrySet()) {
			String keyStr = e.getKey().toString();
			if (keyStr.indexOf('#') > 0
					&& ns.toString().startsWith(
							keyStr.substring(0, keyStr.indexOf('#'))))
				return e.getValue();
		}

		Ontology ont = manager.createNamed(ns.trimFragment(), Ontology.class);
		ontologies.put(ns, ont);
		return ont;
	}

	private String findPrefix(Ontology ont) {
		IExtendedIterator<INamespace> spaces = manager.getNamespaces();
		try {
			while (spaces.hasNext()) {
				INamespace next = spaces.next();
				if (next.getURI().equals(ont.getURI().namespace()))
					return next.getPrefix();
				for (Map.Entry<URI, Ontology> e : ontologies.entrySet()) {
					if (e.getValue().equals(ont)
							&& next.getURI().equals(e.getKey()))
						return next.getPrefix();
				}
			}
		} finally {
			spaces.close();
		}
		return null;
	}

	private Set<net.enilink.vocab.rdfs.Class> findSuperClasses(Class of) {
		HashSet<net.enilink.vocab.rdfs.Class> set = new HashSet<net.enilink.vocab.rdfs.Class>();
		set.add(of);
		return findSuperClasses(of, set);
	}

	private Set<net.enilink.vocab.rdfs.Class> findSuperClasses(
			Class of, Set<net.enilink.vocab.rdfs.Class> supers) {
		if (supers.addAll(of.getRdfsSubClassOf())) {
			for (Object s : of.getRdfsSubClassOf()) {
				findSuperClasses((Class) s, supers);
			}
		}
		return supers;
	}

	public Map<URI, URI> getAliases() {
		return aliases;
	}

	public Set<IReference> getAnonymousClasses() {
		return anonymousClasses;
	}

	public URI getOriginal(URI alias) {
		if (anonymousClasses.contains(alias))
			return null;
		if (aliases.containsKey(alias))
			return aliases.get(alias);
		return alias;
	}

	private String getPrefix(URI namespace) {
		return manager.getPrefix(namespace);
	}

	private URI guessNamespace(IEntity bean) {
		if (bean instanceof Ontology) {
			return bean.getURI();
		}
		return bean.getURI().namespace();
	}

	private void infer() {
		log.debug("inferring");
		propagateSubClassType(RDFS.TYPE_CLASS);
		symmetric(OWL.PROPERTY_INVERSEOF);
		symmetric(OWL.PROPERTY_EQUIVALENTCLASS);
		symmetric(OWL.PROPERTY_EQUIVALENTPROPERTY);
		symmetric(OWL.PROPERTY_DISJOINTWITH);
		setSubjectType(RDF.PROPERTY_FIRST, null, RDF.TYPE_LIST);
		setSubjectType(RDF.PROPERTY_REST, null, RDF.TYPE_LIST);
		setSubjectType(OWL.PROPERTY_UNIONOF, null, OWL.TYPE_CLASS);
		setSubjectType(OWL.PROPERTY_DISJOINTWITH, null, OWL.TYPE_CLASS);
		setSubjectType(OWL.PROPERTY_COMPLEMENTOF, null, OWL.TYPE_CLASS);
		setSubjectType(OWL.PROPERTY_EQUIVALENTCLASS, null, OWL.TYPE_CLASS);
		setSubjectType(OWL.PROPERTY_INTERSECTIONOF, null, OWL.TYPE_CLASS);
		setSubjectType(RDF.PROPERTY_TYPE, RDFS.TYPE_CLASS, OWL.TYPE_CLASS);
		setSubjectType(RDF.PROPERTY_TYPE, OWL.TYPE_DEPRECATEDCLASS,
				OWL.TYPE_CLASS);
		setObjectType(RDFS.PROPERTY_SUBCLASSOF, OWL.TYPE_CLASS);
		setObjectType(OWL.PROPERTY_UNIONOF, RDF.TYPE_LIST);
		setObjectType(RDFS.PROPERTY_ISDEFINEDBY, OWL.TYPE_ONTOLOGY);
		setSubjectType(OWL.PROPERTY_INVERSEOF, null, OWL.TYPE_OBJECTPROPERTY);
		setObjectType(OWL.PROPERTY_INVERSEOF, OWL.TYPE_OBJECTPROPERTY);
		setSubjectType(RDFS.PROPERTY_RANGE, null, RDF.TYPE_PROPERTY);
		setSubjectType(RDFS.PROPERTY_DOMAIN, null, RDF.TYPE_PROPERTY);
		setObjectType(RDFS.PROPERTY_SUBPROPERTYOF, RDF.TYPE_PROPERTY);
		setDatatype(OWL.PROPERTY_CARDINALITY, XMLSCHEMA.TYPE_NONNEGATIVEINTEGER);
		setDatatype(OWL.PROPERTY_MINCARDINALITY,
				XMLSCHEMA.TYPE_NONNEGATIVEINTEGER);
		setDatatype(OWL.PROPERTY_MAXCARDINALITY,
				XMLSCHEMA.TYPE_NONNEGATIVEINTEGER);
	}

	private String initcap(String str) {
		if (str.isEmpty()) {
			return str;
		}
		return str.substring(0, 1).toUpperCase() + str.substring(1);
	}

	private boolean isInOntology(URI subj, URI ns, Ontology ont) {
		if (subj.namespace().equals(ns))
			return true;
		return manager.hasMatch(subj, RDFS.PROPERTY_ISDEFINEDBY, ont);
	}

	private boolean isInSameOntology(URI subj, URI obj) {
		if (subj.namespace().equals(obj.namespace()))
			return true;
		IExtendedIterator<IStatement> stmts = manager.match(subj,
				RDFS.PROPERTY_ISDEFINEDBY, null);
		try {
			while (stmts.hasNext()) {
				if (manager.hasMatch(obj, RDFS.PROPERTY_ISDEFINEDBY, stmts
						.next().getObject()))
					return true;
			}
		} finally {
			stmts.close();
		}
		return false;
	}

	private boolean isLocal(IReference nc, IReference obj) {
		if (obj.getURI() == null)
			return true;
		if (nc.getURI() == null)
			return true;
		return isInSameOntology(nc.getURI(), obj.getURI());
	}

	private void mergeUnionClasses() {
		Iterable<Class> classes = manager.findAll(Class.class);
		for (Class clazz : classes) {
			List<? extends IEntity> unionOf = clazz.getOwlUnionOf();
			if (unionOf != null) {
				Collection<? extends net.enilink.vocab.rdfs.Class> common = findCommonSupers(unionOf);
				if (common.contains(clazz)) {
					unionOf.clear();
					manager.remove(unionOf);
					continue;
				} else if (findCommon(common, unionOf) != null) {
					Class sup = findCommon(common, unionOf);
					unionOf.clear();
					manager.remove(unionOf);
					if (sup.getURI() == null) {
						sup = nameAnonymous(sup);
					}
					rename(clazz, sup.getURI());
					continue;
				}
				clazz.getRdfsSubClassOf().addAll(common);
				for (IEntity bean : unionOf) {
					Class of = manager.assignTypes(bean, Class.class);
					if (bean instanceof Datatype && bean.getURI() != null) {
						// don't use anonymous class for datatypes
						rename(clazz, bean.getURI());
					} else if (isLocal(clazz, of)) {
						of.getRdfsSubClassOf().add(clazz);
					} else {
						Ontology ont = (Ontology) clazz.getRdfsIsDefinedBy()
								.toArray()[0];
						URI nc = createLocalClass(of.getURI(), ont);
						manager.add(Arrays
								.asList( //
								new Statement(nc, RDF.PROPERTY_TYPE,
										OWL.TYPE_CLASS), //
										new Statement(nc,
												RDFS.PROPERTY_SUBCLASSOF, of), //
										new Statement(nc,
												RDFS.PROPERTY_SUBCLASSOF, clazz), //
										new Statement(nc,
												RDFS.PROPERTY_ISDEFINEDBY, ont)));
						renameClass(of.getURI(), nc);
					}
				}
			}
		}
	}

	private void moveForeignDomains() {
		IExtendedIterator<IStatement> stmts = manager.match(null,
				RDFS.PROPERTY_DOMAIN, null);
		try {
			while (stmts.hasNext()) {
				IStatement stmt = stmts.next();
				if (stmt.getSubject().getURI() != null
						&& stmt.getObject() instanceof IReference
						&& ((IReference) stmt.getObject()).getURI() != null) {
					URI subj = stmt.getSubject().getURI();
					URI obj = ((IReference) stmt.getObject()).getURI();
					for (Map.Entry<URI, Ontology> e : ontologies.entrySet()) {
						URI ns = e.getKey();
						Ontology ont = e.getValue();
						if (isInOntology(subj, ns, ont)
								&& !isInSameOntology(subj, obj)) {
							URI nc = createLocalClass(obj, ont);
							log.debug("moving " + subj + " " + nc);
							manager.remove(stmt);
							manager.add(Arrays.asList( //
									new Statement(subj, RDFS.PROPERTY_DOMAIN,
											nc), //
									new Statement(nc, RDF.PROPERTY_TYPE,
											OWL.TYPE_CLASS), //
									new Statement(nc, RDFS.PROPERTY_SUBCLASSOF,
											obj), //
									new Statement(nc,
											RDFS.PROPERTY_ISDEFINEDBY, ont)));
						}
					}
				}
			}
		} finally {
			stmts.close();
		}
	}

	@SuppressWarnings("unchecked")
	private Class nameAnonymous(Class clazz) {
		List<Class> unionOf = clazz.getOwlUnionOf();
		if (unionOf != null) {
			return renameClass(clazz, "Or", unionOf);
		}
		List<Class> intersectionOf = clazz.getOwlIntersectionOf();
		if (intersectionOf != null) {
			return renameClass(clazz, "And", intersectionOf);
		}
		List<? extends IEntity> oneOf = (List<? extends IEntity>) clazz
				.getOwlOneOf();
		if (oneOf != null) {
			boolean things = true;
			for (Object of : oneOf) {
				things &= of instanceof Thing;
			}
			if (things)
				return renameClass(clazz, "Or", oneOf);
		}
		Class complement = clazz.getOwlComplementOf();
		if (complement != null) {
			net.enilink.komma.core.URI comp = complement.getURI();
			if (comp == null) {
				Class nc = nameAnonymous(complement);
				if (nc == null)
					return null;
				comp = nc.getURI();
			}
			String name = "Not" + comp.localPart();
			return rename(clazz, comp.namespace().appendLocalPart(name));
		}
		return null;
	}

	public void normalize() throws Exception {
		infer();
		ontologies = findOntologies();
		checkNamespacePrefixes();
		checkPropertyDomains();
		subClassIntersectionOf();
		subClassOneOf();
		distributeEquivalentClasses();
		mergeUnionClasses();
		moveForeignDomains();
		if (log.isDebugEnabled()) {
			File file = File.createTempFile("normalized", ".rdf");
			// manager.getConnection().export(new OrganizedRDFXMLWriter(file));
			log.debug("Normalized RDF saved to " + file);
		}
	}

	private void propagateSubClassType(IReference classDef) {
		for (IReference c : findClasses(Collections.singleton(classDef))) {
			if (c.equals(RDFS.TYPE_DATATYPE))
				continue;
			IExtendedIterator<IStatement> stmts = manager.match(null,
					RDF.PROPERTY_TYPE, c);
			try {
				while (stmts.hasNext()) {
					IStatement stmt = stmts.next();
					IReference subj = stmt.getSubject();
					manager.add(new Statement(subj, RDF.PROPERTY_TYPE, classDef));
				}
			} finally {
				stmts.close();
			}
		}
	}

	private Class rename(Class clazz, net.enilink.komma.core.URI name) {
		log.debug("renaming " + clazz + " " + name);
		Ontology ont = findOntology(name.namespace(), ontologies);
		Class renamed = manager.rename(clazz, name);
		renamed.getRdfsIsDefinedBy().add(ont);
		anonymousClasses.add(renamed);
		return renamed;
	}

	private Class renameClass(Class clazz, String and,
			List<? extends IEntity> list) {
		String namespace = null;
		Set<String> names = new TreeSet<String>();
		for (IEntity of : list) {
			net.enilink.komma.core.URI uri = of.getURI();
			if (uri == null) {
				if (!(of instanceof Class))
					return null;
				Class nc = nameAnonymous((Class) of);
				if (nc == null)
					return null;
				uri = nc.getURI();
			}
			if (namespace == null || commonNS.contains(namespace)) {
				namespace = uri.namespace().toString();
			}
			names.add(uri.localPart());
		}
		StringBuilder sb = new StringBuilder();
		for (String localPart : names) {
			sb.append(initcap(localPart));
			sb.append(and);
		}
		sb.setLength(sb.length() - and.length());
		return rename(clazz,
				URIs.createURI(namespace).appendLocalPart(sb.toString()));
	}

	private void renameClass(URI obj, URI nc) {
		log.debug("renaming " + obj + " " + nc);
		aliases.put(nc, obj);
		IExtendedIterator<IStatement> stmts = manager.match(null, null, obj);
		try {
			while (stmts.hasNext()) {
				IStatement stmt = stmts.next();
				if (isLocal(nc, stmt.getSubject())) {
					if (!stmt.getPredicate().equals(RDFS.PROPERTY_RANGE)
							|| !stmt.getObject().equals(RDFS.TYPE_RESOURCE)) {
						if (!stmt.getPredicate().equals(RDF.PROPERTY_TYPE))
							manager.remove(stmt);
						manager.add(new Statement(stmt.getSubject(), stmt
								.getPredicate(), nc));
					}
				}
			}
		} finally {
			stmts.close();
		}
		if (obj.equals(RDFS.TYPE_RESOURCE)) {
			Class base = manager.createNamed(URIs.createURI(nc.toString()),
					Class.class);
			addBaseClass(base, Class.class);
		}
	}

	private void setDatatype(URI pred, URI datatype) {
		IExtendedIterator<IStatement> stmts = manager.match(null, pred, null);
		try {
			while (stmts.hasNext()) {
				IStatement stmt = stmts.next();
				if (stmt.getObject() instanceof IReference) {
					continue;
				}
				String label = ((ILiteral) stmt.getObject()).getLabel();
				ILiteral literal = manager.createLiteral(label, datatype, null);
				manager.remove(stmt);
				manager.add(new Statement(stmt.getSubject(), stmt
						.getPredicate(), literal));
			}
		} finally {
			stmts.close();
		}
	}

	private void setObjectType(URI pred, URI type) {
		IExtendedIterator<IStatement> stmts = manager.match(null, pred, null);
		try {
			while (stmts.hasNext()) {
				IStatement st = stmts.next();
				if (st.getObject() instanceof IReference) {
					IReference subj = (IReference) st.getObject();
					manager.add(new Statement(subj, RDF.PROPERTY_TYPE, type));
				} else {
					log.warn("Invalid statement " + st);
				}
			}
		} finally {
			stmts.close();
		}
	}

	@Inject
	public void setEntityManager(IEntityManager manager) {
		this.manager = manager;
	}

	private void setSubjectType(URI pred, IValue obj, URI type) {
		IExtendedIterator<IStatement> stmts = manager.match(null, pred, obj);
		try {
			while (stmts.hasNext()) {
				manager.add(new Statement(stmts.next().getSubject(),
						RDF.PROPERTY_TYPE, type));
			}
		} finally {
			stmts.close();
		}
	}

	@SuppressWarnings("unchecked")
	private void subClassIntersectionOf() {
		IQuery<?> query = manager.createQuery(SELECT_C_INTERSECTION_OF);
		java.util.List<Class> classes = (java.util.List<Class>) query
				.getResultList();
		for (Class c : classes) {
			List<Class> intersectionOf = c.getOwlIntersectionOf();

			if (intersectionOf != null) {
				for (Class of : intersectionOf) {
					c.getRdfsSubClassOf().add(of);
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void subClassOneOf() {
		IQuery<?> query = manager.createQuery(SELECT_C_ONE_OF);
		java.util.List<Class> classes = (java.util.List<Class>) query
				.getResultList();
		for (Class c : classes) {
			java.util.List<Object> list = new ArrayList<Object>();
			for (Object of : c.getOwlOneOf()) {
				if (of instanceof Thing) {
					Set<?> types = ((Thing) of).getRdfTypes();
					if (types.isEmpty()) {
						list.add(manager.find(OWL.TYPE_THING));
					} else {
						list.addAll(types);
					}
				}
			}
			for (net.enilink.vocab.rdfs.Class s : findCommonSupers(list)) {
				c.getRdfsSubClassOf().add(s);
			}
		}
	}

	private void symmetric(URI pred) {
		IExtendedIterator<IStatement> stmts = manager.match(null, pred, null);
		try {
			while (stmts.hasNext()) {
				IStatement stmt = stmts.next();
				if (stmt.getObject() instanceof IReference) {
					IReference subj = (IReference) stmt.getObject();
					manager.add(new Statement(subj, pred, stmt.getSubject()));
				} else {
					log.warn("Invalid statement " + stmt);
				}
			}
		} finally {
			stmts.close();
		}
	}
}