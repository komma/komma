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
package net.enilink.komma.em.concepts;

import java.util.Comparator;
import java.util.Set;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.commons.util.Pair;
import net.enilink.composition.annotations.Iri;
import net.enilink.composition.cache.annotations.Cacheable;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.IValue;
import net.enilink.komma.core.URI;
import net.enilink.vocab.komma.KOMMA;
import net.enilink.vocab.owl.OWL;
import net.enilink.vocab.rdf.RDF;
import net.enilink.vocab.rdfs.RDFS;
import net.enilink.vocab.rdfs.Resource;
import net.enilink.vocab.xmlschema.XMLSCHEMA;

/**
 * An extended interface for beans representing arbitrary {@link Resource RDF
 * resources}.
 *
 * <p>
 * This interface contains methods for reflective access to associated
 * properties as well as containment and precedence relationships.
 */
@Iri("http://www.w3.org/2000/01/rdf-schema#Resource")
public interface IResource extends Resource {
	/**
	 * Comparator to sort resources according to their "semantic" level.
	 */
	public static final Comparator<IReference> RANK_COMPARATOR = new Comparator<IReference>() {
		URI[] defaultNamespaces = { XMLSCHEMA.NAMESPACE_URI, RDF.NAMESPACE_URI, RDFS.NAMESPACE_URI, OWL.NAMESPACE_URI };

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

	/**
	 * Adds a statement for the given property and value to the underlying store.
	 * 
	 * @param property
	 *            an RDF property
	 * @param value
	 *            the property value
	 */
	void addProperty(IReference property, Object value);

	/**
	 * Determines the applicable cardinality [min, max] for the property when used
	 * with this resource.
	 * 
	 * <p>
	 * Considered restrictions are:
	 * <ul>
	 * <li>owl:cardinality</li>
	 * <li>owl:minCardinality</li>
	 * <li>owl:maxCardinality</li>
	 * <li>owl:qualifiedCardinality</li>
	 * <li>owl:minQualifiedCardinality</li>
	 * <li>owl:maxQualifiedCardinality</li>
	 * </ul>
	 * 
	 * @param property
	 *            the RDF property
	 * @return the cardinality range [min, max] for the given property, if
	 *         unconstrained then max == {@link Integer#MAX_VALUE}
	 */
	@Cacheable
	Pair<Integer, Integer> getApplicableCardinality(IReference property);

	/**
	 * Returns the relevant properties for this resource by considering
	 * <code>rdfs:domain</code> and OWL restrictions.
	 * 
	 * @return the relevant properties for this resource
	 */
	IExtendedIterator<IProperty> getRelevantProperties();

	/**
	 * Returns the actual cardinality of a property for this resource.
	 * 
	 * <p>
	 * This method counts the number of statements where this resource is the
	 * subject and the given property is the predicate.
	 * 
	 * @param property
	 *            an RDF property
	 * @return the actual cardinality of the property for this resource
	 */
	int getCardinality(IReference property);

	/**
	 * Returns all named (explicit or inferred) types of this resource.
	 * 
	 * @return the named types of this resource
	 */
	IExtendedIterator<IClass> getNamedClasses();

	/**
	 * Returns all named (explicit or inferred) top-level types of this resource.
	 * 
	 * <p>
	 * This methods filters the set of named classes to remove any sub-types.
	 * 
	 * @return the direct named types of this resource
	 * 
	 * @see IResource#getNamedClasses()
	 */
	@Cacheable(key = "komma:directNamedClasses")
	IExtendedIterator<IClass> getDirectNamedClasses();

	/**
	 * Returns all named or anonymous (explicit or inferred) types of this resource.
	 * 
	 * <p>
	 * This method returns the same resources as {@link #getRdfTypes()} but converts
	 * them to instances of {@link IClass}.
	 * 
	 * @return the RDF types of this resource as instances of {@link IClass}
	 */
	IExtendedIterator<IClass> getClasses();

	/**
	 * Returns all named or anonymous explicit types of this resource.
	 * 
	 * @return the explicit RDF types of this resource as instances of
	 *         {@link IClass}
	 */
	@Cacheable
	IExtendedIterator<IClass> getClasses(boolean includeInferred);

	/**
	 * Returns all (explicit or inferred) top-level types of this resource.
	 * 
	 * <p>
	 * This methods filters the set of classes to remove any sub-types.
	 * 
	 * @return the direct named types of this resource
	 * 
	 * @see IResource#getClasses()
	 */
	@Cacheable(key = "komma:directClasses")
	IExtendedIterator<IClass> getDirectClasses();

	/**
	 * Returns the values of the given property for this resource.
	 * 
	 * @param property
	 *            an RDF property
	 * @param includeInferred
	 *            whether to consider inferred knowledge or not
	 * @return an iterator of the property values
	 */
	IExtendedIterator<IValue> getPropertyValues(IReference property, boolean includeInferred);

	/**
	 * Returns statements having as <b>subject</b> this resource and as
	 * <b>predicate</b> the given property.
	 * 
	 * @param property
	 *            an RDF property
	 * @param includeInferred
	 *            whether to consider inferred knowledge or not
	 * @return an iterator of the property statements
	 */
	IExtendedIterator<IStatement> getPropertyStatements(IReference property, boolean includeInferred);

	/**
	 * Returns statements having as <b>object</b> this resource and as
	 * <b>predicate</b> the given property.
	 * 
	 * @param property
	 *            an RDF property
	 * @param includeInferred
	 *            whether to consider inferred knowledge or not
	 * @return an iterator of the inverse property statements
	 */
	IExtendedIterator<IStatement> getInversePropertyStatements(IReference property, boolean includeInferred);

	/**
	 * Returns statements having as <b>object</b> this resource and as
	 * <b>predicate</b> the given property.
	 * 
	 * @param property
	 *            an RDF property
	 * @param filterSymmetric
	 *            controls whether statements with equal subjects and objects should
	 *            be excluded or not
	 * @param includeInferred
	 *            whether to consider inferred knowledge or not
	 * @return an iterator of the inverse property statements
	 */
	IExtendedIterator<IStatement> getInversePropertyStatements(IReference property, boolean filterSymmetric,
			boolean includeInferred);

	/**
	 * Tests whether the given property can be applied to this resource or not.
	 * 
	 * <p>
	 * A property is considered applicable if its domain is compatible with this
	 * resource's types or there exists an OWL restriction for the property among
	 * this resource's types.
	 * 
	 * @param property
	 *            an RDF property
	 * @return <code>true</code> if the property is considered applicable for this
	 *         resource, else <code>false</code>
	 */
	boolean hasApplicableProperty(IReference property);

	/**
	 * Tests whether an explicit or inferred statement exists for this resource, the
	 * property and the given value.
	 * 
	 * @param property
	 *            an RDF property
	 * @param value
	 *            the property value or <code>null</code>
	 * @param includeInferred
	 *            whether to consider inferred knowledge or not
	 * @return <code>true</code> if the statement exists, else <code>false</code>
	 */
	boolean hasProperty(IReference property, Object value, boolean includeInferred);

	/**
	 * Tests whether an explicit or inferred statement exists for this resource, the
	 * property and an arbitrary object.
	 * 
	 * @param property
	 *            an RDF property
	 * @param includeInferred
	 *            whether to consider inferred knowledge or not
	 * @return <code>true</code> if the statement exists, else <code>false</code>
	 */
	boolean hasProperty(IReference property, boolean includeInferred);

	/**
	 * Returns whether the {@link URI} of this resource has either the RDF, RDFS,
	 * OWL or XMLSchema namespace.
	 *
	 * @return <code>true</code> if this resource has a W3C standard namespace, else
	 *         <code>false</code>
	 */
	boolean isOntLanguageTerm();

	/**
	 * Removes the statement(s) for this resource, the property and the given value.
	 * 
	 * @param property
	 *            an RDF property
	 * @param value
	 *            the property value or <code>null</code>
	 */
	void removeProperty(IReference property, Object value);

	/**
	 * Removes the statement(s) for this resource, the property and arbitrary
	 * values.
	 * 
	 * @param property
	 *            an RDF property
	 */
	void removeProperty(IReference property);

	/**
	 * Returns the value of the given RDF property.
	 * 
	 * <p>
	 * This method determines the applicable cardinality of the property for this
	 * resource, e.g. by using {@link #getApplicableCardinality(IReference)}, and
	 * then returns either a single value (bean or literal value) or an instance of
	 * {@link Set} for multiple values.
	 * 
	 * @param property
	 *            an RDF property
	 * @return either a single property value or a {@link Set set of values}
	 */
	Object get(IReference property);

	/**
	 * Returns the first value of the given RDF property.
	 * 
	 * <p>
	 * If the RDF property has multiple values then only one of these is returned.
	 * In this case the order of the values may depend on the underlying store and
	 * is not predictable.
	 * 
	 * @param property
	 *            an RDF property
	 * @return a property value or <code>null</code>
	 */
	Object getSingle(IReference property);

	/**
	 * Returns a mutable set for the values of the given RDF property.
	 * 
	 * @param property
	 *            an RDF property
	 * @return a mutable set for the property values
	 */
	Set<Object> getAsSet(IReference property);

	/**
	 * Clears any cached state for the given RDF property.
	 * 
	 * <p>
	 * Usually KOMMA ensures that the cached state is automatically invalidated. But
	 * this method may be required if the property values are inferred or the
	 * underlying store was externally modified.
	 * 
	 * @param property
	 *            an RDF property
	 */
	void refresh(IReference property);

	/**
	 * Sets the value of a single- or multi-valued property.
	 * 
	 * @param property
	 *            an RDF property
	 * @param value
	 *            a single property value or a collection of multiple values
	 */
	void set(IReference property, Object value);

	/* support for containment and partial ordering */

	/**
	 * Returns the transitive contents of this resource that are 
	 * defined via <code>komma:contains</code>.
	 * 
	 * @return all transitive contents of this resource
	 */
	@Iri(KOMMA.NAMESPACE + "containsTransitive")
	Set<IResource> getAllContents();

	IExtendedIterator<IProperty> getApplicableChildProperties();

	/**
	 * Returns the container resource that <code>komma:contains</code> this
	 * resource.
	 * 
	 * @return the container resource or <code>null</code>
	 */
	IResource getContainer();

	/**
	 * Returns a mutable set for the contained resources.
	 * 
	 * <p>
	 * Containment is specified via the property <code>komma:contains</code>.
	 * 
	 * @return a mutable set for the contained resources
	 */
	@Iri(KOMMA.NAMESPACE + "contains")
	Set<IResource> getContents();

	/**
	 * Sets the contained resources.
	 * 
	 * <p>
	 * Containment is specified via the property <code>komma:contains</code>.
	 * 
	 * @param contents the contents of this resource
	 */
	void setContents(Set<IResource> contents);
}
