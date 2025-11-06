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
 * $Id: IItemLabelProvider.java,v 1.2 2005/06/08 06:17:05 nickb Exp $
 */
package net.enilink.komma.edit.provider;

/**
 * This is the interface implemented to provide a label text and even a label
 * icon for an item; it receives delegated calls from ILabelProvider.
 */
public interface IItemLabelProvider {
	/**
	 * This does the same thing as ILabelProvider.getText, it fetches the label
	 * text specific to this object instance.
	 */
	String getText(Object object);

	/**
	 * This does the same thing as ILabelProvider.getImage, it fetches the label
	 * image specific to this object instance.
	 */
	Object getImage(Object object);
}
