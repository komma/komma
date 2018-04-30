/*******************************************************************************
 * Copyright (c) 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
