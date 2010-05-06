/**
 * <copyright> 
 *
 * Copyright (c) 2002, 2010 IBM Corporation and others.
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
 * $Id: TreeIterator.java,v 1.4 2007/06/12 20:56:17 emerks Exp $
 */
package net.enilink.komma.common.util;

import java.util.Iterator;

/**
 * A mechanism for iterating over all the nodes of a tree; it provides the
 * capability to {@link #prune prune} the iteration so that all descendants of a
 * particular node are skipped.
 */
public interface ITreeIterator<E> extends Iterator<E> {
	/**
	 * Prunes the iterator so that it skips over all the nodes below the most
	 * recent result of calling {@link #next() next()}.
	 */
	void prune();
}
