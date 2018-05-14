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
 * $Id: ChildrenToCopyProvider.java,v 1.3 2006/12/28 06:48:55 marcelop Exp $
 */
package net.enilink.komma.edit.command;

import java.util.Collection;

/**
 * This interface is implemented by commands that need to support specialized
 * copying of children.
 */
public interface IChildrenToCopyProvider {
	Collection<?> getChildrenToCopy();
}
