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

/** Interface to represent RDF literals. */
public interface ILiteral extends IValue {

	/**
	 * Gets some value of this literal.
	 * 
	 * @return The literal's value as-is.
	 */
	Object getInstanceValue();

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
