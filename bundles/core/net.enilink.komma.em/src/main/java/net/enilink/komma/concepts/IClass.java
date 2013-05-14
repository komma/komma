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
package net.enilink.komma.concepts;

import java.util.Collection;

import net.enilink.composition.annotations.Iri;
import net.enilink.composition.cache.annotations.Cacheable;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.URI;

@Iri("http://www.w3.org/2000/01/rdf-schema#Class")
public interface IClass extends net.enilink.vocab.owl.Class, IResource {
	IExtendedIterator<IProperty> getDeclaredProperties(boolean includeInferred);

	Collection<IResource> getInstances();
	
	Collection<IReference> getInstancesAsReferences();

	IExtendedIterator<IClass> getNamedSubClasses();

	@Cacheable
	IExtendedIterator<IClass> getDirectNamedSubClasses();

	IExtendedIterator<IClass> getSubClasses(boolean direct,
			boolean includeInferred);

	IExtendedIterator<IClass> getLeafSubClasses(boolean includeInferred);

	IExtendedIterator<IClass> getNamedLeafSubClasses(boolean includeInferred);

	@Cacheable
	IExtendedIterator<IClass> getNamedSuperClasses();

	@Cacheable(key = "komma:directNamedSuperClasses")
	IExtendedIterator<IClass> getDirectNamedSuperClasses();

	IExtendedIterator<IClass> getSuperClasses(boolean direct,
			boolean includeInferred);

	@Cacheable(key = "komma:hasNamedSubClasses")
	Boolean hasNamedSubClasses();

	boolean hasDeclaredProperties(boolean includeInferred);

	boolean hasNamedSubClasses(boolean includeInferred);

	boolean hasSubClasses(boolean includeInferred);

	IResource newInstance(URI uri);

	IResource newInstance();

	boolean isAbstract();
}