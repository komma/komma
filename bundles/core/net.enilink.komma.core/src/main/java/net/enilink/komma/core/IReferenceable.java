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
 * Represents an entity that has an associated
 * reference as unique identifier.
 */
public interface IReferenceable {
	/**
	 * The reference (either a {@link URI} or a blank node) that
	 * can be used as identifier to refer to this entity. 
	 * 
	 * @return The reference for this entity
	 */
	IReference getReference();
}
