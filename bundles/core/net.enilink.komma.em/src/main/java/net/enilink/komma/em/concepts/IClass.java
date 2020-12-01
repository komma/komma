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
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.URI;
import net.enilink.vocab.owl.Class;
import net.enilink.vocab.owl.Restriction;

/**
 * An extended interfaces to beans that represent {@link Class OWL classes}.
 *
 * <p>
 * This interface contains methods for convenient access to instances,
 * properties as well as certain sub- or super-classes of an {@link Class OWL
 * class}.
 */
@Iri("http://www.w3.org/2000/01/rdf-schema#Class")
public interface IClass extends net.enilink.vocab.owl.Class, IResource {
	/**
	 * Returns the {@link IProperty properties} that are explicitly declared for
	 * this type either via <code>rdfs:domain</code> or by using an
	 * {@link Restriction OWL restriction}
	 * 
	 * @param includeInferred
	 *            if inferred knowledge should be included or not
	 * @return the RDF properties declared for this type
	 */
	IExtendedIterator<IProperty> getDeclaredProperties(boolean includeInferred);

	/**
	 * Returns instances of this type.
	 * 
	 * @return the instances of this type
	 */
	IExtendedIterator<IResource> getInstances();

	/**
	 * Returns instances of this type as plain {@link IReference references}.
	 * 
	 * <p>
	 * This method does not convert the results to beans and therefore saves the
	 * associated overhead.
	 * 
	 * @return the instances of this type as references
	 */
	IExtendedIterator<IReference> getInstancesAsReferences();

	/**
	 * Returns the subclasses of this type that have a {@link URI uri}.
	 * 
	 * @return the named subclasses
	 */
	IExtendedIterator<IClass> getNamedSubClasses();

	/**
	 * Returns only the direct subclasses of this type that have a {@link URI uri}.
	 * 
	 * <p>
	 * This method ensures that - even if inference is enabled - only the classes
	 * directly connected to this type via <code>rdfs:subClassOf</code> are
	 * returned.
	 * 
	 * @return the direct named subclasses of this type
	 */
	@Cacheable
	IExtendedIterator<IClass> getDirectNamedSubClasses();

	/**
	 * Returns either the direct or indirect subclasses of this type.
	 * 
	 * <p>
	 * The subclasses returned by this method may have a {@link URI uri} or not.
	 * 
	 * @param direct
	 *            whether to only include direct subclasses or also indirect
	 *            subclasses
	 * @param includeInferred
	 *            if inferred knowledge should be considered or not
	 * @return the direct or indirect subclasses of this type
	 */
	IExtendedIterator<IClass> getSubClasses(boolean direct, boolean includeInferred);

	/**
	 * Returns the indirect subclasses of this type that don't have any known
	 * further subclasses.
	 * 
	 * @param includeInferred
	 *            if inferred knowledge should be considered or not
	 * @return indirect subclasses of this type without further subclasses
	 * 
	 * @see #getSubClasses(boolean, boolean)
	 */
	IExtendedIterator<IClass> getLeafSubClasses(boolean includeInferred);

	/**
	 * Returns the named indirect subclasses of this type that don't have any known
	 * further subclasses.
	 * 
	 * @param includeInferred
	 *            if inferred knowledge should be considered or not
	 * @return named indirect subclasses of this type without further subclasses
	 * 
	 * @see #getNamedSubClasses()
	 */
	IExtendedIterator<IClass> getNamedLeafSubClasses(boolean includeInferred);

	/**
	 * Returns the super-classes of this type that have a {@link URI uri}.
	 * 
	 * @return the named super-classes
	 * 
	 * @see #getNamedSubClasses()
	 */
	@Cacheable
	IExtendedIterator<IClass> getNamedSuperClasses();

	/**
	 * Returns only the direct super-classes of this type that have a {@link URI
	 * uri}.
	 * 
	 * <p>
	 * This method ensures that - even if inference is enabled - only the classes
	 * where this type is a direct <code>rdfs:subClassOf</code> are returned.
	 * 
	 * @return the direct named super-classes of this type
	 * 
	 * @see #getDirectNamedSubClasses()
	 */
	@Cacheable(key = "komma:directNamedSuperClasses")
	IExtendedIterator<IClass> getDirectNamedSuperClasses();

	/**
	 * Returns either the direct or indirect super-classes of this type.
	 * 
	 * <p>
	 * The super-classes returned by this method may have a {@link URI uri} or not.
	 * For example, they also may contain {@link Restriction OWL restrictions}.
	 * 
	 * @param direct
	 *            whether to only include direct super-classes or also indirect
	 *            super-classes
	 * @param includeInferred
	 *            if inferred knowledge should be considered or not
	 * @return the direct or indirect super-classes of this type
	 * 
	 * @see #getSubClasses(boolean, boolean)
	 */
	IExtendedIterator<IClass> getSuperClasses(boolean direct, boolean includeInferred);

	/**
	 * Tests whether this type has any named subclasses or not.
	 * 
	 * @return <code>true</code> if this type has named subclasses, else
	 *         <code>false</code>
	 * 
	 * @see #getNamedSubClasses()
	 */
	@Cacheable(key = "komma:hasNamedSubClasses")
	Boolean hasNamedSubClasses();

	/**
	 * Tests whether this type has any declared properties or not.
	 * 
	 * @param includeInferred
	 *            if inferred knowledge should be considered or not
	 * @return <code>true</code> if this type has declared properties, else
	 *         <code>false</code>
	 * 
	 * @see #getDeclaredProperties(boolean)
	 */
	boolean hasDeclaredProperties(boolean includeInferred);

	/**
	 * Tests whether this type has any named subclasses or not.
	 * 
	 * @param includeInferred
	 *            if inferred knowledge should be considered or not
	 * @return <code>true</code> if this type has named subclasses, else
	 *         <code>false</code>
	 * 
	 * @see #getNamedSubClasses()
	 */
	boolean hasNamedSubClasses(boolean includeInferred);

	/**
	 * Tests whether this type has any subclasses or not.
	 * 
	 * @param includeInferred
	 *            if inferred knowledge should be considered or not
	 * @return <code>true</code> if this type has subclasses, else
	 *         <code>false</code>
	 * 
	 * @see #getSubClasses(boolean, boolean)
	 */
	boolean hasSubClasses(boolean includeInferred);

	/**
	 * Creates a new named instance of this type.
	 * 
	 * <p>
	 * This method adds the correspond <code>rdf:type</code> to the underlying store
	 * and creates a bean for this type.
	 * 
	 * @param uri
	 *            the uri of the RDF resource
	 * @return a bean for the newly created resource
	 */
	IResource newInstance(URI uri);

	/**
	 * Creates a new anonymous instance (blank node) of this type.
	 * 
	 * <p>
	 * This method adds the correspond <code>rdf:type</code> to the underlying store
	 * and creates a bean for this type.
	 * 
	 * @return a bean for the newly created resource
	 */
	IResource newInstance();

	/**
	 * Returns whether this type is abstract (marked with
	 * <code>komma:isAbstract true</code>) or not.
	 * 
	 * @return <code>true</code> if this type is abstract, else <code>false</code>
	 */
	boolean isAbstract();
}