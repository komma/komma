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
package net.enilink.vocab.rdfs;

import net.enilink.composition.annotations.Iri;

/** The class of RDF datatypes. */
@Iri("http://www.w3.org/2000/01/rdf-schema#Datatype")
public interface Datatype extends Class {
	/** http://www.w3.org/2002/07/owl#datatypeComplementOf */
	@Iri("http://www.w3.org/2002/07/owl#datatypeComplementOf")
	public abstract Datatype getOwlDatatypeComplementOf();

	/** http://www.w3.org/2002/07/owl#datatypeComplementOf */
	public abstract void setOwlDatatypeComplementOf(Datatype value);
	
}
