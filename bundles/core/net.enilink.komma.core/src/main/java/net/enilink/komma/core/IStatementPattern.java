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
package net.enilink.komma.core;

/**
 * An pattern for an RDF statement that can be used for matching.
 */
public interface IStatementPattern {

	/**
	 * Compares this pattern object to another object.
	 * 
	 * @param other
	 *            The object to compare this pattern to.
	 * @return <tt>true</tt> if the other object is an instance of
	 *         {@link IStatementPattern} and if their subjects, predicates and
	 *         objects are equal.
	 */
	boolean equals(Object other);
	
	/**
	 * Compares a statement object to another statement object.
	 * 
	 * @param other
	 *            The object to compare this statement to.
	 * @return <tt>true</tt> if the other object is an instance of
	 *         {@link Statement} and if their subjects, predicates and objects
	 *         are equal.
	 */
	boolean equalsIgnoreContext(IStatementPattern other);

	/**
	 * Gets the context of this pattern.
	 * 
	 * @return The pattern's context, or <tt>null</tt>.
	 */
	IReference getContext();

	/**
	 * Gets the object of this pattern.
	 * 
	 * @return The pattern's object.
	 */
	Object getObject();

	/**
	 * Gets the predicate of this pattern.
	 * 
	 * @return The pattern's predicate.
	 */
	IReference getPredicate();

	/**
	 * Gets the subject of this pattern.
	 * 
	 * @return The pattern's subject.
	 */
	IReference getSubject();

	/**
	 * Computes the hash code of this pattern.
	 * 
	 * @return A hash code for the pattern.
	 */
	int hashCode();
}
