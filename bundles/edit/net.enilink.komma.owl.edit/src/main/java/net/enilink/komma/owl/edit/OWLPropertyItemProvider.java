/*******************************************************************************
 * Copyright (c) 2009 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.owl.edit;

import java.util.Collection;

import net.enilink.komma.common.util.IResourceLocator;
import net.enilink.komma.core.URI;
import net.enilink.komma.em.concepts.IClass;
import net.enilink.komma.model.IObject;
import net.enilink.komma.rdfs.edit.RDFSPropertyItemProvider;
import net.enilink.vocab.owl.OWL;

public class OWLPropertyItemProvider extends RDFSPropertyItemProvider {
	public OWLPropertyItemProvider(
			OWLItemProviderAdapterFactory adapterFactory,
			IResourceLocator resourceLocator, Collection<IClass> supportedTypes) {
		super(adapterFactory, resourceLocator, supportedTypes);
	}

	protected String findPatternsFor(URI propertyType) {
		String typeLiteral = "<" + propertyType + ">";
		StringBuilder patterns = new StringBuilder("?s a ").append(typeLiteral)
				.append(" . ");
		if (OWL.TYPE_OBJECTPROPERTY.equals(propertyType)) {
			patterns.append("FILTER NOT EXISTS {" //
					+ "		?s a ?otherType . ?otherType rdfs:subClassOf "
					+ typeLiteral //
					+ "		FILTER (?otherType = owl:AnnotationProperty || ?otherType = owl:DatatypeProperty || ?otherType = rdfs:ContainerMembershipProperty)" //
					+ "}");
		}
		return patterns.toString();
	};

	@Override
	protected String getQueryFindPatterns(Object parent) {
		if (OWL.PROPERTY_TOPOBJECTPROPERTY.equals(parent)) {
			return findPatternsFor(OWL.TYPE_OBJECTPROPERTY);
		} else if (OWL.PROPERTY_TOPDATAPROPERTY.equals(parent)) {
			return findPatternsFor(OWL.TYPE_DATATYPEPROPERTY);
		}
		return super.getQueryFindPatterns(parent);
	}

	@Override
	public Collection<?> getChildren(Object object) {
		if (object instanceof IObject) {
			if (OWL.PROPERTY_TOPOBJECTPROPERTY.equals(object)) {
				return ((IObject) object).getModel().getOntology()
						.getRootObjectProperties().toList();
			} else if (OWL.PROPERTY_TOPDATAPROPERTY.equals(object)) {
				return ((IObject) object).getModel().getOntology()
						.getRootDatatypeProperties().toList();
			}
		}
		return super.getChildren(object);
	}
}
