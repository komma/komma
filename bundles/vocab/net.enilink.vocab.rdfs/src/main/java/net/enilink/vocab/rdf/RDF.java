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
package net.enilink.vocab.rdf;

import net.enilink.composition.annotations.Iri;

import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIs;

@Iri("http://www.w3.org/1999/02/22-rdf-syntax-ns#")
public interface RDF {
	public static final String NAMESPACE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	public static final URI NAMESPACE_URI = URIs.createURI(NAMESPACE);

	public static final URI TYPE_PROPERTY = NAMESPACE_URI.appendFragment("Property");

	public static final URI TYPE_STATEMENT = NAMESPACE_URI.appendFragment("Statement");

	public static final URI TYPE_BAG = NAMESPACE_URI.appendFragment("Bag");

	public static final URI TYPE_SEQ = NAMESPACE_URI.appendFragment("Seq");

	public static final URI TYPE_ALT = NAMESPACE_URI.appendFragment("Alt");
	
	public static final URI TYPE_LIST = NAMESPACE_URI.appendFragment("List");

	public static final URI TYPE_HTML = NAMESPACE_URI.appendFragment("HTML");
	
	public static final URI TYPE_LANGSTRING = NAMESPACE_URI.appendFragment("langString");
	
	public static final URI TYPE_XMLLITERAL = NAMESPACE_URI.appendFragment("XMLLiteral");
	
	public static final URI PROPERTY_LI = NAMESPACE_URI.appendFragment("li");

	public static final URI PROPERTY_TYPE = NAMESPACE_URI.appendFragment("type");

	public static final URI PROPERTY_SUBJECT = NAMESPACE_URI.appendFragment("subject");

	public static final URI PROPERTY_PREDICATE = NAMESPACE_URI.appendFragment("predicate");

	public static final URI PROPERTY_OBJECT = NAMESPACE_URI.appendFragment("object");

	public static final URI PROPERTY_VALUE = NAMESPACE_URI.appendFragment("value");

	public static final URI PROPERTY_FIRST = NAMESPACE_URI.appendFragment("first");

	public static final URI PROPERTY_REST = NAMESPACE_URI.appendFragment("rest");
	
	public static final URI NIL = NAMESPACE_URI.appendFragment("nil");

}
