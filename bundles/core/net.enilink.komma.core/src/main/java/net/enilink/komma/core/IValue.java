/*******************************************************************************
 * Copyright (c) 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.core;

/** Interface to represent arbitrary RDF values. */
public interface IValue {
	/**
	 * Returns the same value as {@link #getURI()}<code>.toString()</code> for named nodes, a blank node
	 * identifier for blank nodes and a string representation for literals.
	 * 
	 * @return URI for named nodes, identifier for blank nodes and string
	 *         representation for literals.
	 */
	String toString();
}
