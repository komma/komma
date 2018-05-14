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
 * $Id: IItemPropertyDescriptor.java,v 1.7 2007/06/14 18:32:42 emerks Exp $
 */
package net.enilink.komma.edit.provider;

import java.util.Collection;

/**
 * This interface extends IPropertyDescriptor so that the methods of
 * {@link IItemPropertySource} can be delegated to the descriptor. This allows
 * the implementing class to completely encapsulate the work associated with
 * supporting a particular property sheet property.
 */
public interface IItemPropertyDescriptor {
	/**
	 * This fetches this descriptor's property from the object. Sometimes it's
	 * necessary to update the contents of the cell editor during this call,
	 * i.e., the call is used as a notification that this descriptor is being
	 * used to edit another object.
	 */
	public Object getPropertyValue(Object object);

	/**
	 * This determines whether this descriptor's property for the object is set.
	 * I'm not sure right now what this is used for? I should find out.
	 */
	public boolean isPropertySet(Object object);

	/**
	 * This determines whether this descriptor's property for the object
	 * supports set (and reset).
	 */
	public boolean canSetProperty(Object object);

	/**
	 * This resets this descriptor's property for the object.
	 */
	public void resetPropertyValue(Object object);

	/**
	 * This sets this descriptor's property for the object to the given value.
	 */
	public void setPropertyValue(Object object, Object value);

	/**
	 * Returns the name of the category to which this property belongs.
	 */
	String getCategory(Object object);

	/**
	 * Returns a brief description of this property.
	 */
	String getDescription(Object object);

	/**
	 * Returns the display name for this property.
	 */
	String getDisplayName(Object object);

	/**
	 * Returns a list of filter types to which this property belongs.
	 */
	String[] getFilterFlags(Object object);

	/*
	 * Returns the help context id for this property.
	 */
	Object getHelpContextIds(Object object);

	/**
	 * Returns the id for this property.
	 */
	String getId(Object object);

	/**
	 * Returns the label provider for this property.
	 */
	IItemLabelProvider getLabelProvider(Object object);

	/**
	 * Returns the feature.
	 */
	public Object getProperty(Object object);

	/**
	 * Returns whether this property represents multiple values. This may not be
	 * the same as the feature's getMany(), as the property may allows editing
	 * only a single value of a multi-valued feature.
	 */
	public boolean isMany(Object object);

	/**
	 * Returns the choices of all the values that this property may take one.
	 */
	public Collection<?> getChoiceOfValues(Object object);

	/**
	 * Returns whether this property's value will consist of multi-line text.
	 */
	boolean isMultiLine(Object object);

	/**
	 * Returns whether the choices for this property should be sorted for
	 * display.
	 */
	boolean isSortChoices(Object object);

	/**
	 * This interface may be implemented by item property descriptors to allow
	 * an object to be provided as an override for whatever would usually be the
	 * owner of any commands created to set the property's value. This is
	 * typically used when a wrapper is being displayed in place of a real model
	 * object, so that commands will be created by the wrapper.
	 */
	public interface OverrideableCommandOwner {
		/**
		 * Sets the object to use as the owner of commands created to set the
		 * property's value.
		 */
		public void setCommandOwner(Object override);

		/**
		 * Returns the override command owner set via {@link #setCommandOwner
		 * setCommandOwner}.
		 */
		public Object getCommandOwner();
	}
}
