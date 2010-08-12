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
import net.enilink.komma.internal.sesame.behaviours.OrderedSesamePropertySet;
import net.enilink.komma.results.ResultDescriptor;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IQuery;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IResultDescriptor;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.IValue;
import net.enilink.komma.core.Statement;
import net.enilink.komma.sesame.ISesameEntity;
import net.enilink.komma.sesame.ISesameResourceAware;
import net.enilink.komma.util.KommaUtil;
import net.enilink.komma.util.Pair;

public abstract class ResourceSupport extends BehaviorBase implements
		IResource, Behaviour<IResource> {
	class PropertyInfo {
		private IReference property;
		private KommaPropertySet<Object> propertySet;
		private boolean single;

		PropertyInfo(IReference property) {
			this.property = getKommaManager().find(property);

			// the following code checks if property is functional
			// the property is functional if it is an FunctionalProperty or
			// its maximum cardinality is 1
			single = this.property instanceof FunctionalProperty;
			if (!single) {
				Pair<Integer, Integer> cardinality = getApplicableCardinality(property);
				single = cardinality.getSecond() <= 1;
			}
		}

		KommaPropertySet<Object> getPropertySet() {
			if (propertySet != null) {
				return propertySet;
			}

			if (property instanceof IProperty
					&& ((IProperty) property).isOrderedContainment()) {
				propertySet = new OrderedSesamePropertySet<Object>(
						(ISesameEntity) getBehaviourDelegate(), property);
			} else {
				propertySet = new KommaPropertySet<Object>(
						(ISesameEntity) getBehaviourDelegate(), property);
			}

			injector.injectMembers(propertySet);

			return propertySet;
		}

		boolean isSingle() {
			return single;
		}
	}

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

	@Inject
	private Injector injector;

	private ConcurrentHashMap<IReference, PropertyInfo> properties;

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

	private static final String SELECT_APPLICABLE_PROPERTIES = PREFIX //
			+ "SELECT DISTINCT ?property " //
			+ "WHERE { " //
			+ "{?property a owl:AnnotationProperty OPTIONAL {?property rdfs:domain ?class} FILTER (!bound(?class))} UNION "
			+ "{?property rdfs:subPropertyOf rdf:type} UNION "
			+ "{ ?resource a ?class ." // given resource has type class
			+ "{{?property rdfs:domain ?class} UNION" //
			+ "{?class rdfs:subClassOf ?restriction . ?restriction owl:onProperty ?property}}" //
			+ "}} ORDER BY ?property";

	private static final String SELECT_PROPERTIES_AND_OBJECTS = PREFIX //
			+ "SELECT ?pred ?obj WHERE { ?subj ?pred ?obj . }";

	private static final String SELECT_PROPERTY_OBJECTS = PREFIX //
			+ "SELECT ?obj WHERE { ?subj ?pred ?obj . }";

	private static final String SELECT_CLASSES(boolean named) {
		return PREFIX //
				+ "SELECT ?class WHERE {"
				+ "?resource a ?class"
				+ (named ? " FILTER isIRI(?class)" : "") + "}";
	}

	private static final String SELECT_DIRECT_CLASSES(boolean named) {
		return PREFIX //
				+ "SELECT ?class WHERE {" //
				+ "?resource a ?class ." //
				+ "OPTIONAL {?resource a ?otherClass . ?otherClass rdfs:subClassOf ?class FILTER ("
				+ (named ? "isIRI(?otherClass) && " : "")
				+ "?otherClass != ?class)}" //
				+ "OPTIONAL {?resource a ?otherClass . FILTER ("
				+ (named ? "isIRI(?otherClass) && " : "")
				+ "?class = owl:Thing && ?otherClass != ?class)}" //
				+ "FILTER ("
				+ (named ? "isIRI(?class) && " : "")
				+ "!bound(?otherClass))}";
	}

	@Override
	public void addProperty(IReference property, Object obj) {
		getKommaManager().add(new Statement(this, property, obj));
	}

	private PropertyInfo ensurePropertyInfo(IReference property) {
		PropertyInfo propertyInfo = getProperties().get(property);
		if (propertyInfo == null) {
			propertyInfo = new PropertyInfo(property);
			getProperties().put(property, propertyInfo);
		}
		return propertyInfo;
	}

	public Object get(IReference property) {
		PropertyInfo propertyInfo = ensurePropertyInfo(property);
		if (propertyInfo.isSingle()) {
			return ensurePropertyInfo(property).getPropertySet().getSingle();
		}
		return ensurePropertyInfo(property).getPropertySet();
	}

	@Override
	public Pair<Integer, Integer> getApplicableCardinality(IReference property) {
		IQuery<?> query = getKommaManager().createQuery(
				SELECT_APPLICABLE_CARDINALITY);
		query.setParameter("resource", getBehaviourDelegate());
		query.setParameter("property", property);

		int min = 0;
		int max = Integer.MAX_VALUE;
		for (Iterator<?> it = query.evaluate(); it.hasNext();) {
			Object[] values = (Object[]) it.next();

			if (values[0] instanceof Number) {
				min = Math.max(min, ((Number) values[0]).intValue());
			}
			if (values[1] instanceof Number) {
				max = Math.min(max, ((Number) values[1]).intValue());
			}
		}

		// if min is greater than max, then max = min
		max = Math.max(min, max);

		return new Pair<Integer, Integer>(min, max);
	}

	@Override
	public IExtendedIterator<IProperty> getApplicableProperties() {
		IQuery<?> query = getKommaManager().createQuery(
				SELECT_APPLICABLE_PROPERTIES);
		query.setParameter("resource", getBehaviourDelegate());

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
		IQuery<?> query = getKommaManager().createQuery(SELECT_CLASSES(false));
		query.setParameter("resource", getBehaviourDelegate())
				.setIncludeInferred(includeInferred);

		return query.evaluate(IClass.class);
	}

	@Override
	public IExtendedIterator<IClass> getDirectClasses() {
		return getKommaManager()
				.createQuery(DIRECT_CLASSES_DESC().toQueryString())
				.setParameter("resource", getBehaviourDelegate())
				.setIncludeInferred(true).evaluate(IClass.class);
	}

	@Override
	public IExtendedIterator<IClass> getDirectNamedClasses() {
		return getKommaManager()
				.createQuery(DIRECT_NAMED_CLASSES_DESC().toQueryString())
				.setParameter("resource", getBehaviourDelegate())
				.setIncludeInferred(true).evaluate(IClass.class);
	}

	@Override
	public IExtendedIterator<IClass> getNamedClasses() {
		IQuery<?> query = getKommaManager().createQuery(SELECT_CLASSES(true));
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
		IEntity propertyEntity = (property instanceof IEntity) ? (IEntity) property
				: (IEntity) getSesameManager().getInstance(
						((ISesameResourceAware) property).getSesameResource(),
						null);

		IExtendedIterator<IStatement> stmts = internalGetPropertyStmts(
				propertyEntity, false);
		if (includeInferred) {
			stmts = UniqueExtendedIterator.create(stmts
					.andThen(internalGetPropertyStmts(propertyEntity, true)));
		}

		return stmts;
	}

	@Override
	public IExtendedIterator<IValue> getPropertyValues(IReference property,
			boolean includeInferred) {
		IQuery<IValue> query = getKommaManager().createQuery(
				SELECT_PROPERTY_OBJECTS).bindResultType(IValue.class);
		query.setParameter("subj", this);
		query.setParameter("pred", property);
		query.setIncludeInferred(includeInferred);
		return query.evaluate();
	}

	@Override
	public boolean hasApplicableProperty(IReference property) {
		IQuery<?> query = getKommaManager()
				.createQuery(HAS_APPLICABLE_PROPERTY);
		query.setParameter("resource", this);
		query.setParameter("property", property);

		return query.getBooleanResult();
	}

	@Override
	public boolean hasProperty(IReference property, Object obj,
			boolean includeInferred) {
		return getKommaManager().createQuery("ASK {?s ?p ?o}")
				.setParameter("s", this).setParameter("p", property)
				.setParameter("o", obj).setIncludeInferred(includeInferred)
				.getBooleanResult();
	}

	protected IExtendedIterator<IStatement> internalGetPropertyStmts(
			final IEntity property, final boolean includeInferred) {
		IQuery<?> query = getKommaManager().createQuery(
				property != null ? SELECT_PROPERTY_OBJECTS
						: SELECT_PROPERTIES_AND_OBJECTS);
		query.setParameter("subj", this);
		query.setParameter("pred", property);
		query.setIncludeInferred(includeInferred);
		query.bindResultType(property != null ? 0 : 1, IValue.class);

		return new ConvertingIterator<Object, IStatement>(query.evaluate()) {
			@Override
			protected IStatement convert(Object value) {
				if (value instanceof Object[]) {
					Object[] values = (Object[]) value;
					return new Statement(getBehaviourDelegate(),
							(IReference) values[0], values[1]);
				}
				return new Statement(getBehaviourDelegate(), property, value);
			}
		};
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
		getKommaManager().remove(new Statement(this, property, null));
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
}
