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

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import net.enilink.composition.annotations.Iri;
import net.enilink.composition.cache.annotations.Cacheable;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.vocab.owl.OWL;
import net.enilink.vocab.rdf.RDF;
import net.enilink.vocab.rdfs.RDFS;
import net.enilink.vocab.rdfs.Resource;
import net.enilink.vocab.xmlschema.XMLSCHEMA;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.IValue;
import net.enilink.komma.core.URI;
import net.enilink.komma.util.Pair;

@Iri("http://www.w3.org/2000/01/rdf-schema#Resource")
public interface IResource extends Resource {
	/**
	 * Comparator to sort resources according to their "semantic" level.
	 */
	public static final Comparator<IReference> RANK_COMPARATOR = new Comparator<IReference>() {
		URI[] defaultNamespaces = { XMLSCHEMA.NAMESPACE_URI, RDF.NAMESPACE_URI,
				RDFS.NAMESPACE_URI, OWL.NAMESPACE_URI };

		@Override
		public int compare(IReference a, IReference b) {
			URI aUri = a.getURI();
			URI bUri = b.getURI();
			if (aUri == null) {
				if (bUri != null) {
					return 1;
				}
				return 0;
			} else if (bUri == null) {
				return -1;
			}
			return getRank(bUri.namespace()) - getRank(aUri.namespace());
		}

		int getRank(URI namespace) {
			for (int i = 0; i < defaultNamespaces.length; i++) {
				if (namespace.equals(defaultNamespaces[i])) {
					return i;
				}
			}
			return defaultNamespaces.length + 1;
		}
	};

	@Iri("http://enilink.net/vocab/komma#image")
	java.net.URI getImage();

	void addProperty(IReference property, Object obj);

	@Cacheable
	Pair<Integer, Integer> getApplicableCardinality(IReference property);

	IExtendedIterator<IProperty> getRelevantProperties();

	int getCardinality(IReference property);

	IExtendedIterator<IClass> getNamedClasses();

	@Cacheable(key = "komma:directNamedClasses")
	IExtendedIterator<IClass> getDirectNamedClasses();

	IExtendedIterator<IClass> getClasses();

	@Cacheable
	IExtendedIterator<IClass> getClasses(boolean includeInferred);

	@Cacheable(key = "komma:directClasses")
	IExtendedIterator<IClass> getDirectClasses();

	IExtendedIterator<IValue> getPropertyValues(IReference property,
			boolean includeInferred);

	IExtendedIterator<IStatement> getPropertyStatements(IReference property,
			boolean includeInferred);

	IExtendedIterator<IStatement> getInversePropertyStatements(
			IReference property, boolean includeInferred);

	IExtendedIterator<IStatement> getInversePropertyStatements(
			IReference property, boolean filterSymmetric,
			boolean includeInferred);

	boolean hasApplicableProperty(IReference property);

	boolean hasProperty(IReference property, Object obj, boolean includeInferred);

	boolean isOntLanguageTerm();

	boolean isPropertySet(IReference property, boolean includeInferred);

	void removeProperty(IReference property, Object obj);

	void removeProperty(IReference property);

	Object get(IReference property);

	Set<Object> getAsSet(IReference property);

	void refresh(IReference property);

	void set(IReference property, Object value);

	/* support for containment and partial ordering */

	@Iri(CONCEPTS.NAMESPACE + "containsTransitive")
	Set<IResource> getAllContents();

	IExtendedIterator<IProperty> getApplicableChildProperties();

	IResource getContainer();

	@Iri(CONCEPTS.NAMESPACE + "contains")
	Set<IResource> getContents();

	List<IResource> getOrderedContents();

	void setOrderedContents();

	@Iri(CONCEPTS.NAMESPACE + "precedes")
	Set<IResource> getPrecedes();

	@Iri(CONCEPTS.NAMESPACE + "precedesTransitive")
	Set<IResource> getPrecedesTransitive();

	void setContents(Set<IResource> contents);

	void setPrecedes(Set<IResource> precedes);
}
