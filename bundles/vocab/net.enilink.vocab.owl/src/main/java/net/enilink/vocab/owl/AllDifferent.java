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

import java.util.List;

import net.enilink.composition.annotations.Iri;
import net.enilink.composition.properties.annotations.Type;

@Iri("http://www.w3.org/2002/07/owl#AllDifferent")
public interface AllDifferent extends Thing {

	/** http://www.w3.org/2002/07/owl#distinctMembers */
	@Iri("http://www.w3.org/2002/07/owl#distinctMembers")
	@Type("http://www.w3.org/1999/02/22-rdf-syntax-ns#List")
	public abstract List<Thing> getOwlDistinctMembers();

	/** http://www.w3.org/2002/07/owl#distinctMembers */
	public abstract void setOwlDistinctMembers(List<? extends Thing> value);

}
