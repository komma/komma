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

/** Interface to represent RDF literals. */
public interface ILiteral extends IValue {

	/**
	 * Gets the label of this literal.
	 * 
	 * @return The literal's label.
	 */
	String getLabel();

	/**
	 * Gets the datatype for this literal.
	 * 
	 * @return The datatype for this literal, or <tt>null</tt> if it doesn't
	 *         have one.
	 */
	URI getDatatype();

	/**
	 * Gets the language tag for this literal, normalized to lower case.
	 * 
	 * @return The language tag for this literal, or <tt>null</tt> if it doesn't
	 *         have one.
	 */
	String getLanguage();

}
