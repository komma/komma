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
package net.enilink.vocab.komma;

import net.enilink.composition.annotations.Iri;

import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIImpl;

@Iri("http://enilink.net/vocab/komma#")
public interface KOMMA {
	public static final String NAMESPACE = "http://enilink.net/vocab/komma#";
	public static final URI NAMESPACE_URI = URIImpl.createURI(NAMESPACE);

	public static final URI TYPE_KEYVALUEMAP = NAMESPACE_URI.appendFragment("KeyValueMap");

	public static final URI TYPE_LITERALKEYMAP = NAMESPACE_URI.appendFragment("LiteralKeyMap");

	public static final URI TYPE_LITERALKEYVALUEMAP = NAMESPACE_URI.appendFragment("LiteralKeyValueMap");

	public static final URI TYPE_LITERALVALUEMAP = NAMESPACE_URI.appendFragment("LiteralValueMap");

	public static final URI TYPE_MAP = NAMESPACE_URI.appendFragment("Map");

	public static final URI TYPE_MAPENTRY = NAMESPACE_URI.appendFragment("MapEntry");

	public static final URI TYPE_KOMMARESOURCE = NAMESPACE_URI.appendFragment("KommaResource");

	public static final URI PROPERTY_KEYDATA = NAMESPACE_URI.appendFragment("keyData");

	public static final URI PROPERTY_VALUEDATA = NAMESPACE_URI.appendFragment("valueData");

	public static final URI PROPERTY_PRECEDESTRANSITIVE = NAMESPACE_URI.appendFragment("precedesTransitive");

	public static final URI PROPERTY_ENTRY = NAMESPACE_URI.appendFragment("entry");

	public static final URI PROPERTY_KEY = NAMESPACE_URI.appendFragment("key");

	public static final URI PROPERTY_VALUE = NAMESPACE_URI.appendFragment("value");

	public static final URI PROPERTY_CONTAINSTRANSITIVE = NAMESPACE_URI.appendFragment("containsTransitive");
	
	public static final URI PROPERTY_HASDESCENDANT = NAMESPACE_URI.appendFragment("hasDescendant");

	public static final URI PROPERTY_CONTAINS = NAMESPACE_URI.appendFragment("contains");

	public static final URI PROPERTY_ORDEREDCONTAINS = NAMESPACE_URI.appendFragment("orderedContains");

	public static final URI PROPERTY_PRECEDES = NAMESPACE_URI.appendFragment("precedes");

	public static final URI PROPERTY_ISABSTRACT = NAMESPACE_URI.appendFragment("isAbstract");
	
	public static final URI PROPERTY_ROOTOBJECTPROPERTY = NAMESPACE_URI.appendFragment("rootObjectProperty");
	
	public static final URI PROPERTY_ROOTDATATYPEPROPERTY = NAMESPACE_URI.appendFragment("rootDatatypeProperty");
	
	public static final URI PROPERTY_ROOTPROPERTY = NAMESPACE_URI.appendFragment("rootProperty");

}
