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

import net.enilink.vocab.rdfs.Container;

/** The class of ordered containers. */
@Iri("http://www.w3.org/1999/02/22-rdf-syntax-ns#Seq")
public interface Seq<E> extends Container<E> {

}
