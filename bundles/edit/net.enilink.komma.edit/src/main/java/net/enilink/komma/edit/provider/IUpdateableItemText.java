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
 * $Id: IUpdateableItemText.java,v 1.2 2005/06/08 06:17:05 nickb Exp $
 */
package net.enilink.komma.edit.provider;



/**
 * This is the interface implemented by an item provider if it supports an updateable item text.
 * This is be used to support edit-in-place tree items.
 */
public interface IUpdateableItemText
{
  /**
   * This returns the text that will be displayed when editing begins.
   */
  String getUpdateableText(Object object);

  /**
   * This sets the given object's label text to the given text. 
   */
  void setText(Object object, String text);
}
