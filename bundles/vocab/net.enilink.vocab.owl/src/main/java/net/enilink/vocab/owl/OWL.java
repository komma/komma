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
package net.enilink.vocab.owl;

import net.enilink.composition.annotations.Iri;

import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIImpl;

@Iri("http://www.w3.org/2002/07/owl#")
public interface OWL {
	public static final String NAMESPACE = "http://www.w3.org/2002/07/owl#";
	public static final URI NAMESPACE_URI = URIImpl.createURI(NAMESPACE);

	public static final URI TYPE_THING = NAMESPACE_URI.appendFragment("Thing");

	public static final URI TYPE_NOTHING = NAMESPACE_URI.appendFragment("Nothing");

	public static final URI TYPE_CLASS = NAMESPACE_URI.appendFragment("Class");

	public static final URI TYPE_ALLDIFFERENT = NAMESPACE_URI.appendFragment("AllDifferent");

	public static final URI TYPE_RESTRICTION = NAMESPACE_URI.appendFragment("Restriction");

	public static final URI TYPE_OBJECTPROPERTY = NAMESPACE_URI.appendFragment("ObjectProperty");

	public static final URI TYPE_DATATYPEPROPERTY = NAMESPACE_URI.appendFragment("DatatypeProperty");

	public static final URI TYPE_TRANSITIVEPROPERTY = NAMESPACE_URI.appendFragment("TransitiveProperty");

	public static final URI TYPE_SYMMETRICPROPERTY = NAMESPACE_URI.appendFragment("SymmetricProperty");

	public static final URI TYPE_FUNCTIONALPROPERTY = NAMESPACE_URI.appendFragment("FunctionalProperty");

	public static final URI TYPE_INVERSEFUNCTIONALPROPERTY = NAMESPACE_URI.appendFragment("InverseFunctionalProperty");

	public static final URI TYPE_ANNOTATIONPROPERTY = NAMESPACE_URI.appendFragment("AnnotationProperty");

	public static final URI TYPE_ONTOLOGY = NAMESPACE_URI.appendFragment("Ontology");

	public static final URI TYPE_ONTOLOGYPROPERTY = NAMESPACE_URI.appendFragment("OntologyProperty");

	public static final URI TYPE_DEPRECATEDCLASS = NAMESPACE_URI.appendFragment("DeprecatedClass");

	public static final URI TYPE_DEPRECATEDPROPERTY = NAMESPACE_URI.appendFragment("DeprecatedProperty");

	public static final URI TYPE_DATARANGE = NAMESPACE_URI.appendFragment("DataRange");

	public static final URI TYPE_OWLCLASS = NAMESPACE_URI.appendFragment("OwlClass");

	public static final URI TYPE_OWLPROPERTY = NAMESPACE_URI.appendFragment("OwlProperty");

	public static final URI TYPE_OWLRESOURCE = NAMESPACE_URI.appendFragment("OwlResource");

	public static final URI PROPERTY_EQUIVALENTCLASS = NAMESPACE_URI.appendFragment("equivalentClass");

	public static final URI PROPERTY_DISJOINTWITH = NAMESPACE_URI.appendFragment("disjointWith");

	public static final URI PROPERTY_EQUIVALENTPROPERTY = NAMESPACE_URI.appendFragment("equivalentProperty");

	public static final URI PROPERTY_SAMEAS = NAMESPACE_URI.appendFragment("sameAs");

	public static final URI PROPERTY_DIFFERENTFROM = NAMESPACE_URI.appendFragment("differentFrom");

	public static final URI PROPERTY_DISTINCTMEMBERS = NAMESPACE_URI.appendFragment("distinctMembers");

	public static final URI PROPERTY_UNIONOF = NAMESPACE_URI.appendFragment("unionOf");

	public static final URI PROPERTY_INTERSECTIONOF = NAMESPACE_URI.appendFragment("intersectionOf");

	public static final URI PROPERTY_COMPLEMENTOF = NAMESPACE_URI.appendFragment("complementOf");

	public static final URI PROPERTY_ONEOF = NAMESPACE_URI.appendFragment("oneOf");

	public static final URI PROPERTY_ONPROPERTY = NAMESPACE_URI.appendFragment("onProperty");

	public static final URI PROPERTY_ALLVALUESFROM = NAMESPACE_URI.appendFragment("allValuesFrom");

	public static final URI PROPERTY_HASVALUE = NAMESPACE_URI.appendFragment("hasValue");

	public static final URI PROPERTY_SOMEVALUESFROM = NAMESPACE_URI.appendFragment("someValuesFrom");

	public static final URI PROPERTY_MINCARDINALITY = NAMESPACE_URI.appendFragment("minCardinality");

	public static final URI PROPERTY_MAXCARDINALITY = NAMESPACE_URI.appendFragment("maxCardinality");

	public static final URI PROPERTY_CARDINALITY = NAMESPACE_URI.appendFragment("cardinality");

	public static final URI PROPERTY_INVERSEOF = NAMESPACE_URI.appendFragment("inverseOf");

	public static final URI PROPERTY_IMPORTS = NAMESPACE_URI.appendFragment("imports");

	public static final URI PROPERTY_VERSIONINFO = NAMESPACE_URI.appendFragment("versionInfo");

	public static final URI PROPERTY_PRIORVERSION = NAMESPACE_URI.appendFragment("priorVersion");

	public static final URI PROPERTY_BACKWARDCOMPATIBLEWITH = NAMESPACE_URI.appendFragment("backwardCompatibleWith");

	public static final URI PROPERTY_INCOMPATIBLEWITH = NAMESPACE_URI.appendFragment("incompatibleWith");

}
