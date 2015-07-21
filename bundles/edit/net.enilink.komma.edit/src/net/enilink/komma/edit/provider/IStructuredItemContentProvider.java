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
 * $Id: IStructuredItemContentProvider.java,v 1.3 2006/12/28 06:48:53 marcelop Exp $
 */
package net.enilink.komma.edit.provider;

import java.util.Collection;

/**
 * This is the interface needed to populate the top level items in a TreeViewer,
 * the items of a ListViewer, or the rows of a TableViewer.
 */
public interface IStructuredItemContentProvider {
	/**
	 * This does the same thing as IStructuredContentProvider.getElements.
	 */
	Collection<?> getElements(Object object);

}
