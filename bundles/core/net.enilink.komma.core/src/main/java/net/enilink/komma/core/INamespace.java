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

/**
 * Represents an association of a prefix to a namespace URI.
 * 
 */
public interface INamespace {
	/**
	 * Returns the prefix of this namespace declaration.
	 * 
	 * @return The prefix
	 */
	String getPrefix();

	/**
	 * Returns the associated URI of this namespace declaration.
	 * 
	 * @return The associated URI
	 */
	URI getURI();

	/**
	 * Returns whether this namespace is explicit, and thus changeable, or in
	 * some way derived, and thus read-only.
	 * 
	 * @return <code>true</code> if this statement is inferred
	 */
	boolean isDerived();
}
