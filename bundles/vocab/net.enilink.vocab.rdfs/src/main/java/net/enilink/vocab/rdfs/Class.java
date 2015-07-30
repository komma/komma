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

import java.util.Set;

import net.enilink.composition.annotations.Iri;

/** The class of classes. */
@Iri("http://www.w3.org/2000/01/rdf-schema#Class")
public interface Class extends Resource {
	/** The subject is a subclass of a class. */
	@Iri("http://www.w3.org/2000/01/rdf-schema#subClassOf")
	public abstract Set<Class> getRdfsSubClassOf();

	/** The subject is a subclass of a class. */
	public abstract void setRdfsSubClassOf(Set<? extends Class> value);
}
