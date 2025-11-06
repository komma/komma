/**
 * <copyright> 
 *
 * Copyright (c) 2008, 2009 IBM Corporation and others.
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
 * $Id: ITableItemFontProvider.java,v 1.1 2008/01/15 17:15:40 emerks Exp $
 */
package net.enilink.komma.edit.provider;



/**
 * This is the interface needed to provide color for items in a TableViewer.
 * This interface is similar to {@link IItemFontProvider}, but this will pass additional information, 
 * namely the column index.
 */
public interface ITableItemFontProvider
{
  /**
   * This does the same thing as ITableFontProvider.getFont.
   */
  Object getFont(Object object, int columnIndex);
}
