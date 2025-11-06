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
 * $Id: IUpdateableItemParent.java,v 1.2 2005/06/08 06:17:05 nickb Exp $
 */
package net.enilink.komma.edit.provider;



/**
 * This is the interface implemented by an item provider if it supports an updateable parent relation.
 */
public interface IUpdateableItemParent
{
  /**
   * This sets the given object's parent to be parent.
   */
  void setParent(Object object, Object parent);
}
