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
package net.enilink.komma.model;

import java.util.List;
import java.util.Set;

import net.enilink.composition.annotations.Iri;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.komma.concepts.CONCEPTS;
import net.enilink.komma.concepts.IProperty;
import net.enilink.komma.concepts.IResource;
import net.enilink.komma.core.IEntity;

public interface IObject extends IEntity, IResource, IModelAware {
	@Iri(CONCEPTS.NAMESPACE + "containsTransitive")
	Set<IObject> getAllContents();

	IExtendedIterator<IProperty> getApplicableChildProperties();

	IObject getContainer();

	@Iri(CONCEPTS.NAMESPACE + "contains")
	Set<IObject> getContents();

	List<IObject> getOrderedContents();

	void setOrderedContents();

	@Iri(CONCEPTS.NAMESPACE + "precedes")
	Set<IObject> getPrecedes();

	@Iri(CONCEPTS.NAMESPACE + "precedesTransitive")
	Set<IObject> getPrecedesTransitive();

	void setContents(Set<IObject> contents);

	void setPrecedes(Set<IObject> precedes);
}