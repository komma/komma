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
 * $Id: PropertySource.java,v 1.3 2006/12/28 06:50:05 marcelop Exp $
 */
package net.enilink.komma.edit.ui.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;

import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.edit.provider.IItemPropertyDescriptor;
import net.enilink.komma.edit.provider.IItemPropertySource;

/**
 * This is used to encapsulate an {@link IItemPropertySource} along with the
 * object for which it is an item property source and make it behave like an
 * {@link org.eclipse.ui.views.properties.IPropertySource}.
 */
public class PropertySource implements IPropertySource {
	/**
	 * This is the object for which this class is a property source.
	 */
	protected Object object;

	protected IAdapterFactory adapterFactory;

	/**
	 * This is the descriptor to which we will delegate all the
	 * {@link org.eclipse.ui.views.properties.IPropertySource} methods.
	 */
	protected IItemPropertySource itemPropertySource;

	/**
	 * An instance is constructed from an object and its item property source.
	 */
	public PropertySource(Object object, IAdapterFactory adapterFactory,
			IItemPropertySource itemPropertySource) {
		this.object = object;
		this.adapterFactory = adapterFactory;
		this.itemPropertySource = itemPropertySource;
	}

	/**
	 * This delegates to {@link IItemPropertySource#getEditableValue
	 * IItemPropertySource.getEditableValue}.
	 */
	public Object getEditableValue() {
		return itemPropertySource.getEditableValue(object);
	}

	/**
	 * This delegates to {@link IItemPropertySource#getPropertyDescriptors
	 * IItemPropertySource.getPropertyDescriptors}.
	 */
	public IPropertyDescriptor[] getPropertyDescriptors() {
		Collection<IPropertyDescriptor> result = new ArrayList<IPropertyDescriptor>();
		List<IItemPropertyDescriptor> itemPropertyDescriptors = itemPropertySource
				.getPropertyDescriptors(object);
		if (itemPropertyDescriptors != null) {
			for (IItemPropertyDescriptor itemPropertyDescriptor : itemPropertyDescriptors) {
				result.add(createPropertyDescriptor(itemPropertyDescriptor));
			}
		}

		return result.toArray(new IPropertyDescriptor[result.size()]);
	}

	protected IPropertyDescriptor createPropertyDescriptor(
			IItemPropertyDescriptor itemPropertyDescriptor) {
		return new PropertyDescriptor(object, adapterFactory,
				itemPropertyDescriptor);
	}

	/**
	 * This delegates to {@link IItemPropertyDescriptor#getPropertyValue
	 * IItemPropertyDescriptor.getPropertyValue}.
	 */
	public Object getPropertyValue(Object propertyId) {
		return itemPropertySource.getPropertyDescriptor(object, propertyId)
				.getPropertyValue(object);
	}

	/**
	 * This delegates to {@link IItemPropertyDescriptor#isPropertySet
	 * IItemPropertyDescriptor.isPropertySet}.
	 */
	public boolean isPropertySet(Object propertyId) {
		return itemPropertySource.getPropertyDescriptor(object, propertyId)
				.isPropertySet(object);
	}

	/**
	 * This delegates to {@link IItemPropertyDescriptor#resetPropertyValue
	 * IItemPropertyDescriptor.resetPropertyValue}.
	 */
	public void resetPropertyValue(Object propertyId) {
		itemPropertySource.getPropertyDescriptor(object, propertyId)
				.resetPropertyValue(object);
	}

	/**
	 * This delegates to {@link IItemPropertyDescriptor#setPropertyValue
	 * IItemPropertyDescriptor.setPropertyValue}.
	 */
	public void setPropertyValue(Object propertyId, Object value) {
		itemPropertySource.getPropertyDescriptor(object, propertyId)
				.setPropertyValue(object, value);
	}
}
