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
package net.enilink.vocab.rdf;

import java.util.Set;

import net.enilink.composition.annotations.Iri;

import net.enilink.vocab.rdfs.Resource;

/** The class of RDF properties. */
@Iri("http://www.w3.org/1999/02/22-rdf-syntax-ns#Property")
public interface Property extends Resource {


	/** A domain of the subject property. */
	@Iri("http://www.w3.org/2000/01/rdf-schema#domain")
	public abstract Set<net.enilink.vocab.rdfs.Class> getRdfsDomains();

	/** A domain of the subject property. */
	public abstract void setRdfsDomains(Set<? extends net.enilink.vocab.rdfs.Class> value);


	/** A range of the subject property. */
	@Iri("http://www.w3.org/2000/01/rdf-schema#range")
	public abstract Set<net.enilink.vocab.rdfs.Class> getRdfsRanges();

	/** A range of the subject property. */
	public abstract void setRdfsRanges(Set<? extends net.enilink.vocab.rdfs.Class> value);


	/** The subject is a subproperty of a property. */
	@Iri("http://www.w3.org/2000/01/rdf-schema#subPropertyOf")
	public abstract Set<Property> getRdfsSubPropertyOf();

	/** The subject is a subproperty of a property. */
	public abstract void setRdfsSubPropertyOf(Set<? extends Property> value);

}
