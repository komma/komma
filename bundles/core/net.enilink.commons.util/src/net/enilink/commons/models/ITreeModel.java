/*******************************************************************************
 * Copyright (c) 2009 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.commons.models;

import java.util.Collection;

public interface ITreeModel<E> {
	public void addTreeModelListener(ITreeModelListener l);

	public Collection<? extends E> getChildren(E parent);

	public Collection<? extends E> getElements();
	
	public E getParent(E node);

	public boolean hasChildren(E node);

	public void removeTreeModelListener(ITreeModelListener l);
}