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

import org.eclipse.core.runtime.ListenerList;

public abstract class AbstractListModel<E> implements IListModel<E> {
	private ListenerList listenerList = new ListenerList();
	
	public void addListModelListener(IListModelListener listener) {
		listenerList.add(listener);
	}

	public void removeListModelListener(IListModelListener listener) {
		listenerList.remove(listener);
	}
	
	protected void fireContentsChanged() {
		fireContentsChanged(null);
	}
	
	protected void fireContentsChanged(Object[] elements) {
		for (Object listener : listenerList.getListeners()) {
			((IListModelListener)listener).contentsChanged(
				new ListModelEvent(ListModelEvent.Type.CHANGED, ListModelEvent.NO_INDEX, elements)
			);
		}
	}
	
	public void fireElementChanged(Object element) {
		fireContentsChanged(new Object[] { element });
	}
	
	protected void fireElementsAdded(int index, Object[] elements) {
		for (Object listener : listenerList.getListeners()) {
			((IListModelListener)listener).elementsAdded(
				new ListModelEvent(ListModelEvent.Type.CHANGED, index, elements)
			);
		}
	} 
	
	protected void fireElementsAdded(Object[] elements) {
		fireElementsAdded(ListModelEvent.NO_INDEX, elements);
	}
	
	protected void fireElementsRemoved(int index, Object[] elements) {
		for (Object listener : listenerList.getListeners()) {
			((IListModelListener)listener).elementsRemoved(
				new ListModelEvent(ListModelEvent.Type.CHANGED, index, elements)
			);
		}
	}
	
	protected void fireElementsRemoved(Object[] elements) {
		fireElementsRemoved(ListModelEvent.NO_INDEX, elements);
	}
}
