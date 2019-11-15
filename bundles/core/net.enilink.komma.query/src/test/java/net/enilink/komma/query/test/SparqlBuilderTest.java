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
package net.enilink.komma.query.test;

import org.junit.Test;

import net.enilink.komma.query.SparqlBuilder;

/**
 * Simple JUnit Test for the SPARQL query builder
 * 
 * @author Ken Wenzel
 */
public class SparqlBuilderTest {
	public static final String PREFIX = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
			+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
			+ "PREFIX owl: <http://www.w3.org/2002/07/owl#> "
			+ "PREFIX komma: <http://enilink.net/vocab/komma#> ";

	private static final String SELECT_ROOT_CLASSES(boolean named) {
		return PREFIX
				+ "SELECT DISTINCT ?class "
				+ "WHERE { "
				+ "{?class a owl:Class} UNION {?class a rdfs:Class}"
				+ "OPTIONAL {"
				+ "?class rdfs:subClassOf ?otherClass "
				+ "FILTER (?class != ?otherClass && ?otherClass != owl:Thing"
				+ (named ? " && isIRI(?otherClass)" : "")
				+ ")"
				+ "} FILTER ("
				+ (named ? "isIRI(?class) && " : "")
				+ "!bound(?otherClass) && ?class != owl:Thing) } ORDER BY ?class";
	};

	private static final String HAS_SUBCLASSES(boolean named) {
		return PREFIX + "ASK { " + "?subClass rdfs:subClassOf ?superClass ."
				+ "{?subClass a owl:Class} UNION {?subClass a rdfs:Class} . "
				+ "FILTER (?subClass != ?superClass"
				+ (named ? " && isIRI(?subClass)" : "") + ")}";
	}

	private static final String SELECT_DIRECT_CLASSES(boolean named) {
		return PREFIX //
				+ "SELECT ?class WHERE {" //
				+ "?resource a ?class ." //
				+ "OPTIONAL {?resource a ?otherClass . ?otherClass rdfs:subClassOf ?class FILTER ("
				+ (named ? "isIRI(?otherClass) && " : "")
				+ "?otherClass != ?class)}" //
				+ "OPTIONAL {?resource a ?otherClass . FILTER ("
				+ (named ? "isIRI(?otherClass) && " : "")
				+ "?class = owl:Thing && ?otherClass != ?class)}" //
				+ "FILTER ("
				+ (named ? "isIRI(?class) && " : "")
				+ "!bound(?otherClass))}";
	}

	@Test
	public void test() throws Exception {
		System.out.println(new SparqlBuilder(SELECT_ROOT_CLASSES(true)) //
				.optional("komma:hasNamedSubClasses", "subClass",
						"superClass", HAS_SUBCLASSES(true))//
				.optional("komma:directNamedClasses", "class", "resource",
						SELECT_DIRECT_CLASSES(true)).toString());
	}
}
