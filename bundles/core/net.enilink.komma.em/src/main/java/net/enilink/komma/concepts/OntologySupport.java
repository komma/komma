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
import net.enilink.vocab.rdf.RDF;
import net.enilink.komma.core.IQuery;
import net.enilink.komma.util.ISparqlConstants;

public abstract class OntologySupport extends BehaviorBase implements IOntology {
	private static final String SELECT_PROPERTIES() {
		return ISparqlConstants.PREFIX //
				+ "SELECT DISTINCT ?p WHERE {" //
				+ "?p a ?propertyType ." //
				+ "OPTIONAL {" //
				+ "		?p a ?otherType . ?otherType rdfs:subClassOf ?propertyType " //
				+ "		FILTER (?propertyType = owl:ObjectProperty && (?otherType = owl:AnnotationProperty || ?otherType = owl:DatatypeProperty || ?otherType = rdfs:ContainerMembershipProperty))" //
				+ "}" //
				+ "OPTIONAL {" //
				+ "		?p rdfs:subPropertyOf ?other " //
				+ "		FILTER(?p != ?other && isIRI(?other))" //
				+ "} " //
				+ "FILTER (!bound(?otherType) && !bound(?other) && isIRI(?p)) }";
	}

	@Override
	public Collection<IClass> getRootClasses() {
		return getEntityManager().find(OWL.TYPE_THING, IClass.class)
				.getSubClasses(true, true).toList();
	}

	@Override
	public IExtendedIterator<IProperty> getRootProperties() {
		IQuery<?> query = getEntityManager().createQuery(SELECT_PROPERTIES())
				.setParameter("propertyType", RDF.TYPE_PROPERTY);
		return query.evaluate(IProperty.class);
	}

	@Override
	public IExtendedIterator<IProperty> getRootObjectProperties() {
		IQuery<?> query = getEntityManager().createQuery(SELECT_PROPERTIES())
				.setParameter("propertyType", OWL.TYPE_OBJECTPROPERTY);
		return query.evaluate(IProperty.class, ObjectProperty.class);
	}

	@Override
	public IExtendedIterator<IProperty> getRootDatatypeProperties() {
		IQuery<?> query = getEntityManager().createQuery(SELECT_PROPERTIES())
				.setParameter("propertyType", OWL.TYPE_DATATYPEPROPERTY);
		return query.evaluate(IProperty.class, DatatypeProperty.class);
	}
}
