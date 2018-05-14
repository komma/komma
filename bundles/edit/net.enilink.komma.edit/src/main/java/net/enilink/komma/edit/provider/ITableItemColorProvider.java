/**
 * <copyright> 
 *
 * Copyright (c) 2008, 2009 IBM Corporation and others.
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
 * $Id: ITableItemColorProvider.java,v 1.1 2008/01/15 17:15:41 emerks Exp $
 */
package net.enilink.komma.edit.provider;



/**
 * This is the interface needed to provide color for items in a TableViewer.
 * This interface is similar to {@link IItemColorProvider}, but this will pass additional information, 
 * namely the column index.
 */
public interface ITableItemColorProvider
{
  /**
   * This does the same thing as ITableColorProvider.getForeground.
   */
  public Object getForeground(Object object, int columnIndex);

  /**
   * This does the same thing as ITableColorProvider.getBackground.
   */
  public Object getBackground(Object object, int columnIndex);
}
