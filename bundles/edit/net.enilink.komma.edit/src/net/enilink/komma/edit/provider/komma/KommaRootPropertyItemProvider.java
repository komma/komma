/**
 * <copyright>
 *
 * Copyright (c) 2002, 2009 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   IBM - Initial API and implementation
 *
 * </copyright>
 *
 * $Id: ResourceItemProvider.java,v 1.12 2007/06/14 18:32:42 emerks Exp $
 */
package net.enilink.komma.edit.provider.komma;

import java.util.Collection;

import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.common.util.ICollector;
import net.enilink.komma.common.util.IResourceLocator;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIImpl;
import net.enilink.komma.edit.provider.ISearchableItemProvider;
import net.enilink.komma.edit.provider.IViewerNotification;
import net.enilink.komma.edit.provider.ReflectiveItemProvider;
import net.enilink.komma.edit.provider.SparqlSearchableItemProvider;
import net.enilink.komma.edit.provider.ViewerNotification;
import net.enilink.komma.em.concepts.IOntology;
import net.enilink.komma.em.concepts.IProperty;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IObject;
import net.enilink.komma.model.event.IStatementNotification;
import net.enilink.vocab.komma.KOMMA;
import net.enilink.vocab.owl.OWL;
import net.enilink.vocab.rdfs.RDFS;

/**
 * This is the item provider adapter for a KOMMA's synthetic root properties.
 */
public class KommaRootPropertyItemProvider extends ReflectiveItemProvider {
	/**
	 * This constructs an instance from a factory and a notifier.
	 */
	public KommaRootPropertyItemProvider(IAdapterFactory adapterFactory,
			IResourceLocator resourceLocator,
			Collection<? extends IReference> targetTypes) {
		super(adapterFactory, resourceLocator, targetTypes);
	}

	protected void addViewerNotifications(
			Collection<IViewerNotification> viewerNotifications,
			IStatementNotification notification) {
		if (RDFS.PROPERTY_SUBPROPERTYOF.equals(notification.getPredicate())) {
			IEntity object = resolveReference(notification.getObject());
			if (object != null) {
				viewerNotifications.add(new ViewerNotification(object));
			}
			return;
		}
		super.addViewerNotifications(viewerNotifications, notification);
	}

	@Override
	public Collection<?> getChildren(Object object) {
		if (object instanceof IObject) {
			IOntology ontology = ((IObject) object).getModel().getOntology();
			if (KOMMA.PROPERTY_ROOTOBJECTPROPERTY.equals(object)) {
				return ontology.getRootObjectProperties().toList();
			} else if (KOMMA.PROPERTY_ROOTDATATYPEPROPERTY.equals(object)) {
				return ontology.getRootDatatypeProperties().toList();
			} else if (KOMMA.PROPERTY_ROOTPROPERTY.equals(object)) {
				return ontology.getRootProperties().toList();
			}
		}
		return super.getChildren(object);
	}

	@Override
	protected void collectChildrenProperties(Object object,
			Collection<IProperty> childrenProperties) {
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
	protected ISearchableItemProvider getSearchableItemProvider() {
		return new SparqlSearchableItemProvider() {
			@Override
			protected String getQueryFindPatterns(Object parent) {
				if (KOMMA.PROPERTY_ROOTOBJECTPROPERTY.equals(parent)) {
					return findPatternsFor(OWL.TYPE_OBJECTPROPERTY);
				} else if (KOMMA.PROPERTY_ROOTDATATYPEPROPERTY.equals(parent)) {
					return findPatternsFor(OWL.TYPE_DATATYPEPROPERTY);
				} else if (KOMMA.PROPERTY_ROOTPROPERTY.equals(parent)) {
					return "?s a ?type { ?type rdfs:subClassOf rdf:Property } UNION { ?s a rdf:Property } "
							+ "FILTER (?type = owl:AnnotationProperty || !regex(str(?type), 'http://www.w3.org/2002/07/owl#'))";
				}
				return super.getQueryFindPatterns(parent);
			}
		};
	}

	/**
	 * This returns Resource.gif.
	 */
	@Override
	public Object getImage(Object object) {
		URI uri = (object instanceof IModel) ? ((IModel) object).getURI()
				: ((IEntity) object).getURI();

		Object image = URIImpl.createURI(getResourceLocator().getImage(
				"full/obj16/Model").toString()
				+ "#" + uri.fileExtension());
		return image;
	}

	@Override
	public boolean hasChildren(Object object) {
		return true;
	}

	/**
	 * This adds {@link net.enilink.komma.edit.command.CommandParameter}s
	 * describing the children that can be created under this object.
	 */
	@Override
	protected void collectNewChildDescriptors(
			ICollector<Object> newChildDescriptors, Object object) {
		newChildDescriptors.done();
	}
}
