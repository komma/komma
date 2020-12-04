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

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.composition.annotations.Iri;
import net.enilink.composition.cache.annotations.Cacheable;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IReference;
import net.enilink.vocab.rdf.Property;

/**
 * An extended interface for beans representing {@link Property RDF
 * properties}.
 *
 * <p>
 * This interface contains methods for convenient access to the domain, range as
 * well as certain sub- or super-properties of a {@link Property RDF property}.
 */
@Iri("http://www.w3.org/1999/02/22-rdf-syntax-ns#Property")
public interface IProperty extends Property, IResource {
	/**
	 * Returns whether this is an ordered containment property or not.
	 * 
	 * <p>
	 * The property <code>komma:orderedContains</code> or any sub-properties are considered
	 * to be ordered containment properties.
	 * 
	 * @return <code>true</code> if this is an ordered containment property, else <code>false</code>
	 */
	@Cacheable
	boolean isOrderedContainment();

	/**
	 * Returns whether this is a containment property or not.
	 * 
	 * <p>
	 * The property <code>komma:contains</code> or any sub-properties are considered
	 * to be containment properties.
	 * 
	 * @return <code>true</code> if this is a containment property, else <code>false</code>
	 */
	@Cacheable
	boolean isContainment();

	/**
	 * Tests whether this property allows multiple values or not.
	 * 
	 * <p>
	 * The property allows multiple values if it is no functional property, there
	 * exists no OWL restriction for its cardinality or it [{@link #hasListRange()
	 * has a list range}.
	 * 
	 * @param subject
	 *            the target subject of this property.
	 * @return <code>true</code> if this property may have multiple values, else
	 *         <code>false</code>
	 */
	boolean isMany(IReference subject);

	/**
	 * Tests whether the local (defined as OWL restriction on <code>subject</code>)
	 * or global range of this property is compatible with the given object.
	 * 
	 * @param subject
	 *            the possible target object of this property
	 * @param object
	 *            the possible value of this property
	 * @return <code>true</code> if the <code>object</code> is compatible with the
	 *         local or global range of this property, else <code>false</code>
	 */
	boolean isRangeCompatible(IResource subject, Object object);

	/**
	 * Tests whether the <code>rdfs:range</code> of this property is compatible with
	 * the given object.
	 * 
	 * @param object
	 *            the possible value of this property
	 * @return <code>true</code> if the <code>object</code> is compatible with this
	 *         property's range, else <code>false</code>
	 */
	boolean isRangeCompatible(Object object);

	/**
	 * Tests whether the <code>rdfs:domain</code> of this property is compatible
	 * with the given subject.
	 * 
	 * @param subject
	 *            the possible target subject of this property
	 * @return <code>true</code> if the <code>subject</code> is compatible with this
	 *         property's domain, else <code>false</code>
	 */
	boolean isDomainCompatible(Object subject);

	/**
	 * Returns the global ranges for this property.
	 * 
	 * @param direct
	 *            whether to include only the directly specified types or also any
	 *            subtypes
	 * @return the global ranges for this property
	 */
	IExtendedIterator<? extends IClass> getRanges(boolean direct);

	/**
	 * Returns any local (defined as OWL restriction) or global ranges for this
	 * property when applied to the given <code>subject</code>.
	 * 
	 * @param subject
	 *            the subject this property should be applied to
	 * @param direct
	 *            whether to include only the directly specified types or also any
	 *            subtypes
	 * @return the named ranges for this property when applied to the given subject
	 */
	IExtendedIterator<? extends IClass> getNamedRanges(IEntity subject, boolean direct);

	/**
	 * Returns only the direct sub-properties of this property.
	 * 
	 * <p>
	 * This method ensures that - even if inference is enabled - only the properties
	 * directly connected to this property via <code>rdfs:subPropertyOf</code> are
	 * returned.
	 * 
	 * @return the direct sub-properties of this property
	 */
	@Cacheable(key = "komma:directSubProperty")
	IExtendedIterator<IProperty> getDirectSubProperties();

	/**
	 * Returns only the direct super-properties of this property.
	 * 
	 * <p>
	 * This method ensures that - even if inference is enabled - only the properties
	 * where this property is a direct <code>rdfs:subPropertyOf</code> are returned.
	 * 
	 * @return the direct super-properties of this property
	 */
	@Cacheable(key = "komma:directSuperProperty")
	IExtendedIterator<IProperty> getDirectSuperProperties();

	/**
	 * Returns direct or indirect super-properties of this property.
	 * 
	 * @return the direct or indirect super-properties of this property
	 */
	@Cacheable(key = "komma:superProperty")
	IExtendedIterator<IProperty> getSuperProperties();

	/**
	 * Returns direct or indirect sub-properties of this property.
	 * 
	 * @return the direct or indirect sub-properties of this property
	 */
	@Cacheable(key = "komma:subProperty")
	IExtendedIterator<IProperty> getSubProperties();

	/**
	 * Tests whether the <code>rdfs:range</code> of this property is
	 * <code>rdf:List</code> or <code>rdfs:Container</code>.
	 * 
	 * @return <code>true</code> if this property has a (global) list range, else
	 *         <code>false</code>
	 */
	boolean hasListRange();
}
