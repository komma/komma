/*******************************************************************************
 * Copyright (c) 2009, 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.em.concepts;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.composition.traits.Behaviour;
import net.enilink.komma.core.IQuery;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IResultDescriptor;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.core.URI;
import net.enilink.komma.em.results.ResultDescriptor;
import net.enilink.vocab.komma.KOMMA;
import net.enilink.vocab.owl.OWL;
import net.enilink.vocab.rdfs.RDFS;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ClassSupport extends BehaviorBase implements IClass,
		Behaviour<IClass> {
	private static Logger log = LoggerFactory.getLogger(ClassSupport.class);

	@SuppressWarnings("unlikely-arg-type")
	private final String SELECT_DIRECT_SUBCLASSES(boolean named) {
		StringBuilder sb = new StringBuilder(PREFIX);
		sb.append("SELECT ?subClass WHERE { ");
		// support stores that don't draw the inferences
		// (someClass rdfs:subClassOf rdfs:Resource) and (someClass
		// rdfs:subClassOf owl:Thing)
		if (RDFS.TYPE_RESOURCE.equals(this) || OWL.TYPE_THING.equals(this)) {
			sb.append("{ ?subClass a ")
					.append(RDFS.TYPE_RESOURCE.equals(this) ? "rdfs:Class"
							: "owl:Class").append(" . ");
			sb.append("MINUS { ?subClass rdfs:subClassOf ?someSuperClass . { ?someSuperClass a rdfs:Class } UNION { ?someSuperClass a owl:Class } ")
					.append("FILTER (isIRI(?someSuperClass) && ?someSuperClass != ?subClass && ?someSuperClass != owl:Thing && ?someSuperClass != rdfs:Resource) } }");
			sb.append(" UNION { ?subClass rdfs:subClassOf ?superClass }");
		} else {
			sb.append("?subClass rdfs:subClassOf ?superClass . ");
		}
		sb.append("FILTER NOT EXISTS {")
				.append("?subClass rdfs:subClassOf ?otherSuperClass . ")
				.append("?otherSuperClass rdfs:subClassOf ?superClass  . ")
				.append("FILTER (")
				.append(named ? "isIRI(?otherSuperClass) && " : "")
				.append("?subClass != ?otherSuperClass && ?superClass != ?otherSuperClass)")
				.append("}")
				.append(" FILTER (" + (named ? "isIRI(?subClass) && " : ""))
				.append("?subClass != ?superClass && ?subClass != owl:Nothing)"
						+ "}").append("ORDER BY ?subClass");
		return sb.toString();
	};

	private final String SELECT_SUBCLASSES(boolean named) {
		return PREFIX + "SELECT DISTINCT ?subClass { "
				+ ((OWL.TYPE_THING.equals(this)) ? "?subClass a owl:Class . " //
						: "?subClass rdfs:subClassOf+ ?superClass . " //
				) + "FILTER (?subClass != ?superClass"
				+ (named ? " && isIRI(?subClass)" : "")
				+ ") } ORDER BY ?subClass";
	};

	private static final String SELECT_LEAF_SUBCLASSES(boolean named) {
		return PREFIX + "SELECT DISTINCT ?subClass { "
				+ "?subClass rdfs:subClassOf* ?superClass . OPTIONAL {"
				+ "?otherSubClass rdfs:subClassOf ?subClass . "
				+ "FILTER (?subClass != ?otherSubClass)" + "} FILTER ("
				+ (named ? "isIRI(?subClass) && " : "")
				+ "!bound(?otherSubClass)) } ORDER BY ?subClass";
	};

	private static final String SELECT_DIRECT_SUPERCLASSES(boolean named) {
		return PREFIX
				+ "SELECT DISTINCT ?superClass { "
				+ "?subClass rdfs:subClassOf ?superClass . "
				+ "FILTER NOT EXISTS { ?superClass a owl:Restriction } "
				+ "FILTER NOT EXISTS {"
				+ "?subClass rdfs:subClassOf ?otherSuperClass . "
				+ "?otherSuperClass rdfs:subClassOf ?superClass . "
				+ "FILTER (?subClass != ?otherSuperClass && ?superClass != ?otherSuperClass"
				+ (named ? " && isIRI(?otherSuperClass)" : "") + ")"
				+ "} FILTER (?subClass != ?superClass"
				+ (named ? "&& isIRI(?superClass)" : "")
				+ ") } ORDER BY ?superClass";
	};

	private static final String SELECT_SUPERCLASSES(boolean named) {
		return PREFIX + "SELECT DISTINCT ?superClass { "
				+ "?subClass rdfs:subClassOf ?superClass . "
				+ "FILTER NOT EXISTS {?superClass a owl:Restriction}"
				+ "FILTER (?subClass != ?superClass"
				+ (named ? "&& isIRI(?subClass)" : "")
				+ ") } ORDER BY ?superClass";
	}

	private static final String SELECT_INSTANCES = PREFIX
			+ "SELECT DISTINCT ?instance { ?instance a ?class . }";

	private static final String HAS_SUBCLASSES(boolean named) {
		return PREFIX + "ASK { ?subClass rdfs:subClassOf ?superClass . "
				+ "FILTER (" + (named ? "isIRI(?subClass) && " : "")
				+ "?subClass != ?superClass && ?subClass != owl:Nothing)}";
	}

	private static final String SELECT_DECLARED_PROPERTIES = PREFIX
			+ "SELECT DISTINCT ?property {{ ?property rdfs:domain ?class } UNION " // 
			+ "{ ?class rdfs:subClassOf ?restriction . ?restriction owl:onProperty ?property }} ORDER BY ?property";

	private static final String HAS_DECLARED_PROPERTIES = PREFIX + "ASK { "
			+ "{ ?property rdfs:domain ?class } UNION " // 
			+ "{ ?class rdfs:subClassOf ?restriction . ?restriction owl:onProperty ?property }}";

	public static final IResultDescriptor<?> HAS_NAMED_SUBCLASSES_DESC() {
		return new ResultDescriptor<Object>(HAS_SUBCLASSES(true),
				"komma:hasNamedSubClasses", "subClass", "superClass");
	}

	public static final IResultDescriptor<?> DIRECT_NAMED_SUPERCLASSES_DESC() {
		return new ResultDescriptor<Object>(SELECT_DIRECT_SUPERCLASSES(true),
				"komma:directNamedSuperClasses", "superClass", "subClass");
	}

	public IExtendedIterator<IResource> getInstances() {
		IQuery<?> query = getEntityManager().createQuery(SELECT_INSTANCES);
		query.setParameter("class", this);
		return query.evaluate(IResource.class);
	}

	public IExtendedIterator<IReference> getInstancesAsReferences() {
		IQuery<?> query = getEntityManager().createQuery(SELECT_INSTANCES);
		query.setParameter("class", this);
		return query.evaluateRestricted(IReference.class);
	}

	@Override
	public IExtendedIterator<IClass> getDirectNamedSubClasses() {
		// [PERFORMANCE] direct named subclasses are retrieved without inference
		return getSubClasses(true, false, true);
	}

	@Override
	public IExtendedIterator<IClass> getNamedSubClasses() {
		return getSubClasses(false, true, true);
	}

	@Override
	public IExtendedIterator<IClass> getSubClasses(boolean direct,
			boolean includeInferred) {
		return getSubClasses(direct, includeInferred, false);
	}

	protected IExtendedIterator<IClass> getSubClasses(boolean direct,
			boolean includeInferred, boolean named) {
		String queryString;
		if (direct) {
			queryString = SELECT_DIRECT_SUBCLASSES(named);
		} else {
			queryString = SELECT_SUBCLASSES(named);
		}

		if (named) {
			queryString = new ResultDescriptor<IClass>(queryString)
			// .prefetch(DIRECT_NAMED_SUPERCLASSES_DESC()).prefetchTypes()
					.prefetch(HAS_NAMED_SUBCLASSES_DESC()).//
					prefetch(ResourceSupport.DIRECT_NAMED_CLASSES_DESC()).//
					toQueryString();
		}

		IQuery<?> query = getEntityManager().createQuery(queryString,
				includeInferred);
		query.setParameter("superClass", this);

		return query.evaluate(IClass.class);
	}

	@Override
	public Boolean hasNamedSubClasses() {
		return getEntityManager()
				.createQuery(HAS_NAMED_SUBCLASSES_DESC().toQueryString())
				.setParameter("superClass", this).getBooleanResult();
	}

	@Override
	public IExtendedIterator<IClass> getNamedLeafSubClasses(
			boolean includeInferred) {
		return getLeafSubClasses(includeInferred, true);
	}

	@Override
	public IExtendedIterator<IClass> getLeafSubClasses(boolean includeInferred) {
		return getLeafSubClasses(includeInferred, false);
	}

	protected IExtendedIterator<IClass> getLeafSubClasses(
			boolean includeInferred, boolean named) {
		IQuery<?> query = getEntityManager().createQuery(
				SELECT_LEAF_SUBCLASSES(named), includeInferred);
		query.setParameter("superClass", getBehaviourDelegate());

		return query.evaluate(IClass.class);
	}

	@Override
	public IExtendedIterator<IClass> getDirectNamedSuperClasses() {
		log.info("Get super classes for {}", getBehaviourDelegate());
		// [PERFORMANCE] direct named super-classes are retrieved without
		// inference
		return getEntityManager()
				.createQuery(DIRECT_NAMED_SUPERCLASSES_DESC().toQueryString(),
						false).setParameter("subClass", getBehaviourDelegate())
				.evaluate(IClass.class);
	}

	@Override
	public IExtendedIterator<IClass> getNamedSuperClasses() {
		return getSuperClasses(false, true, true);
	}

	@Override
	public IExtendedIterator<IClass> getSuperClasses(boolean direct,
			boolean includeInferred) {
		return getSuperClasses(direct, includeInferred, false);
	}

	protected IExtendedIterator<IClass> getSuperClasses(boolean direct,
			boolean includeInferred, boolean named) {
		if (direct && named) {
			return getBehaviourDelegate().getDirectNamedSuperClasses();
		}
		IQuery<?> query = getEntityManager().createQuery(
				direct ? SELECT_DIRECT_SUPERCLASSES(named)
						: SELECT_SUPERCLASSES(named), includeInferred);
		query.setParameter("subClass", getBehaviourDelegate());

		return query.evaluate(IClass.class);
	}

	@Override
	public boolean hasNamedSubClasses(boolean includeInferred) {
		return hasSubClasses(includeInferred, true);
	}

	@Override
	public boolean hasSubClasses(boolean includeInferred) {
		return hasSubClasses(includeInferred, false);
	}

	protected boolean hasSubClasses(boolean includeInferred, boolean named) {
		if (named) {
			// use cacheable method
			return getBehaviourDelegate().hasNamedSubClasses();
		}
		try {
			IQuery<?> query = getEntityManager().createQuery(
					HAS_SUBCLASSES(named), includeInferred);
			query.setParameter("superClass", this);

			return query.getBooleanResult();
		} catch (Exception e) {
			throw new KommaException(e);
		}
	}

	public boolean hasDeclaredProperties(boolean includeInferred) {
		try {
			IQuery<?> query = getEntityManager().createQuery(
					HAS_DECLARED_PROPERTIES, includeInferred);
			query.setParameter("class", this);

			return query.getBooleanResult();
		} catch (Exception e) {
			throw new KommaException(e);
		}
	}

	@SuppressWarnings("unchecked")
	public IExtendedIterator<IProperty> getDeclaredProperties(
			boolean includeInferred) {
		IQuery<?> query = getEntityManager().createQuery(
				SELECT_DECLARED_PROPERTIES, includeInferred);
		query.setParameter("class", getBehaviourDelegate());

		return (IExtendedIterator<IProperty>) query.evaluate();
	}

	@Override
	public IResource newInstance() {
		return newInstance(null);
	}

	@Override
	public IResource newInstance(URI uri) {
		if (uri == null) {
			return (IResource) getEntityManager()
					.create(getBehaviourDelegate());
		}
		return (IResource) getEntityManager().createNamed(uri,
				getBehaviourDelegate());
	}

	@Override
	public boolean isAbstract() {
		return getAsSet(KOMMA.PROPERTY_ISABSTRACT).contains(Boolean.TRUE);
	}
}
