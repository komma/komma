/**
 * <copyright> 
 *
 * Copyright (c) 2002, 2009 IBM Corporation and others.
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
 * $Id: IItemPropertySource.java,v 1.5 2008/05/29 14:56:37 marcelop Exp $
 */
package net.enilink.komma.edit.provider;

import java.util.List;

/**
 * This is the interface is needed to populate property sheet items; it is the
 * same as IPropertySource except that the object is passed as the first
 * parameter for each method.
 */
public interface IItemPropertySource {
	/**
	 * This does the same thing as IPropertySource.getPropertyDescriptors.
	 */
	List<IItemPropertyDescriptor> getPropertyDescriptors(Object object);

	/**
	 * This returns the property descriptor with an
	 * {@link IItemPropertyDescriptor#getId(Object) ID} or
	 * {@link IItemPropertyDescriptor#getProperty(Object) feature} that matches
	 * the given ID.
	 */
	IItemPropertyDescriptor getPropertyDescriptor(Object object,
			Object propertyID);

	/**
	 * This returns the value to be edited.
	 */
	Object getEditableValue(Object object);
}
