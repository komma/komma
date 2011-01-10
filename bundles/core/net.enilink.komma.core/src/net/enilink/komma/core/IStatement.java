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
package net.enilink.komma.core;

/**
 * An RDF statement. A statement is equal to another statement if the subjects,
 * predicates and objects are equal.
 */
public interface IStatement {
	/**
	 * Compares a statement object to another object.
	 * 
	 * @param other
	 *            The object to compare this statement to.
	 * @return <tt>true</tt> if the other object is an instance of
	 *         {@link IStatement} and if their subjects, predicates and objects
	 *         are equal.
	 */
	boolean equals(Object other);

	/**
	 * Gets the context of this statement.
	 * 
	 * @return The statement's context, or <tt>null</tt>.
	 */
	IReference getContext();

	/**
	 * Gets the object of this statement.
	 * 
	 * @return The statement's object.
	 */
	Object getObject();

	/**
	 * Gets the predicate of this statement.
	 * 
	 * @return The statement's predicate.
	 */
	IReference getPredicate();

	/**
	 * Gets the subject of this statement.
	 * 
	 * @return The statement's subject.
	 */
	IReference getSubject();

	/**
	 * Computes the hash code of this statement.
	 * 
	 * @return A hash code for the statement.
	 */
	int hashCode();

	/**
	 * Returns whether this statement is inferred or not
	 * 
	 * @return <code>true<</code> if this statement is inferred
	 */
	boolean isInferred();
}
