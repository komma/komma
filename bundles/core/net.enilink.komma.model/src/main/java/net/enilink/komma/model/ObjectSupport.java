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
package net.enilink.komma.model;

import net.enilink.composition.traits.Behaviour;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.komma.concepts.BehaviorBase;
import net.enilink.komma.concepts.IProperty;
import net.enilink.komma.core.IQuery;

public abstract class ObjectSupport extends BehaviorBase implements IObject,
		IModelAware, Behaviour<IObject>,
		net.enilink.komma.internal.model.IModelAware {
	private static final String SELECT_APPLICABLE_CHILD_PROPERTIES = PREFIX //
			+ "SELECT DISTINCT ?property " //
			+ "WHERE { " //
			// select potential child properties
			+ "{" //
			+ "?resource a ?class ." //
			+ "{" //
			+ "    ?property rdfs:domain ?class ." //
			+ "} UNION {" //
			+ "    ?class rdfs:subClassOf [owl:onProperty ?property] ." //
			+ "}" //
			+ "?property rdfs:subPropertyOf komma:hasDescendant ." //
			+ "OPTIONAL {" //
			+ "    ?otherProperty rdfs:subPropertyOf ?property ." //
			+ "    {" //
			+ "        ?otherProperty rdfs:domain ?class ." //
			+ "    } UNION {" //
			+ "        ?class rdfs:subClassOf [owl:onProperty ?otherProperty]" //
			+ "    }" //
			+ "	FILTER (?property != ?otherProperty)" //
			+ "}" //
			+ "FILTER (! bound(?otherProperty))" //
			// select already used child properties
			+ "} UNION {" //
			+ "    ?resource ?property ?someObject ." //
			+ "    ?property rdfs:subPropertyOf komma:hasDescendant ." //
			+ "    OPTIONAL {" //
			+ "        ?otherProperty rdfs:subPropertyOf ?property ." //
			+ "        ?resource ?otherProperty ?someObject ." //
			+ "        FILTER (?property != ?otherProperty)" //
			+ "    }" //
			+ "    FILTER (! bound(?otherProperty))" //
			+ "}" //
			+ "} ORDER BY ?property";

	private static final String SELECT_CONTAINER = PREFIX //
			+ "SELECT ?container WHERE { ?container komma:contains ?obj . }";

	private IModel model;

	@Override
	public IExtendedIterator<IProperty> getApplicableChildProperties() {
		IQuery<?> query = getEntityManager().createQuery(
				SELECT_APPLICABLE_CHILD_PROPERTIES);
		query.setParameter("resource", this);
		query.setIncludeInferred(true);

		return query.evaluate(IProperty.class);
	}

	@Override
	public IObject getContainer() {
		IQuery<?> query = getEntityManager().createQuery(SELECT_CONTAINER);
		query.setParameter("obj", this);
		IExtendedIterator<?> it = query.evaluate();
		try {
			return it.hasNext() ? (IObject) it.next() : null;
		} finally {
			it.close();
		}
	}

	@Override
	public IModel getModel() {
		return model;
	}

	@Override
	public void initModel(IModel model) {
		this.model = model;
	}
}