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
public interface IStatement extends IStatementPattern {
	/**
	 * Returns whether this statement is inferred or not.
	 * 
	 * @return <code>true</code> if this statement is inferred
	 */
	boolean isInferred();
}
