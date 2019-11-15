/*******************************************************************************
 * Copyright (c) 2009, 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.vocab.rdfs;

import java.util.List;

import net.enilink.composition.annotations.Iri;

/** The class of RDF datatypes. */
@Iri("http://www.w3.org/2000/01/rdf-schema#Datatype")
public interface Datatype extends Class {
	/** http://www.w3.org/2002/07/owl#datatypeComplementOf */
	@Iri("http://www.w3.org/2002/07/owl#datatypeComplementOf")
	public abstract Datatype getOwlDatatypeComplementOf();

	/** http://www.w3.org/2002/07/owl#datatypeComplementOf */
	public abstract void setOwlDatatypeComplementOf(Datatype value);
	
	/** http://www.w3.org/2002/07/owl#onDatatype */
	@Iri("http://www.w3.org/2002/07/owl#onDatatype")
	public abstract Datatype getOwlOnDatatype();

	/** http://www.w3.org/2002/07/owl#onDatatype */
	public abstract void setOwlOnDatatype(Datatype datatype);

	/** http://www.w3.org/2002/07/owl#withRestrictions */
	@Iri("http://www.w3.org/2002/07/owl#withRestrictions")
	public abstract List getOwlWithRestrictions();

	/** http://www.w3.org/2002/07/owl#withRestrictions */
	public abstract void setOwlWithRestrictions(List restrictions);
}
