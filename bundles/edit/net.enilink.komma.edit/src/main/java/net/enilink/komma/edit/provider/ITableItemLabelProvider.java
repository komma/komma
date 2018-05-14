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
 * $Id: ITableItemLabelProvider.java,v 1.2 2005/06/08 06:17:05 nickb Exp $
 */
package net.enilink.komma.edit.provider;



/**
 * This is the interface needed to provide labels for items in a TableViewer.
 * This interface is similar to {@link IItemLabelProvider}, but this will pass additional information, 
 * namely the column index.
 */
public interface ITableItemLabelProvider
{
  /**
   * This does the same thing as ITableLabelProvider.getColumnText.
   */
  public String getColumnText(Object object, int columnIndex);

  /**
   * This does the same thing as ITableLabelProvider.getColumnImage.
   */
  public Object getColumnImage(Object object, int columnIndex);
}
