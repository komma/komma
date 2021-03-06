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

/** The class of RDF Lists. */
@Iri("http://www.w3.org/1999/02/22-rdf-syntax-ns#List")
public interface List<E> extends Resource, java.util.List<E> {
	/** The first item in the subject RDF list. */
	@Iri("http://www.w3.org/1999/02/22-rdf-syntax-ns#first")
	public abstract E getRdfFirst();

	/** The first item in the subject RDF list. */
	public abstract void setRdfFirst(E value);


	/** The rest of the subject RDF list after the first item. */
	@Iri("http://www.w3.org/1999/02/22-rdf-syntax-ns#rest")
	public abstract List<E> getRdfRest();

	/** The rest of the subject RDF list after the first item. */
	public abstract void setRdfRest(List<E> value);
}
