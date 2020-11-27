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
 * An RDF statement. A statement is equal to another statement if the subjects,
 * predicates and objects are equal.
 * 
 * <p>
 * The {@link Iterable} interface is implemented to simplify interactions with
 * methods that require multiple statements.
 */
public interface IStatement extends IStatementPattern, Iterable<IStatement> {
	/**
	 * Returns whether this statement is inferred or not.
	 * 
	 * @return <code>true</code> if this statement is inferred
	 */
	boolean isInferred();
}
