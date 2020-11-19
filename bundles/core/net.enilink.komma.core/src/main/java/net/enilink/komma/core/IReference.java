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
 * Represents a reference to a resource.
 * <p>
 * This is simply a value-object and does not implement any behavior.
 */
public interface IReference extends IValue {
	/**
	 * Return a {@link URI} for named nodes and <code>null</code> for blank nodes.
	 * 
	 * @return URI for named nodes, <code>null</code> for blank nodes.
	 */
	URI getURI();
}