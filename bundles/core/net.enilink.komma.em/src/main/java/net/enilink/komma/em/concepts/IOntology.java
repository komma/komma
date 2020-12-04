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

import java.util.Collection;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.vocab.owl.Ontology;

/**
 * An extended interface for beans representing {@link Ontology OWL ontologies}.
 *
 * <p>
 * This interface contains methods for convenient access to top-level classes
 * and properties.
 */
public interface IOntology extends Ontology {
	/**
	 * Returns the top-level class of this ontology.
	 * 
	 * @return the top-level classes
	 */
	Collection<IClass> getRootClasses();

	/**
	 * Returns the top-level properties of this ontology.
	 * 
	 * @return the top-level properties
	 */
	IExtendedIterator<IProperty> getRootProperties();

	/**
	 * Returns the top-level object properties of this ontology.
	 * 
	 * @return the top-level object properties
	 */
	IExtendedIterator<IProperty> getRootObjectProperties();

	/**
	 * Returns the top-level data-type properties of this ontology.
	 * 
	 * @return the top-level data-type properties
	 */
	IExtendedIterator<IProperty> getRootDatatypeProperties();
}
