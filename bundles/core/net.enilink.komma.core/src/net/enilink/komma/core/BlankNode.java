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
 * This class represents a generic blank node.
 * 
 * Only identical blank nodes are considered as equal.
 * 
 * @author Ken Wenzel
 */
public class BlankNode implements IReference {
	/**
	 * Returns <code>null</code>, since a blank node does not have an
	 * {@link URI}
	 */
	@Override
	public URI getURI() {
		return null;
	}
}
