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

import net.enilink.vocab.rdfs.Resource;

@Iri("http://www.w3.org/2002/07/owl#Thing")
public interface Thing extends Resource {

	/** http://www.w3.org/2002/07/owl#differentFrom */
	@Iri("http://www.w3.org/2002/07/owl#differentFrom")
	public abstract Set<Thing> getOwlDifferentFrom();

	/** http://www.w3.org/2002/07/owl#differentFrom */
	public abstract void setOwlDifferentFrom(Set<? extends Thing> value);

	/** http://www.w3.org/2002/07/owl#sameAs */
	@Iri("http://www.w3.org/2002/07/owl#sameAs")
	public abstract Set<Thing> getOwlSameAs();

	/** http://www.w3.org/2002/07/owl#sameAs */
	public abstract void setOwlSameAs(Set<? extends Thing> value);

	/** http://www.w3.org/2002/07/owl#versionInfo */
	@Iri("http://www.w3.org/2002/07/owl#versionInfo")
	public abstract Set<Object> getOwlVersionInfo();

	/** http://www.w3.org/2002/07/owl#versionInfo */
	public abstract void setOwlVersionInfo(Set<?> value);

}
