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
 * $Id: ITreeItemContentProvider.java,v 1.3 2006/12/28 06:48:53 marcelop Exp $
 */
package net.enilink.komma.edit.provider;


import java.util.Collection;


/**
 * This is the interface needed to populate subtrees in a TreeViewer.
 */
public interface ITreeItemContentProvider extends IStructuredItemContentProvider
{
  /**
   * This does the same thing as ITreeContentProvider.getChildren.
   */
  Collection<?> getChildren(Object object);

  /**
   * This does the same thing as ITreeContentProvider.hasChildren.
   */
  boolean hasChildren(Object object);

  /**
   * This does the same thing as ITreeContentProvider.getParent.
   */
  Object getParent(Object object);
}
