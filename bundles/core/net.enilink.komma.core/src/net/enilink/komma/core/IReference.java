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
 * Represents a reference to a resource.
 * 
 * This is simply a value-object and does not implement any behavior.
 * 
 */
public interface IReference extends IValue {
	/**
	 * Return an URI for named nodes and <code>null</code> for blank nodes.
	 * 
	 * @return URI for named nodes, <code>null</code> for blank nodes.
	 */
	URI getURI();
}