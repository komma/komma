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
package net.enilink.komma.em.concepts;

import net.enilink.composition.annotations.Iri;
import net.enilink.composition.cache.annotations.Cacheable;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.vocab.rdf.Property;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IReference;

@Iri("http://www.w3.org/1999/02/22-rdf-syntax-ns#Property")
public interface IProperty extends Property, IResource {
	@Cacheable
	boolean isOrderedContainment();

	@Cacheable
	boolean isContainment();

	boolean isMany(IReference subject);

	boolean isRangeCompatible(IResource subject, Object object);

	boolean isRangeCompatible(Object object);

	boolean isDomainCompatible(Object object);

	IExtendedIterator<? extends IClass> getRanges(boolean direct);

	IExtendedIterator<? extends IClass> getNamedRanges(IEntity subject,
			boolean direct);

	@Cacheable(key = "komma:directSubProperty")
	IExtendedIterator<IProperty> getDirectSubProperties();

	@Cacheable(key = "komma:directSuperProperty")
	IExtendedIterator<IProperty> getDirectSuperProperties();

	@Cacheable(key = "komma:superProperty")
	IExtendedIterator<IProperty> getSuperProperties();

	@Cacheable(key = "komma:subProperty")
	IExtendedIterator<IProperty> getSubProperties();

	boolean hasListRange();
}
