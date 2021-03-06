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
package net.enilink.vocab.owl;

import java.util.Set;

import net.enilink.composition.annotations.Iri;

import net.enilink.vocab.rdf.Property;

@Iri("http://www.w3.org/1999/02/22-rdf-syntax-ns#Property")
public interface OwlProperty extends Property, Thing {
	/** http://www.w3.org/2002/07/owl#equivalentProperty */
	@Iri("http://www.w3.org/2002/07/owl#equivalentProperty")
	public abstract Set<Property> getOwlEquivalentProperties();

	/** http://www.w3.org/2002/07/owl#equivalentProperty */
	public abstract void setOwlEquivalentProperties(Set<? extends Property> value);
}
