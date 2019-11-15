/**
 * <copyright> 
 *
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 * 
 * Contributors: 
 *   IBM - Initial API and implementation
 *
 * </copyright>
 *
 * $Id: IWrapperItemProvider.java,v 1.5 2007/06/14 18:32:42 emerks Exp $
 */
package net.enilink.komma.edit.provider;

import net.enilink.komma.core.IReference;

/**
 * The base interface for a value wrapper that can implement item provider
 * interfaces.
 */
public interface IWrapperItemProvider extends IDisposable {
	/**
	 * Returns the wrapped value.
	 */
	Object getValue();

	/**
	 * Returns the object that owns the value.
	 */
	Object getOwner();

	/**
	 * Returns the property through which the value can be set and retrieved, or
	 * null if the property is unknown or not applicable.
	 */
	IReference getProperty();

	/**
	 * The index at which the value is located, or
	 * {@link net.enilink.komma.edit.command.CommandParameter#NO_INDEX} if the
	 * index isn't known to the wrapper. If {@link #getProperty} is non-null,
	 * this index is within that property.
	 */
	int getIndex();

	/**
	 * Sets the index. Has no effect if the index isn't known to the wrapper.
	 */
	void setIndex(int index);

	/**
	 * Returns <code>true</code> if this item represents inferred knowledge
	 */
	boolean isInferred();
}
