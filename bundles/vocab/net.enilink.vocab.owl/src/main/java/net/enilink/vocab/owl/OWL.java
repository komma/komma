/*******************************************************************************
 * Copyright (c) 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.vocab.owl;

import net.enilink.composition.annotations.Iri;

import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIs;

@Iri("http://www.w3.org/2002/07/owl#")
public interface OWL {
	public static final String NAMESPACE = "http://www.w3.org/2002/07/owl#";
	public static final URI NAMESPACE_URI = URIs.createURI(NAMESPACE);
	
	public static final URI TYPE_ANNOTATION = NAMESPACE_URI.appendFragment("Annotation");
	
	public static final URI TYPE_AXIOM = NAMESPACE_URI.appendFragment("Axiom");

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
	
	public static final URI TYPE_INDIVIDUAL = NAMESPACE_URI.appendFragment("Individual");
	
	public static final URI TYPE_NEGATIVEPROPERTYASSERTION = NAMESPACE_URI.appendFragment("NegativePropertyAssertion");

	public static final URI PROPERTY_ANNOTATEDSOURCE = NAMESPACE_URI.appendFragment("annotatedSource");
	
	public static final URI PROPERTY_ANNOTATEDPROPERTY = NAMESPACE_URI.appendFragment("annotatedProperty");

	public static final URI PROPERTY_ANNOTATEDTARGET = NAMESPACE_URI.appendFragment("annotatedTarget");
	
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
	
	public static final URI PROPERTY_MINQUALIFIEDCARDINALITY = NAMESPACE_URI.appendFragment("minQualifiedCardinality");

	public static final URI PROPERTY_MAXQUALIFIEDCARDINALITY = NAMESPACE_URI.appendFragment("maxQualifiedCardinality");

	public static final URI PROPERTY_HASSELF = NAMESPACE_URI.appendFragment("hasSelf");
	
	public static final URI PROPERTY_QUALIFIEDCARDINALITY = NAMESPACE_URI.appendFragment("qualifiedCardinality");

	public static final URI PROPERTY_INVERSEOF = NAMESPACE_URI.appendFragment("inverseOf");

	public static final URI PROPERTY_IMPORTS = NAMESPACE_URI.appendFragment("imports");

	public static final URI PROPERTY_VERSIONINFO = NAMESPACE_URI.appendFragment("versionInfo");

	public static final URI PROPERTY_PRIORVERSION = NAMESPACE_URI.appendFragment("priorVersion");

	public static final URI PROPERTY_BACKWARDCOMPATIBLEWITH = NAMESPACE_URI.appendFragment("backwardCompatibleWith");

	public static final URI PROPERTY_INCOMPATIBLEWITH = NAMESPACE_URI.appendFragment("incompatibleWith");
	
	public static final URI PROPERTY_PROPERTYCHAINAXIOM = NAMESPACE_URI.appendFragment("propertyChainAxiom");
	
	public static final URI PROPERTY_ONDATATYPE = NAMESPACE_URI.appendFragment("onDatatype");
	
	public static final URI PROPERTY_WITHRESTRICTIONS = NAMESPACE_URI.appendFragment("withRestrictions");
	
	public static final URI PROPERTY_HASKEY = NAMESPACE_URI.appendFragment("hasKey");
	
	public static final URI PROPERTY_ONDATARANGE = NAMESPACE_URI.appendFragment("onDataRange");
	
	public static final URI PROPERTY_ONCLASS = NAMESPACE_URI.appendFragment("onClass");
	
	public static final URI PROPERTY_TOPOBJECTPROPERTY = NAMESPACE_URI.appendFragment("topObjectProperty");
	
	public static final URI PROPERTY_TOPDATAPROPERTY = NAMESPACE_URI.appendFragment("topDataProperty");
	
	public static final URI PROPERTY_SOURCEINDIVIDUAL = NAMESPACE_URI.appendFragment("sourceIndividual");
	
	public static final URI PROPERTY_TARGETINDIVIDUAL = NAMESPACE_URI.appendFragment("targetIndividual");
	
	public static final URI PROPERTY_TARGETVALUE = NAMESPACE_URI.appendFragment("targetValue");
	
	public static final URI PROPERTY_ASSERTIONPROPERTY = NAMESPACE_URI.appendFragment("assertionProperty");
	
	public static final URI PROPERTY_DISJOINTUNIONOF = NAMESPACE_URI.appendFragment("disjointUnionOf");

}
