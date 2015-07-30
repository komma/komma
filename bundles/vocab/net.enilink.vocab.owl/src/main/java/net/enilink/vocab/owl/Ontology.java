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
package net.enilink.vocab.owl;

import java.util.Set;

import net.enilink.composition.annotations.Iri;

@Iri("http://www.w3.org/2002/07/owl#Ontology")
public interface Ontology extends Thing {


	/** http://www.w3.org/2002/07/owl#backwardCompatibleWith */
	@Iri("http://www.w3.org/2002/07/owl#backwardCompatibleWith")
	public abstract Set<Ontology> getOwlBackwardCompatibleWith();

	/** http://www.w3.org/2002/07/owl#backwardCompatibleWith */
	public abstract void setOwlBackwardCompatibleWith(Set<? extends Ontology> value);


	/** http://www.w3.org/2002/07/owl#imports */
	@Iri("http://www.w3.org/2002/07/owl#imports")
	public abstract Set<Ontology> getOwlImports();

	/** http://www.w3.org/2002/07/owl#imports */
	public abstract void setOwlImports(Set<? extends Ontology> value);


	/** http://www.w3.org/2002/07/owl#incompatibleWith */
	@Iri("http://www.w3.org/2002/07/owl#incompatibleWith")
	public abstract Set<Ontology> getOwlIncompatibleWith();

	/** http://www.w3.org/2002/07/owl#incompatibleWith */
	public abstract void setOwlIncompatibleWith(Set<? extends Ontology> value);


	/** http://www.w3.org/2002/07/owl#priorVersion */
	@Iri("http://www.w3.org/2002/07/owl#priorVersion")
	public abstract Set<Ontology> getOwlPriorVersion();

	/** http://www.w3.org/2002/07/owl#priorVersion */
	public abstract void setOwlPriorVersion(Set<? extends Ontology> value);

}
