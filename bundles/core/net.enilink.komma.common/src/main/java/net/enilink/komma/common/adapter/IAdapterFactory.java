/**
 * <copyright>
 *
 * Copyright (c) 2002, 2010 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   IBM - Initial API and implementation
 *
 * </copyright>
 *
 * $Id: AdapterFactory.java,v 1.3 2005/06/08 06:19:08 nickb Exp $
 */
package net.enilink.komma.common.adapter;

/**
 * A factory for creating adapters.
 */
public interface IAdapterFactory {
	/**
	 * Returns whether this factory supports adapters for the given type.
	 * 
	 * @param type
	 *            the key indicating the type of adapter in question.
	 * @return whether this factory supports adapters for the given type.
	 */
	boolean isFactoryForType(Object type);

	/**
	 * Returns either an associated adapter for the object, or the object
	 * itself, depending on whether the object supports an adapter of the given
	 * type. This is essentially just a convenience method that allows a factory
	 * to act as a filter for converting objects to adapters.
	 * 
	 * @param object
	 *            arbitrary object to adapt.
	 * @param type
	 *            the key indicating the type of adapter required.
	 * @return either an associated adapter or the object itself.
	 */
	Object adapt(Object object, Object type);
}
