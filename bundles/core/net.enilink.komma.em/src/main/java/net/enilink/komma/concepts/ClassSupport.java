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
package net.enilink.komma.concepts;

import java.util.Collection;

import net.enilink.composition.traits.Behaviour;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.vocab.owl.OWL;
import net.enilink.komma.results.ResultDescriptor;
import net.enilink.komma.core.IQuery;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IResultDescriptor;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.core.URI;

public abstract class ClassSupport extends BehaviorBase implements IClass,
		Behaviour<IClass> {
	private static Logger log = LoggerFactory.getLogger(ClassSupport.class);

	private final String SELECT_DIRECT_SUBCLASSES(boolean named) {
		return PREFIX
				+ "SELECT ?subClass WHERE { "
				// support stores that don't draw the inference
				// (someClass rdfs:subClassOf owl:Thing)
				+ ((OWL.TYPE_THING.equals(this)) ? "{ ?subClass a owl:Class . "
						+ "MINUS { ?subClass rdfs:subClassOf ?someSuperClass "
						+ "FILTER (isIRI(?someSuperClass) && ?someSuperClass != ?subClass && ?someSuperClass != owl:Thing && ?someSuperClass != rdfs:Resource) } }"
						: "?subClass rdfs:subClassOf ?superClass . " //
				)
				// + "?subClass rdfs:subClassOf ?superClass . "
				+ "FILTER NOT EXISTS {"
				+ "?subClass rdfs:subClassOf ?otherSuperClass . "
				+ "?otherSuperClass rdfs:subClassOf ?superClass  . " //
				+ "FILTER ("
				+ (named ? "isIRI(?otherSuperClass) && " : "")
				+ "?subClass != ?otherSuperClass && ?superClass != ?otherSuperClass)"
				+ "}" //
				+ " FILTER (" + (named ? "isIRI(?subClass) && " : "")
				+ "?subClass != ?superClass && ?subClass != owl:Nothing)" + "}"
				+ "ORDER BY ?subClass";
	};

	private final String SELECT_SUBCLASSES(boolean named) {
		return PREFIX + "SELECT DISTINCT ?subClass WHERE { "
				+ ((OWL.TYPE_THING.equals(this)) ? "?subClass a owl:Class . " //
						: "?subClass rdfs:subClassOf ?superClass . " //
				) + "FILTER (?subClass != ?superClass"
				+ (named ? " && isIRI(?subClass)" : "")
				+ ") } ORDER BY ?subClass";
	};

	private static final String SELECT_LEAF_SUBCLASSES(boolean named) {
		return PREFIX + "SELECT DISTINCT ?subClass " + "WHERE { "
				+ "?subClass rdfs:subClassOf ?superClass . " + "OPTIONAL {"
				+ "?otherSubClass rdfs:subClassOf ?subClass . "
				+ "FILTER (?subClass != ?otherSubClass)" + "} FILTER ("
				+ (named ? "isIRI(?subClass) && " : "")
				+ "!bound(?otherSubClass)) } ORDER BY ?subClass";
	};

	private static final String SELECT_DIRECT_SUPERCLASSES(boolean named) {
		return PREFIX
				+ "SELECT DISTINCT ?superClass "
				+ "WHERE { "
				+ "?subClass rdfs:subClassOf ?superClass . "
				+ "FILTER NOT EXISTS { ?superClass a owl:Restriction }"
				+ "FILTER NOT EXISTS {"
				+ "?subClass rdfs:subClassOf ?otherSuperClass . "
				+ "?otherSuperClass rdfs:subClassOf ?superClass ."
				+ "FILTER (?subClass != ?otherSuperClass && ?superClass != ?otherSuperClass"
				+ (named ? " && isIRI(?otherSuperClass)" : "") + ")"
				+ "} FILTER (?subClass != ?superClass"
				+ (named ? "&& isIRI(?superClass)" : "")
				+ ") } ORDER BY ?superClass";
	};

	private static final String SELECT_SUPERCLASSES(boolean named) {
		return PREFIX + "SELECT DISTINCT ?superClass " + "WHERE { "
				+ "?subClass rdfs:subClassOf ?superClass . "
				+ "FILTER NOT EXISTS {?superClass a owl:Restriction}"
				+ "FILTER (?subClass != ?superClass"
				+ (named ? "&& isIRI(?subClass)" : "")
				+ ") } ORDER BY ?superClass";
	}

	private static final String SELECT_INSTANCES = PREFIX
			+ "SELECT DISTINCT ?instance " + "WHERE { "
			+ "?instance a ?class ." + "}";

	private static final String HAS_SUBCLASSES(boolean named) {
		return PREFIX + "ASK { " + "?subClass rdfs:subClassOf ?superClass . "
				+ "FILTER (" + (named ? "isIRI(?subClass) && " : "")
				+ "?subClass != ?superClass && ?subClass != owl:Nothing)}";
	}

	private static final String SELECT_DECLARED_PROPERTIES = PREFIX
			+ "SELECT DISTINCT ?property " + "WHERE { "
			+ "?property rdfs:domain ?class ." + "} ORDER BY ?property";

	private static final String HAS_DECLARED_PROPERTIES = PREFIX + "ASK { "
			+ "?property rdfs:domain ?class ." + "}";

	public static final IResultDescriptor<?> HAS_NAMED_SUBCLASSES_DESC() {
		return new ResultDescriptor<Object>(HAS_SUBCLASSES(true),
				"urn:komma:hasNamedSubClasses", "subClass", "superClass");
	}

	public static final IResultDescriptor<?> DIRECT_NAMED_SUPERCLASSES_DESC() {
		return new ResultDescriptor<Object>(SELECT_DIRECT_SUPERCLASSES(true),
				"urn:komma:directNamedSuperClasses", "superClass", "subClass");
	}

	public Collection<IResource> getInstances() {
		IQuery<?> query = getEntityManager().createQuery(SELECT_INSTANCES);
		query.setParameter("class", this);
		return query.evaluate(IResource.class).toSet();
	}

	public Collection<IReference> getInstancesAsReferences() {
		IQuery<?> query = getEntityManager().createQuery(SELECT_INSTANCES);
		query.setParameter("class", this);
		return query.evaluateRestricted(IReference.class).toSet();
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
			System.out.println(queryString);
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
		return false;
	}
}
