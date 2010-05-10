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

import net.enilink.composition.properties.sesame.PropertySetModifier;
import net.enilink.composition.properties.sesame.SesamePropertySet;
import net.enilink.composition.traits.Behaviour;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQuery;
import org.openrdf.store.StoreException;

import com.google.inject.Inject;
import com.google.inject.Injector;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.commons.iterator.UniqueExtendedIterator;
import net.enilink.vocab.owl.FunctionalProperty;
import net.enilink.komma.common.util.URIUtil;
import net.enilink.komma.internal.sesame.behaviours.OrderedSesamePropertySet;
import net.enilink.komma.results.ResultDescriptor;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IQuery;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IResultDescriptor;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.core.Statement;
import net.enilink.komma.core.URIImpl;
import net.enilink.komma.sesame.ISesameEntity;
import net.enilink.komma.sesame.ISesameManager;
import net.enilink.komma.sesame.ISesameResourceAware;
import net.enilink.komma.sesame.iterators.SesameIterator;
import net.enilink.komma.util.KommaUtil;
import net.enilink.komma.util.Pair;

public abstract class ResourceSupport extends BehaviorBase implements
		IResource, Behaviour<IResource> {
	class PropertyInfo {
		private IReference property;
		private SesamePropertySet<Object> sesamePropertySet;
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

		SesamePropertySet<Object> getSesamePropertySet() {
			if (sesamePropertySet != null) {
				return sesamePropertySet;
			}

			PropertySetModifier modifier = new PropertySetModifier(URIUtil
					.toSesameUri(property.getURI()));

			if (property instanceof IProperty
					&& ((IProperty) property).isOrderedContainment()) {
				sesamePropertySet = new OrderedSesamePropertySet<Object>(
						(ISesameEntity) getBehaviourDelegate(), modifier);
			} else {
				sesamePropertySet = new SesamePropertySet<Object>(
						(ISesameEntity) getBehaviourDelegate(), modifier);
			}

			injector.injectMembers(sesamePropertySet);

			return sesamePropertySet;
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
			+ "{?property a owl:AnnotationProperty OPTIONAL {?property rdfs:domain ?class} FILTER (!bound(?class))} UNION "
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
	public void addProperty(IReference property, IResource obj) {
		try {
			getSesameManager().getConnection().add(getSesameResource(),
					(URI) getSesameResource(property), getSesameResource(obj));
		} catch (StoreException e) {
			throw new KommaException(e);
		}
	}

	@Override
	public void addProperty(IReference property, Object obj)
			throws KommaException {
		if (obj instanceof Value) {
			addProperty(property, (Value) obj);
		} else {
			addProperty(property, getSesameManager().getValue(obj));
		}
	}

	@Override
	public void addProperty(IReference property, Value obj)
			throws KommaException {
		try {
			getSesameManager().getConnection().add(getSesameResource(),
					(URI) getSesameResource(property), obj);
		} catch (StoreException e) {
			throw new KommaException(e);
		}
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
			return ensurePropertyInfo(property).getSesamePropertySet()
					.getSingle();
		}
		return ensurePropertyInfo(property).getSesamePropertySet();
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
		return getKommaManager().createQuery(
				DIRECT_CLASSES_DESC().toQueryString()).setParameter("resource",
				getBehaviourDelegate()).setIncludeInferred(true).evaluate(
				IClass.class);
	}

	@Override
	public IExtendedIterator<IClass> getDirectNamedClasses() {
		return getKommaManager().createQuery(
				DIRECT_NAMED_CLASSES_DESC().toQueryString()).setParameter(
				"resource", getBehaviourDelegate()).setIncludeInferred(true)
				.evaluate(IClass.class);
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
						((ISesameResourceAware) property).getSesameResource());

		IExtendedIterator<IStatement> stmts = internalGetPropertyStmts(
				propertyEntity, false);
		if (includeInferred) {
			stmts = UniqueExtendedIterator.create(stmts
					.andThen(internalGetPropertyStmts(propertyEntity, true)));
		}

		return stmts;
	}

	@Override
	public IExtendedIterator<?> getPropertyValues(IReference property,
			boolean includeInferred) {
		try {
			TupleQuery query = getSesameManager().getConnection()
					.prepareTupleQuery(SELECT_PROPERTY_OBJECTS);
			query.setBinding("subj", getSesameResource());
			query.setBinding("pred",
					property != null ? getSesameResource(property) : null);
			query.setIncludeInferred(includeInferred);

			return new SesameIterator<BindingSet, Object>(query.evaluate()) {
				@Override
				protected Object convert(BindingSet value) throws Exception {
					Value object = value.getValue("obj");
					if (object instanceof Resource) {
						return getSesameManager().getInstance(object);
					} else {
						net.enilink.komma.core.URI uri = (((Literal) object)
								.getDatatype() != null) ? URIImpl
								.createURI(((Literal) object).getDatatype()
										.toString()) : null;
						return getKommaManager().createLiteral(
								getSesameManager().getInstance(object), uri,
								((Literal) object).getLanguage());
					}
				}
			};
		} catch (Exception e) {
			throw new KommaException(e);
		}
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
	public boolean hasProperty(IReference property, IResource obj,
			boolean includeInferred) {
		try {
			return getSesameManager().getConnection().hasMatch(
					getSesameResource(), (URI) getSesameResource(property),
					getSesameResource(obj), includeInferred);
		} catch (StoreException e) {
			throw new KommaException(e);
		}
	}

	@Override
	public boolean hasProperty(IReference property, Object obj,
			boolean includeInferred) {
		return hasProperty(property, obj, includeInferred);
	}

	@Override
	public boolean hasProperty(IReference property, Value obj,
			boolean includeInferred) {
		try {
			return getSesameManager().getConnection().hasMatch(
					getSesameResource(), (URI) getSesameResource(property),
					obj, includeInferred);
		} catch (StoreException e) {
			throw new KommaException(e);
		}
	}

	protected IExtendedIterator<IStatement> internalGetPropertyStmts(
			final IEntity property, final boolean includeInferred) {
		try {
			TupleQuery query = getSesameManager().getConnection()
					.prepareTupleQuery(
							property != null ? SELECT_PROPERTY_OBJECTS
									: SELECT_PROPERTIES_AND_OBJECTS);
			query.setBinding("subj", getSesameResource());
			query.setBinding("pred",
					property != null ? getSesameResource(property) : null);
			query.setIncludeInferred(includeInferred);

			final ISesameManager manager = getSesameManager();
			return new SesameIterator<BindingSet, IStatement>(query.evaluate()) {
				@Override
				protected IStatement convert(BindingSet value) throws Exception {
					Object object = value.getValue("obj");
					if (object instanceof Resource) {
						object = manager.getInstance((Resource) object);
					} else {
						net.enilink.komma.core.URI uri = (((Literal) object)
								.getDatatype() != null) ? URIImpl
								.createURI(((Literal) object).getDatatype()
										.toString()) : null;
						object = manager.createLiteral(manager
								.getInstance((Value) object), uri,
								((Literal) object).getLanguage());
					}
					return new Statement(getBehaviourDelegate(),
							property != null ? property : (IEntity) manager
									.getInstance(value.getValue("pred")),
							object, includeInferred);
				}
			};
		} catch (Exception e) {
			throw new KommaException(e);
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
		try {
			return getSesameManager()
					.getConnection()
					.hasMatch(
							getSesameResource(),
							(URI) ((ISesameResourceAware) property)
									.getSesameResource(), null, includeInferred);
		} catch (StoreException e) {
			throw new KommaException(e);
		}
	}

	public void refresh(IReference property) {
		PropertyInfo propertyInfo = getPropertyInfo(property);
		if (propertyInfo != null) {
			propertyInfo.getSesamePropertySet().refresh();
		}
	}

	@Override
	public void removeProperty(IReference property) {
		try {
			getSesameManager().getConnection().removeMatch(getSesameResource(),
					(URI) getSesameResource(property), null);
		} catch (StoreException e) {
			throw new KommaException(e);
		}
	}

	@Override
	public void removeProperty(IReference property, IResource obj) {
		try {
			getSesameManager().getConnection().removeMatch(getSesameResource(),
					(URI) getSesameResource(property), getSesameResource(obj));
		} catch (StoreException e) {
			throw new KommaException(e);
		}
	}

	@Override
	public void removeProperty(IReference property, Object obj)
			throws KommaException {
		if (obj instanceof Value) {
			removeProperty(property, (Value) obj);
		} else {
			removeProperty(property, getSesameManager().getValue(obj));
		}
	}

	@Override
	public void removeProperty(IReference property, Value obj)
			throws KommaException {
		try {
			getSesameManager().getConnection().removeMatch(getSesameResource(),
					(URI) getSesameResource(property), obj);
		} catch (StoreException e) {
			throw new KommaException(e);
		}
	}

	@SuppressWarnings("unchecked")
	public void set(IReference property, Object value) {
		PropertyInfo propertyInfo = ensurePropertyInfo(property);
		boolean functional = propertyInfo.isSingle();

		if (value == null) {
			propertyInfo.getSesamePropertySet().clear();
		} else if (!functional && value instanceof Collection<?>) {
			if (value instanceof Set<?>) {
				propertyInfo.getSesamePropertySet().setAll((Set<Object>) value);
			} else {
				propertyInfo.getSesamePropertySet().setAll(
						(new HashSet<Object>((Collection<?>) value)));
			}
		} else {
			propertyInfo.getSesamePropertySet().setSingle(value);
		}
	}
}
