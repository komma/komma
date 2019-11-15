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
import java.util.Set;

import net.enilink.composition.annotations.Iri;
import net.enilink.composition.properties.annotations.Type;

@Iri("http://www.w3.org/2002/07/owl#Class")
public interface Class extends net.enilink.vocab.rdfs.Class, Thing {

	/** http://www.w3.org/2002/07/owl#complementOf */
	@Iri("http://www.w3.org/2002/07/owl#complementOf")
	public abstract Class getOwlComplementOf();

	/** http://www.w3.org/2002/07/owl#complementOf */
	public abstract void setOwlComplementOf(Class value);

	/** http://www.w3.org/2002/07/owl#disjointWith */
	@Iri("http://www.w3.org/2002/07/owl#disjointWith")
	public abstract Set<Class> getOwlDisjointWith();

	/** http://www.w3.org/2002/07/owl#disjointWith */
	public abstract void setOwlDisjointWith(Set<? extends Class> value);

	/** http://www.w3.org/2002/07/owl#equivalentClass */
	@Iri("http://www.w3.org/2002/07/owl#equivalentClass")
	public abstract Set<Class> getOwlEquivalentClasses();

	/** http://www.w3.org/2002/07/owl#equivalentClass */
	public abstract void setOwlEquivalentClasses(Set<? extends Class> value);

	/** http://www.w3.org/2002/07/owl#intersectionOf */
	@Iri("http://www.w3.org/2002/07/owl#intersectionOf")
	@Type("http://www.w3.org/1999/02/22-rdf-syntax-ns#List")
	public abstract List<Class> getOwlIntersectionOf();

	/** http://www.w3.org/2002/07/owl#intersectionOf */
	public abstract void setOwlIntersectionOf(List<? extends Class> value);

	/** http://www.w3.org/2002/07/owl#oneOf */
	@Iri("http://www.w3.org/2002/07/owl#oneOf")
	@Type("http://www.w3.org/1999/02/22-rdf-syntax-ns#List")
	public abstract List<Object> getOwlOneOf();

	/** http://www.w3.org/2002/07/owl#oneOf */
	public abstract void setOwlOneOf(List<?> value);

	/** http://www.w3.org/2002/07/owl#unionOf */
	@Iri("http://www.w3.org/2002/07/owl#unionOf")
	@Type("http://www.w3.org/1999/02/22-rdf-syntax-ns#List")
	public abstract List<Class> getOwlUnionOf();

	/** http://www.w3.org/2002/07/owl#unionOf */
	public abstract void setOwlUnionOf(List<? extends Class> value);

}
