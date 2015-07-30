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
package net.enilink.komma.em.concepts;

import java.util.Collection;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.komma.core.IQuery;
import net.enilink.komma.em.util.ISparqlConstants;
import net.enilink.vocab.owl.DatatypeProperty;
import net.enilink.vocab.owl.OWL;
import net.enilink.vocab.owl.ObjectProperty;

public abstract class OntologySupport extends BehaviorBase implements IOntology {
	private static final String SELECT_PROPERTIES() {
		return ISparqlConstants.PREFIX //
				+ "SELECT DISTINCT ?p WHERE {" //
				+ "?p a ?propertyType . " //
				+ "FILTER NOT EXISTS {" //
				+ "		?p a ?otherType . ?otherType rdfs:subClassOf ?propertyType " //
				+ "		FILTER (?propertyType = owl:ObjectProperty && (?otherType = owl:AnnotationProperty || ?otherType = owl:DatatypeProperty || ?otherType = rdfs:ContainerMembershipProperty))" //
				+ "}" //
				+ "FILTER NOT EXISTS {" //
				+ "		?p rdfs:subPropertyOf ?other " //
				+ "		FILTER (?other != owl:topObjectProperty && ?other != owl:topDataProperty && ?p != ?other && isIRI(?other))" //
				+ "} " //
				+ "FILTER (?p != owl:topObjectProperty && ?p != owl:topDataProperty)" //
				+ "}";
	}

	@Override
	public Collection<IClass> getRootClasses() {
		return getEntityManager().find(OWL.TYPE_THING, IClass.class)
				.getSubClasses(true, true).toList();
	}

	@Override
	public IExtendedIterator<IProperty> getRootProperties() {
		// [PERFORMANCE] root properties are retrieved without inference
		IQuery<?> query = getEntityManager()
				.createQuery(
						ISparqlConstants.PREFIX
								+ "SELECT DISTINCT ?p WHERE {"
								+ "?p a ?type { ?type rdfs:subClassOf* rdf:Property } UNION { ?p a rdf:Property } "
								+ "FILTER NOT EXISTS {"
								+ " ?p rdfs:subPropertyOf ?other FILTER (?p != ?other && isIRI(?other))"
								+ "} "
								+ "FILTER (isIRI(?p) && (?type = owl:AnnotationProperty || !regex(str(?type), 'http://www.w3.org/2002/07/owl#')))"
								+ "}", false);
		return query.evaluate(IProperty.class);
	}

	@Override
	public IExtendedIterator<IProperty> getRootObjectProperties() {
		// [PERFORMANCE] root properties are retrieved without inference
		IQuery<?> query = getEntityManager().createQuery(SELECT_PROPERTIES(),
				false).setParameter("propertyType", OWL.TYPE_OBJECTPROPERTY);
		return query.evaluate(IProperty.class, ObjectProperty.class);
	}

	@Override
	public IExtendedIterator<IProperty> getRootDatatypeProperties() {
		// [PERFORMANCE] root properties are retrieved without inference
		IQuery<?> query = getEntityManager().createQuery(SELECT_PROPERTIES(),
				false).setParameter("propertyType", OWL.TYPE_DATATYPEPROPERTY);
		return query.evaluate(IProperty.class, DatatypeProperty.class);
	}
}
