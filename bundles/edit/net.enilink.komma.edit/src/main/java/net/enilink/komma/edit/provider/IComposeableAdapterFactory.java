/**
 * <copyright> 
 *
 * Copyright (c) 2002, 2009 IBM Corporation and others.
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
 * $Id: ComposeableAdapterFactory.java,v 1.2 2005/06/08 06:17:05 nickb Exp $
 */
package net.enilink.komma.edit.provider;

import net.enilink.komma.common.adapter.IAdapterFactory;

/**
 * This provides support that allows a factory to be composed into a
 * {@link ComposedAdapterFactory} that serves the union of the model objects
 * from different packages.
 */
public interface IComposeableAdapterFactory extends IAdapterFactory {
	/**
	 * This convenience method returns the first adapter factory that doesn't
	 * have a parent, i.e., the root.
	 */
	public IComposeableAdapterFactory getRootAdapterFactory();

	/**
	 * This sets the direct parent adapter factory into which this factory is
	 * composed.
	 */
	public void setParentAdapterFactory(
			ComposedAdapterFactory parentAdapterFactory);
}
