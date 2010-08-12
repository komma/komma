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

import net.enilink.composition.annotations.Iri;
import net.enilink.composition.cache.annotations.Cacheable;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.vocab.rdfs.Resource;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.IValue;
import net.enilink.komma.util.Pair;

@Iri("http://www.w3.org/2000/01/rdf-schema#Resource")
public interface IResource extends Resource {
	void addProperty(IReference property, Object obj);

	@Cacheable
	Pair<Integer, Integer> getApplicableCardinality(IReference property);

	IExtendedIterator<IProperty> getApplicableProperties();

	int getCardinality(IReference property);

	IExtendedIterator<IClass> getNamedClasses();

	@Cacheable(key = "urn:komma:directNamedClasses")
	IExtendedIterator<IClass> getDirectNamedClasses();

	IExtendedIterator<IClass> getClasses();

	@Cacheable
	IExtendedIterator<IClass> getClasses(boolean includeInferred);

	@Cacheable(key = "urn:komma:directClasses")
	IExtendedIterator<IClass> getDirectClasses();

	IExtendedIterator<IValue> getPropertyValues(IReference property,
			boolean includeInferred);

	IExtendedIterator<IStatement> getPropertyStatements(IReference property,
			boolean includeInferred);

	boolean hasApplicableProperty(IReference property);

	boolean hasProperty(IReference property, Object obj, boolean includeInferred);

	boolean isOntLanguageTerm();

	boolean isPropertySet(IReference property, boolean includeInferred);

	void removeProperty(IReference property, Object obj);

	void removeProperty(IReference property);

	Object get(IReference property);

	void refresh(IReference property);

	void set(IReference property, Object value);
}
