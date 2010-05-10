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
package net.enilink.komma.util;

import net.enilink.vocab.owl.OWL;
import net.enilink.vocab.rdf.RDF;
import net.enilink.vocab.rdfs.RDFS;
import net.enilink.komma.concepts.CONCEPTS;

public interface ISparqlConstants {
	public static final String PREFIX = "PREFIX rdf: <" + RDF.NAMESPACE
			+ "> PREFIX rdfs: <" + RDFS.NAMESPACE + "> PREFIX owl: <"
			+ OWL.NAMESPACE + "> PREFIX komma: <" + CONCEPTS.NAMESPACE + "> ";
}