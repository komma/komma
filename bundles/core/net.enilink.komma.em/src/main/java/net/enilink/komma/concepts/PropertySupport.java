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

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import net.enilink.composition.traits.Behaviour;

import net.enilink.commons.iterator.Filter;
import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.commons.iterator.WrappedIterator;
import net.enilink.vocab.owl.DatatypeProperty;
import net.enilink.vocab.owl.FunctionalProperty;
import net.enilink.komma.core.IQuery;
import net.enilink.komma.core.IReference;
import net.enilink.komma.util.KommaUtil;

public abstract class PropertySupport extends BehaviorBase implements
		IProperty, Behaviour<IProperty> {
	private static final String SELECT_DIRECT_SUBPROPERTIES(boolean named) {
		return PREFIX
				+ "SELECT DISTINCT ?subProperty "
				+ "WHERE { "
				+ "?subProperty rdfs:subPropertyOf ?superProperty . "
				+ "OPTIONAL {"
				+ "?subProperty rdfs:subPropertyOf ?otherSuperProperty . "
				+ "?otherSuperProperty rdfs:subPropertyOf ?superProperty . "
				+ "FILTER (!sameTerm(?subProperty, ?otherSuperProperty) && !sameTerm(?superProperty, ?otherSuperProperty))"
				+ "} FILTER (!sameTerm(?subProperty, ?superProperty)"
				+ (named ? " && isIRI(?subProperty)" : "")
				+ " && !bound(?otherSuperProperty)) }";
	};

	private static final String SELECT_SUBPROPERTIES(boolean named) {
		return PREFIX + "SELECT DISTINCT ?subProperty " + "WHERE { "
				+ "?subProperty rdfs:subPropertyOf ?superProperty . "
				+ "FILTER (!sameTerm(?subProperty, ?superProperty)"
				+ (named ? "&& isIRI(?subProperty)" : "") + ") }";
	};

	private static final String SELECT_DIRECT_SUPERPROPERTIES(boolean named) {
		return PREFIX
				+ "SELECT DISTINCT ?superProperty "
				+ "WHERE { "
				+ "?subProperty rdfs:subPropertyOf ?superProperty . "
				+ "OPTIONAL {"
				+ "?subProperty rdfs:subPropertyOf ?otherSuperProperty . "
				+ "?otherSuperProperty rdfs:subPropertyOf ?superProperty . "
				+ "FILTER (!sameTerm(?subProperty, ?otherSuperClass) && !sameTerm(?superProperty, ?otherSuperProperty))"
				+ "} FILTER (!sameTerm(?subProperty, ?superProperty)"
				+ (named ? "&& isIRI(?superProperty)" : "")
				+ "&& !bound(?otherSuperProperty)) }";
	};

	private static final String SELECT_SUPERPROPERTIES(boolean named) {
		return PREFIX + "SELECT DISTINCT ?superProperty " + "WHERE { "
				+ "?subProperty rdfs:subPropertyOf ?superProperty . "
				+ "FILTER (!sameTerm(?subProperty, ?superProperty)"
				+ (named ? "&& isIRI(?subProperty)" : "") + ") }";
	}

	private static final String IS_CONTAINMENT = PREFIX + "ASK { " //
			+ "?property rdfs:subPropertyOf komma:contains" //
			+ " }";

	private static final String IS_ORDERED_CONTAINMENT = PREFIX + "ASK { " //
			+ "{ ?property rdfs:subPropertyOf komma:orderedContains }" //
			+ " }";

	private static final String IS_RANGE_INSTANCE = PREFIX
			+ "ASK { " //
			+ "?object a ?class ." //
			+ "{?property rdfs:range ?class} UNION {?class rdfs:subClassOf ?superClass . ?property rdfs:range ?superClass}" //
			+ " }";
	private static final String IS_DOMAIN_INSTANCE = PREFIX + "ASK { " //
			+ "?object a ?domain ." //
			+ "?property rdfs:domain ?domain " //
			+ " }";

	private static final String HAS_LIST_RANGE_QUERY = PREFIX + "ASK { " //
			+ "{ ?property rdfs:range rdf:List } UNION " //
			+ "{ ?property rdfs:range rdfs:Container }" //
			+ " }";

	private static final String DIRECT_RANGE_QUERY = PREFIX
			+ "SELECT DISTINCT ?range WHERE { " //
			+ "?property rdfs:range ?range " //
			+ "OPTIONAL { ?property rdfs:range ?otherRange . ?range rdfs:subClassOf ?otherRange FILTER (?range != ?otherRange) }" //
			+ "FILTER (!bound(?otherRange))" //
			+ " }";

	private static final String RANGE_QUERY = PREFIX
			+ "SELECT DISTINCT ?range WHERE { " //
			+ "?property rdfs:range ?range " //
			+ " }";

	@Override
	public boolean isContainment() {
		if (getBehaviourDelegate() instanceof DatatypeProperty) {
			return false;
		}

		if (CONCEPTS.PROPERTY_CONTAINS.equals(getURI())) {
			return true;
		}

		IQuery<?> query = getEntityManager().createQuery(IS_CONTAINMENT);
		query.setParameter("property", this);

		return query.getBooleanResult();
	}

	@SuppressWarnings("unchecked")
	protected IExtendedIterator<IProperty> getSubProperties(boolean direct,
			boolean includeInferred) {
		IQuery<?> query = getEntityManager().createQuery(
				direct ? SELECT_DIRECT_SUBPROPERTIES(true)
						: SELECT_SUBPROPERTIES(true), includeInferred);
		query.setParameter("superProperty", getBehaviourDelegate());

		return (IExtendedIterator<IProperty>) query.evaluate();
	}

	@Override
	public IExtendedIterator<IProperty> getDirectSuperProperties() {
		return getSuperProperties(true, true);
	}

	@Override
	public IExtendedIterator<IProperty> getDirectSubProperties() {
		return getSubProperties(true, true);
	}

	@Override
	public IExtendedIterator<IProperty> getSubProperties() {
		return getSubProperties(true, true);
	}

	@Override
	public IExtendedIterator<IProperty> getSuperProperties() {
		return getSuperProperties(true, true);
	}

	@SuppressWarnings("unchecked")
	protected IExtendedIterator<IProperty> getSuperProperties(boolean direct,
			boolean includeInferred) {
		IQuery<?> query = getEntityManager().createQuery(
				direct ? SELECT_DIRECT_SUPERPROPERTIES(true)
						: SELECT_SUPERPROPERTIES(true), includeInferred);
		query.setParameter("subProperty", getBehaviourDelegate());

		return (IExtendedIterator<IProperty>) query.evaluate();
	}

	@Override
	public boolean isOrderedContainment() {
		if (getBehaviourDelegate() instanceof DatatypeProperty) {
			return false;
		}

		if (CONCEPTS.PROPERTY_ORDEREDCONTAINS.equals(getURI())) {
			return true;
		}

		IQuery<?> query = getEntityManager()
				.createQuery(IS_ORDERED_CONTAINMENT);
		query.setParameter("property", this);

		return query.getBooleanResult();
	}

	@Override
	public boolean isDomainCompatible(Object object) {
		if (object instanceof IReference) {
			IQuery<?> query = getEntityManager()
					.createQuery(IS_DOMAIN_INSTANCE);
			query.setParameter("property", getBehaviourDelegate());
			query.setParameter("object", object);

			return query.getBooleanResult();
		}
		return false;
	}

	@Override
	public boolean isRangeCompatible(IResource subject, Object object) {
		if (object instanceof IResource) {
			// query can be optimized if OWL-inferencing is supported
			if (getEntityManager().getInferencing().doesOWL()) {
				String query = PREFIX + "ASK {"
						+ "?o a ?c . ?p rdfs:range ?c }";

				return subject.getEntityManager().createQuery(query)
						.setParameter("o", object)
						.setParameter("p", getBehaviourDelegate())
						.getBooleanResult();
			}

			String query = PREFIX
					+ "SELECT DISTINCT ?r WHERE {"
					+ "?o a ?c . ?c rdfs:subClassOf ?restriction . ?restriction owl:onProperty ?p . "
					+ "{{?restriction owl:allValuesFrom ?r} UNION {?restriction owl:someValuesFrom ?r} OPTIONAL {?r owl:intersectionOf ?x} FILTER (!bound(?x))}"
					+ "}";

			@SuppressWarnings("unchecked")
			IExtendedIterator<? extends IClass> it = (IExtendedIterator<IClass>) subject
					.getEntityManager().createQuery(query)
					.setParameter("o", object)
					.setParameter("p", getBehaviourDelegate()).evaluate();

			if (it.hasNext()) {
				Set<IClass> rangeClasses = new HashSet<IClass>();

				Set<IReference> seenClasses = new HashSet<IReference>();
				Queue<net.enilink.vocab.owl.Class> queue = new LinkedList<net.enilink.vocab.owl.Class>(
						it.toSet());
				while (!queue.isEmpty()) {
					net.enilink.vocab.owl.Class clazz = queue.remove();
					if (!seenClasses.add(clazz)) {
						continue;
					}

					List<net.enilink.vocab.owl.Class> unionOf = clazz
							.getOwlUnionOf();

					if (unionOf == null) {
						List<net.enilink.vocab.owl.Class> intersectionOf = clazz
								.getOwlIntersectionOf();
						if (intersectionOf == null && clazz.getURI() != null) {
							rangeClasses.add((IClass) clazz);
						}
					} else {
						queue.addAll(unionOf);
					}
				}

				KommaUtil.removeSuperClasses(rangeClasses);
				for (IReference typeClass : ((IResource) object).getRdfTypes()) {
					if (rangeClasses.contains(typeClass)) {
						return true;
					}
				}
				return false;
			}

			it.close();
		}

		return isRangeCompatible(object);
	}

	@Override
	public boolean isRangeCompatible(Object object) {
		if (getBehaviourDelegate() instanceof DatatypeProperty) {
			// TODO further tests in this case
			return true;
		}
		if (object instanceof IReference) {
			IQuery<?> query = getEntityManager().createQuery(IS_RANGE_INSTANCE);
			query.setParameter("property", getBehaviourDelegate());
			query.setParameter("object", object);

			return query.getBooleanResult();
		}

		return false;
	}

	@Override
	public IExtendedIterator<? extends IClass> getNamedRanges(
			IResource subject, boolean direct) {
		// query can be optimized if OWL-inferencing is supported
		if (getEntityManager().getInferencing().doesOWL()) {
			String query = PREFIX
					+ "SELECT DISTINCT ?resultR WHERE {"
					+ "	?o a ?c ."
					+ "	?c rdfs:subClassOf ?restriction ."
					+ "	?restriction owl:onProperty ?p ."
					+ "	{?restriction owl:allValuesFrom ?r} UNION {?restriction owl:someValuesFrom ?r} ."
					+ "	OPTIONAL {"
					+ "		?c rdfs:subClassOf [ owl:onProperty ?subP; owl:allValuesFrom ?otherR ] . " //
					+ "		?subP rdfs:subPropertyOf ?p ."
					+ "		?otherR rdfs:subClassOf ?r" //
					+ "		FILTER (isIRI(?otherR) && (?subP != ?p || ?otherR != ?r))" //
					+ "	}" //
					+ "	OPTIONAL {"
					+ "		?c rdfs:subClassOf [ owl:onProperty ?subP; owl:someValueFrom ?otherR ] . " //
					+ "		?subP rdfs:subPropertyOf ?p ."
					+ "		?otherR rdfs:subClassOf ?r" //
					+ "		FILTER (isIRI(?otherR) && (?subP != ?p || ?otherR != ?r))" //
					+ "	}" //
					+ (!direct ? "	?resultR rdfs:subClassOf ?r ." : "")
					+ "	FILTER (! bound(?otherR) && isIRI(?resultR) && ?resultR != owl:Nothing)" //
					+ "	OPTIONAL {" //
					+ "		?resultR komma:isAbstract ?abstract" //
					+ "	} ." //
					+ "	FILTER(!bound(?abstract)) " //
					+ "}";

			return subject.getEntityManager().createQuery(query)
					.setParameter("o", subject).setParameter("p", this)
					.evaluate(IClass.class);
		}

		String query = PREFIX
				+ "SELECT DISTINCT ?r WHERE {"
				+ "?o a ?c . ?c rdfs:subClassOf ?restriction . ?restriction owl:onProperty ?p . "
				+ "{{?restriction owl:allValuesFrom ?r} UNION {?restriction owl:someValuesFrom ?r} OPTIONAL {?r owl:intersectionOf ?x} FILTER (!bound(?x))}"
				+ "}";

		IExtendedIterator<? extends IClass> it = subject.getEntityManager()
				.createQuery(query).setParameter("o", subject)
				.setParameter("p", getBehaviourDelegate())
				.evaluate(IClass.class);

		if (it.hasNext()) {
			Set<IClass> namedRangeClasses = new HashSet<IClass>();

			Set<IReference> seenClasses = new HashSet<IReference>();
			Queue<net.enilink.vocab.owl.Class> queue = new LinkedList<net.enilink.vocab.owl.Class>(
					it.toSet());
			while (!queue.isEmpty()) {
				net.enilink.vocab.owl.Class clazz = queue.remove();
				if (!seenClasses.add(clazz)) {
					continue;
				}

				List<net.enilink.vocab.owl.Class> unionOf = clazz
						.getOwlUnionOf();

				if (unionOf == null) {
					List<net.enilink.vocab.owl.Class> intersectionOf = clazz
							.getOwlIntersectionOf();
					if (intersectionOf == null && clazz.getURI() != null) {
						namedRangeClasses.add((IClass) clazz);
					}
				} else {
					queue.addAll(unionOf);
				}
			}

			KommaUtil.removeSuperClasses(namedRangeClasses);
			if (!direct) {
				KommaUtil.unfoldSubClasses(namedRangeClasses);
			}
			return WrappedIterator.create(namedRangeClasses.iterator());
		}

		it.close();

		return getRanges(direct).filterDrop(new Filter<IClass>() {
			@Override
			public boolean accept(IClass c) {
				return c.getURI() == null;
			}
		});
	}

	@Override
	public boolean hasListRange() {
		if (getBehaviourDelegate() instanceof DatatypeProperty) {
			return false;
		}
		IQuery<?> query = getEntityManager().createQuery(HAS_LIST_RANGE_QUERY);
		query.setParameter("property", this);

		return query.getBooleanResult();
	}

	@Override
	public IExtendedIterator<? extends IClass> getRanges(boolean direct) {
		return getEntityManager()
				.createQuery(direct ? DIRECT_RANGE_QUERY : RANGE_QUERY)
				.setParameter("property", getBehaviourDelegate())
				.evaluate(IClass.class);
	}

	public boolean isMany(IReference subject) {
		if (getBehaviourDelegate() instanceof FunctionalProperty) {
			return hasListRange();
		}
		if (subject != null) {
			if (((IResource) getEntityManager().find(subject))
					.getApplicableCardinality(getBehaviourDelegate())
					.getSecond() <= 1) {
				return hasListRange();
			}
		}
		return true;
	}
}
