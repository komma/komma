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

import javax.swing.event.EventListenerList;

import org.eclipse.core.runtime.ListenerList;

public abstract class AbstractTreeModel<E> implements ITreeModel<E> {
	private ListenerList listenerList = new ListenerList();	

	public void addTreeModelListener(ITreeModelListener listener) {
		listenerList.add(listener);
	}

	public void removeTreeModelListener(ITreeModelListener listener) {
		listenerList.remove(listener);
	}
	
    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type.
     *
     * @param path the path to the root node
     * @param children the changed elements
     */
    protected void fireTreeNodesChanged(Object[] path, Object[] children) {
    	for (Object listener : listenerList.getListeners()) {
    		((ITreeModelListener)listener).treeNodesChanged(new TreeModelEvent(path, children));
    	}
    }

    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type.
     *
     * @param path the path to the root node
     * @param childIndices the indices of the new elements
     * @param children the new elements
     */
    protected void fireTreeNodesInserted(Object[] path, Object[] children, int index) {
    	for (Object listener : listenerList.getListeners()) {
    		((ITreeModelListener)listener).treeNodesInserted(new TreeModelEvent(path, children, index));
    	}
    }

    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type.
     *
     * @param path the path to the root node
     * @param children the removed elements
     */
    protected void fireTreeNodesRemoved(Object[] path, Object[] children) {
    	for (Object listener : listenerList.getListeners()) {
    		((ITreeModelListener)listener).treeNodesRemoved(new TreeModelEvent(path, children));
    	}
    }

    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type.
     *
     * @param path the path to the root node
     * @param children the affected elements
     * @see EventListenerList
     */
    protected void fireTreeStructureChanged(Object[] path, Object[] children) {
    	for (Object listener : listenerList.getListeners()) {
    		((ITreeModelListener)listener).treeStructureChanged(new TreeModelEvent(path, children));
    	}
    }
}
