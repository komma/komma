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

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.vocab.owl.DatatypeProperty;
import net.enilink.vocab.owl.OWL;
import net.enilink.vocab.owl.ObjectProperty;
import net.enilink.komma.core.IQuery;
import net.enilink.komma.util.ISparqlConstants;

public abstract class OntologySupport extends BehaviorBase implements IOntology {
	private static final String SELECT_PROPERTIES(String type) {
		return ISparqlConstants.PREFIX + "SELECT DISTINCT ?r WHERE {" + "?r a "
				+ type + " OPTIONAL {?r rdfs:subPropertyOf ?other "
				+ "FILTER(?r != ?other && isIRI(?other))} "
				+ "FILTER (!bound(?other) && isIRI(?r)) }";
	}

	@Override
	public Collection<IClass> getRootClasses() {
		return getKommaManager().find(OWL.TYPE_THING, IClass.class)
				.getSubClasses(true, true).toList();
	}

	@Override
	public IExtendedIterator<IProperty> getRootProperties() {
		IQuery<?> query = getKommaManager().createQuery(
				SELECT_PROPERTIES("rdf:Property"));
		return query.evaluate(IProperty.class);
	}

	@Override
	public IExtendedIterator<IProperty> getRootObjectProperties() {
		IQuery<?> query = getKommaManager().createQuery(
				SELECT_PROPERTIES("owl:ObjectProperty"));
		return query.evaluateRestricted(IProperty.class, ObjectProperty.class);
	}

	@Override
	public IExtendedIterator<IProperty> getRootDatatypeProperties() {
		IQuery<?> query = getKommaManager().createQuery(
				SELECT_PROPERTIES("owl:DatatypeProperty"));
		return query
				.evaluateRestricted(IProperty.class, DatatypeProperty.class);
	}
}
