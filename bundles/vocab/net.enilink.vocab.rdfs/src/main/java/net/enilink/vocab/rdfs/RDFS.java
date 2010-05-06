/*******************************************************************************
 * Copyright (c) 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.vocab.rdfs;

import net.enilink.composition.annotations.Iri;

import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIImpl;

@Iri("http://www.w3.org/2000/01/rdf-schema#")
public interface RDFS {
	public static final String NAMESPACE = "http://www.w3.org/2000/01/rdf-schema#";
	public static final URI NAMESPACE_URI = URIImpl.createURI(NAMESPACE);

	public static final URI TYPE_RESOURCE = NAMESPACE_URI.appendFragment("Resource");

	public static final URI TYPE_CLASS = NAMESPACE_URI.appendFragment("Class");

	public static final URI TYPE_LITERAL = NAMESPACE_URI.appendFragment("Literal");

	public static final URI TYPE_CONTAINER = NAMESPACE_URI.appendFragment("Container");

	public static final URI TYPE_CONTAINERMEMBERSHIPPROPERTY = NAMESPACE_URI.appendFragment("ContainerMembershipProperty");

	public static final URI TYPE_DATATYPE = NAMESPACE_URI.appendFragment("Datatype");

	public static final URI TYPE_RDFSPROPERTY = NAMESPACE_URI.appendFragment("RdfsProperty");

	public static final URI PROPERTY_SUBCLASSOF = NAMESPACE_URI.appendFragment("subClassOf");

	public static final URI PROPERTY_SUBPROPERTYOF = NAMESPACE_URI.appendFragment("subPropertyOf");

	public static final URI PROPERTY_COMMENT = NAMESPACE_URI.appendFragment("comment");

	public static final URI PROPERTY_LABEL = NAMESPACE_URI.appendFragment("label");

	public static final URI PROPERTY_DOMAIN = NAMESPACE_URI.appendFragment("domain");

	public static final URI PROPERTY_RANGE = NAMESPACE_URI.appendFragment("range");

	public static final URI PROPERTY_SEEALSO = NAMESPACE_URI.appendFragment("seeAlso");

	public static final URI PROPERTY_ISDEFINEDBY = NAMESPACE_URI.appendFragment("isDefinedBy");

	public static final URI PROPERTY_MEMBER = NAMESPACE_URI.appendFragment("member");

}
