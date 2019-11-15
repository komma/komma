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
package net.enilink.vocab.rdf;

import net.enilink.composition.annotations.Iri;

import net.enilink.vocab.rdfs.Resource;

/** The class of RDF statements. */
@Iri("http://www.w3.org/1999/02/22-rdf-syntax-ns#Statement")
public interface Statement extends Resource {


	/** The object of the subject RDF statement. */
	@Iri("http://www.w3.org/1999/02/22-rdf-syntax-ns#object")
	public abstract Object getRdfObject();

	/** The object of the subject RDF statement. */
	public abstract void setRdfObject(Object value);


	/** The predicate of the subject RDF statement. */
	@Iri("http://www.w3.org/1999/02/22-rdf-syntax-ns#predicate")
	public abstract Object getRdfPredicate();

	/** The predicate of the subject RDF statement. */
	public abstract void setRdfPredicate(Object value);


	/** The subject of the subject RDF statement. */
	@Iri("http://www.w3.org/1999/02/22-rdf-syntax-ns#subject")
	public abstract Object getRdfSubject();

	/** The subject of the subject RDF statement. */
	public abstract void setRdfSubject(Object value);

}
