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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.enilink.composition.properties.komma.KommaPropertySet;
import net.enilink.composition.traits.Behaviour;

import com.google.inject.Inject;
import com.google.inject.Injector;

import net.enilink.commons.iterator.ConvertingIterator;
import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.commons.iterator.UniqueExtendedIterator;
import net.enilink.vocab.owl.FunctionalProperty;
import net.enilink.komma.em.internal.behaviours.OrderedPropertySet;
import net.enilink.komma.results.ResultDescriptor;
import net.enilink.komma.core.IBindings;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IQuery;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IResultDescriptor;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.IValue;
import net.enilink.komma.core.Statement;
import net.enilink.komma.util.KommaUtil;
import net.enilink.komma.util.Pair;

public abstract class ResourceSupport extends BehaviorBase implements
		IResource, Behaviour<IResource> {
	class PropertyInfo {
		private IReference property;
		private KommaPropertySet<Object> propertySet;
		private Boolean single;

		PropertyInfo(IReference property) {
			this.property = getEntityManager().find(property);
		}

		KommaPropertySet<Object> getPropertySet() {
			if (propertySet != null) {
				return propertySet;
			}
			if (property instanceof IProperty
					&& ((IProperty) property).isOrderedContainment()) {
				propertySet = new OrderedPropertySet<Object>(
						getBehaviourDelegate(), property);
			} else {
				propertySet = new KommaPropertySet<Object>(
						getBehaviourDelegate(), property);
			}
			injector.injectMembers(propertySet);
			return propertySet;
		}

		boolean isSingle() {
			if (single == null) {
				// the following code checks if property is functional
				// the property is functional if it is an FunctionalProperty or
				// its maximum cardinality is 1
				single = this.property instanceof FunctionalProperty;
				if (!single) {
					Pair<Integer, Integer> cardinality = getApplicableCardinality(property);
					single = cardinality.getSecond() <= 1;
				}
			}
			return single;
		}
	}

	private static final String HAS_APPLICABLE_PROPERTY = PREFIX //
			+ "ASK { " //
			+ "{?property a owl:AnnotationProperty "
			+ "OPTIONAL {?property rdfs:domain ?domain . "
			+ "		OPTIONAL {?resurce a ?class . ?class rdfs:subClassOf ?domain}} FILTER (!bound(?domain) || bound(?class))} UNION "
			+ "{?property rdfs:subPropertyOf rdf:type} UNION "
			+ "{?resource a ?class ." // given resource has type class
			+ "{{?property rdfs:domain ?class} UNION" //
			+ "{?class rdfs:subClassOf ?restriction . ?restriction owl:onProperty ?property}}}" //
			+ "}";

	private static final String SELECT_APPLICABLE_CARDINALITY = PREFIX //
			+ "SELECT DISTINCT ?min ?max " //
			+ "WHERE { " //
			+ "?resource a ?class ." // given resource has type class
			+ "?class rdfs:subClassOf ?restriction ." // class has a local
			// restriction
			+ "?restriction owl:onProperty ?property ." // for given property
			+ "{"
			+ "OPTIONAL {?restriction owl:minCardinality ?min }"
			+ "OPTIONAL {?restriction owl:maxCardinality ?max }"
			+ "OPTIONAL {"
			+ "?restriction owl:cardinality ?min ."
			+ "?restriction owl:cardinality ?max ."
			+ "}"
			// FIXME
			// preliminary support for qualified cardinality restrictions
			+ "} UNION {"
			+ "OPTIONAL {?restriction owl:minQualifiedCardinality ?min }"
			+ "OPTIONAL {?restriction owl:maxQualifiedCardinality ?max }"
			+ "OPTIONAL {"
			+ "?restriction owl:qualifiedCardinality ?min ."
			+ "?restriction owl:qualifiedCardinality ?max ." + //
			"}}} ORDER BY DESC(?min) ?max";

	private static final String SELECT_APPLICABLE_CHILD_PROPERTIES = PREFIX //
			+ "SELECT DISTINCT ?property " //
			+ "WHERE { " //
			// select potential child properties
			+ "{" //
			+ "?resource a ?class ." //
			+ "?class rdfs:subClassOf [owl:onProperty ?property] ." //
			+ "?property rdfs:subPropertyOf komma:contains ." //
			+ "OPTIONAL {" //
			+ "    ?otherProperty rdfs:subPropertyOf ?property ." //
			+ "    ?class rdfs:subClassOf [owl:onProperty ?otherProperty]" //
			+ "	FILTER (?property != ?otherProperty)" //
			+ "} " //
			+ "FILTER (! bound(?otherProperty))" //
			// select already used child properties
			+ "} UNION {" //
			+ "    ?resource ?property ?someObject ." //
			+ "    ?property rdfs:subPropertyOf komma:contains ." //
			+ "    OPTIONAL {" //
			+ "        ?otherProperty rdfs:subPropertyOf ?property ." //
			+ "        ?resource ?otherProperty ?someObject ." //
			+ "        FILTER (?property != ?otherProperty)" //
			+ "    }" //
			+ "    FILTER (! bound(?otherProperty))" //
			+ "}" //
			+ "} ORDER BY ?property";

	private static final String SELECT_APPLICABLE_PROPERTIES = PREFIX //
			+ "SELECT DISTINCT ?property " //
			+ "WHERE { " //
			+ "{?property a owl:AnnotationProperty OPTIONAL {?property rdfs:domain ?class} FILTER (!bound(?class))} UNION "
			+ "{?property rdfs:subPropertyOf rdf:type} UNION "
			+ "{ ?resource a ?class ." // given resource has type class
			+ "{{?property rdfs:domain ?class} UNION" //
			+ "{?class rdfs:subClassOf ?restriction . ?restriction owl:onProperty ?property}}" //

			// exclude properties that can not be applied to
			// the actual types of the subject
			// + "OPTIONAL {" //
			// + "		?property rdfs:domain ?someDomain ." //
			// + "		OPTIONAL {"
			// + " 		?subject a ?someDomain ."
			// + " 		?subject a ?matchDummy ."
			// + "		}"
			// + "		FILTER (! bound(?matchDummy))"
			// + "}"
			// + "FILTER (! bound(?someDomain))"

			+ "}} ORDER BY ?property";

	private static final String SELECT_CONTAINER = PREFIX //
			+ "SELECT ?container WHERE { ?container komma:contains ?obj . }";

	private static final String SELECT_PROPERTY_OBJECTS = PREFIX //
			+ "SELECT ?obj WHERE { ?subj ?pred ?obj . }";

	public static final IResultDescriptor<IClass> DIRECT_CLASSES_DESC() {
		return new ResultDescriptor<IClass>(SELECT_DIRECT_CLASSES(false),
				"urn:komma:directClasses", "class", "resource")
				.bindResultType(IClass.class);
	}

	public static final IResultDescriptor<IClass> DIRECT_NAMED_CLASSES_DESC() {
		return new ResultDescriptor<IClass>(SELECT_DIRECT_CLASSES(true),
				"urn:komma:directNamedClasses", "class", "resource")
				.bindResultType(IClass.class);
	}

	private static final String SELECT_CLASSES(boolean named) {
		return PREFIX //
				+ "SELECT ?class WHERE {"
				+ "?resource a ?class"
				+ (named ? " FILTER isIRI(?class)" : "") + "}";
	}

	private static final String SELECT_DIRECT_CLASSES(boolean named) {
		return PREFIX //
				+ "SELECT ?class WHERE {" //
				+ "?resource a ?class " //
				+ (named ? "FILTER (isIRI(?class)) " : "") //
				+ "FILTER NOT EXISTS {?resource a ?otherClass . ?otherClass rdfs:subClassOf ?class "
				+ "FILTER (" //
				+ (named ? "isIRI(?otherClass) && " : "") //
				+ "?otherClass != ?class)" //
				+ "		FILTER NOT EXISTS {?class rdfs:subClassOf ?otherClass}" //
				+ "} " //
				+ "FILTER NOT EXISTS {?resource a ?otherClass . FILTER ("
				+ (named ? "isIRI(?otherClass) && " : "")
				+ "(?class = owl:Thing || ?class = rdfs:Resource) && ?otherClass != ?class)}" //
				+ "}";
	}

	@Inject
	private Injector injector;

	private ConcurrentHashMap<IReference, PropertyInfo> properties;

	@Override
	public void addProperty(IReference property, Object obj) {
		getEntityManager().add(new Statement(this, property, obj));
	}

	private PropertyInfo ensurePropertyInfo(IReference property) {
		PropertyInfo propertyInfo = getProperties().get(property);
		if (propertyInfo == null) {
			propertyInfo = new PropertyInfo(property);
			getProperties().put(property, propertyInfo);
		}
		return propertyInfo;
	}

	@Override
	public Object get(IReference property) {
		PropertyInfo propertyInfo = ensurePropertyInfo(property);
		if (propertyInfo.isSingle()) {
			return propertyInfo.getPropertySet().getSingle();
		}
		return propertyInfo.getPropertySet();
	}

	@Override
	public Set<Object> getAsSet(IReference property) {
		return ensurePropertyInfo(property).getPropertySet();
	}

	@Override
	public Pair<Integer, Integer> getApplicableCardinality(IReference property) {
		IQuery<?> query = getEntityManager().createQuery(
				SELECT_APPLICABLE_CARDINALITY);
		query.setParameter("resource", getBehaviourDelegate());
		query.setParameter("property", property);

		int min = 0;
		int max = Integer.MAX_VALUE;
		for (//
		@SuppressWarnings("rawtypes")
		Iterator<IBindings> it = query.evaluate(IBindings.class); it.hasNext();) {
			IBindings<?> values = it.next();

			if (values.get("min") instanceof Number) {
				min = Math.max(min, ((Number) values.get("min")).intValue());
			}
			if (values.get("max") instanceof Number) {
				max = Math.min(max, ((Number) values.get("max")).intValue());
			}
		}

		// if min is greater than max, then max = min
		max = Math.max(min, max);
		// handle functional properties
		if (max > 1
				&& getEntityManager().find(property) instanceof FunctionalProperty) {
			max = 1;
		}

		return new Pair<Integer, Integer>(min, max);
	}

	@Override
	public IExtendedIterator<IProperty> getApplicableChildProperties() {
		IQuery<?> query = getEntityManager().createQuery(
				SELECT_APPLICABLE_CHILD_PROPERTIES);
		query.setParameter("resource", this);

		return query.evaluate(IProperty.class);
	}

	@Override
	public int getCardinality(IReference property) {
		int count = 0;
		for (Iterator<?> it = getPropertyValues(property, true); it.hasNext();) {
			it.next();

			count++;
		}
		return count;
	}

	@Override
	public IExtendedIterator<IClass> getClasses() {
		return getClasses(false);
	}

	@Override
	public IExtendedIterator<IClass> getClasses(boolean includeInferred) {
		IQuery<?> query = getEntityManager().createQuery(SELECT_CLASSES(false),
				includeInferred);
		query.setParameter("resource", getBehaviourDelegate());

		return query.evaluate(IClass.class);
	}

	@Override
	public IResource getContainer() {
		IQuery<?> query = getEntityManager().createQuery(SELECT_CONTAINER);
		query.setParameter("obj", this);
		IExtendedIterator<?> it = query.evaluate();
		try {
			return it.hasNext() ? (IResource) it.next() : null;
		} finally {
			it.close();
		}
	}

	@Override
	public IExtendedIterator<IClass> getDirectClasses() {
		return getEntityManager()
				.createQuery(DIRECT_CLASSES_DESC().toQueryString())
				.setParameter("resource", getBehaviourDelegate())
				.evaluate(IClass.class);
	}

	@Override
	public IExtendedIterator<IClass> getDirectNamedClasses() {
		return getEntityManager()
				.createQuery(DIRECT_NAMED_CLASSES_DESC().toQueryString())
				.setParameter("resource", getBehaviourDelegate())
				.evaluate(IClass.class);
	}

	@Override
	public IExtendedIterator<IClass> getNamedClasses() {
		IQuery<?> query = getEntityManager().createQuery(SELECT_CLASSES(true));
		query.setParameter("resource", getBehaviourDelegate());

		return query.evaluate(IClass.class);
	}

	private synchronized Map<IReference, PropertyInfo> getProperties() {
		if (properties == null) {
			properties = new ConcurrentHashMap<IReference, PropertyInfo>();
		}
		return properties;
	}

	private PropertyInfo getPropertyInfo(IReference property) {
		return getProperties().get(property);
	}

	@Override
	public IExtendedIterator<IStatement> getPropertyStatements(
			final IReference property, boolean includeInferred) {
		IExtendedIterator<IStatement> stmts = internalGetPropertyStmts(
				property, false, false, false);
		if (includeInferred) {
			stmts = UniqueExtendedIterator.create(stmts
					.andThen(internalGetPropertyStmts(property, false, false,
							true)));
		}
		return stmts;
	}

	@Override
	public IExtendedIterator<IStatement> getInversePropertyStatements(
			final IReference property, boolean filterSymmetric,
			boolean includeInferred) {
		IExtendedIterator<IStatement> stmts = internalGetPropertyStmts(
				property, true, filterSymmetric, false);
		if (includeInferred) {
			stmts = UniqueExtendedIterator.create(stmts
					.andThen(internalGetPropertyStmts(property, true,
							filterSymmetric, true)));
		}
		return stmts;
	}

	@Override
	public IExtendedIterator<IStatement> getInversePropertyStatements(
			final IReference property, boolean includeInferred) {
		return getInversePropertyStatements(property, false, includeInferred);
	}

	@Override
	public IExtendedIterator<IValue> getPropertyValues(IReference property,
			boolean includeInferred) {
		IQuery<IValue> query = getEntityManager().createQuery(
				SELECT_PROPERTY_OBJECTS, includeInferred).bindResultType(
				IValue.class);
		query.setParameter("subj", this);
		query.setParameter("pred", property);
		return query.evaluate();
	}

	@Override
	public IExtendedIterator<IProperty> getRelevantProperties() {
		IQuery<?> query = getEntityManager().createQuery(
				SELECT_APPLICABLE_PROPERTIES);
		query.setParameter("resource", this);

		return query.evaluate(IProperty.class);
	}

	@Override
	public boolean hasApplicableProperty(IReference property) {
		IQuery<?> query = getEntityManager().createQuery(
				HAS_APPLICABLE_PROPERTY);
		query.setParameter("resource", this);
		query.setParameter("property", property);

		return query.getBooleanResult();
	}

	@Override
	public boolean hasProperty(IReference property, Object obj,
			boolean includeInferred) {
		return getEntityManager()
				.createQuery("ASK { ?s ?p ?o }", includeInferred)
				.setParameter("s", this).setParameter("p", property)
				.setParameter("o", obj).getBooleanResult();
	}

	protected IExtendedIterator<IStatement> internalGetPropertyStmts(
			final IReference propertyRef, final boolean inverse,
			final boolean filterSymmetric, final boolean includeInferred) {
		final IEntity property = (propertyRef instanceof IEntity || propertyRef == null) ? (IEntity) propertyRef
				: getEntityManager().find(propertyRef);
		StringBuilder sb = new StringBuilder(PREFIX);
		sb.append("SELECT ");
		if (property == null) {
			sb.append("?pred ");
		}
		String targetVar = inverse ? "?subj" : "?obj";
		sb.append(targetVar);
		sb.append(" WHERE { ?subj ?pred ?obj . ");
		if (filterSymmetric) {
			String selfVar = inverse ? "?obj" : "?subj";
			sb.append("FILTER NOT EXISTS { ").append(selfVar).append(" ?pred ")
					.append(selfVar).append(" }");
		}
		sb.append(" }");

		IQuery<?> query = getEntityManager().createQuery(sb.toString(),
				includeInferred);
		query.setParameter("pred", property);
		if (!inverse) {
			query.setParameter("subj", this);
			query.bindResultType("obj", IValue.class);
			return new ConvertingIterator<Object, IStatement>(query.evaluate()) {
				@Override
				protected IStatement convert(Object value) {
					if (value instanceof IBindings<?>) {
						IBindings<?> values = (IBindings<?>) value;
						return new Statement(getBehaviourDelegate(),
								property != null ? property
										: (IReference) values.get("pred"),
								values.get("obj"), includeInferred);
					}
					return new Statement(getBehaviourDelegate(), property,
							value, includeInferred);
				}
			};
		} else {
			query.setParameter("obj", this);
			query.bindResultType("subj", IValue.class);
			return new ConvertingIterator<Object, IStatement>(query.evaluate()) {
				@Override
				protected IStatement convert(Object value) {
					if (value instanceof IBindings<?>) {
						IBindings<?> values = (IBindings<?>) value;
						return new Statement((IReference) values.get("subj"),
								property != null ? property
										: (IReference) values.get("pred"),
								getBehaviourDelegate(), includeInferred);
					}
					return new Statement((IReference) value, property,
							getBehaviourDelegate(), includeInferred);
				}
			};
		}
	}

	@Override
	public boolean isOntLanguageTerm() {
		net.enilink.komma.core.URI uri = getURI();
		return uri != null
				&& KommaUtil.isW3CLanguageTerm(uri.namespace().toString());
	}

	@Override
	public boolean isPropertySet(IReference property, boolean includeInferred) {
		return hasProperty(property, null, includeInferred);
	}

	public void refresh(IReference property) {
		PropertyInfo propertyInfo = getPropertyInfo(property);
		if (propertyInfo != null) {
			propertyInfo.getPropertySet().refresh();
		}
	}

	@Override
	public void removeProperty(IReference property) {
		getEntityManager().remove(new Statement(this, property, null));
	}

	@Override
	public void removeProperty(IReference property, Object value) {
		getEntityManager().remove(new Statement(this, property, value));
	}

	@SuppressWarnings("unchecked")
	public void set(IReference property, Object value) {
		PropertyInfo propertyInfo = ensurePropertyInfo(property);
		boolean functional = propertyInfo.isSingle();

		if (value == null) {
			propertyInfo.getPropertySet().clear();
		} else if (!functional && value instanceof Collection<?>) {
			if (value instanceof Set<?>) {
				propertyInfo.getPropertySet().setAll((Set<Object>) value);
			} else {
				propertyInfo.getPropertySet().setAll(
						(new HashSet<Object>((Collection<?>) value)));
			}
		} else {
			propertyInfo.getPropertySet().setSingle(value);
		}
	}

	@Override
	public void refresh() {
		// reset all cached properties
		properties = null;
	}
}
