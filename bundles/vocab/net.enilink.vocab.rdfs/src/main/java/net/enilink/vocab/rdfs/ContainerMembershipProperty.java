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

import net.enilink.vocab.rdf.Property;

/** The class of container membership properties, rdf:_1, rdf:_2, ...,
                    all of which are sub-properties of 'member'. */
@Iri("http://www.w3.org/2000/01/rdf-schema#ContainerMembershipProperty")
public interface ContainerMembershipProperty extends Property {

}
