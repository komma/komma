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

public abstract class AbstractTableModel<E> extends AbstractListModel<E> implements ITableModel<E> {
	private ListenerList listenerList = new ListenerList();
	
	public void addTableModelListener(ITableModelListener listener) {
		addListModelListener(listener);
		listenerList.add(listener);
	}

	public void removeTableModelListener(ITableModelListener listener) {
		removeListModelListener(listener);
		listenerList.remove(listener);
	}
	
	protected void fireColumnsChanged() {
		for (Object listener : listenerList.getListeners()) {
			((ITableModelListener)listener).columnsChanged(
				new TableModelEvent(TableModelEvent.Type.CHANGED, 0)
			);
		}
	}
	
	protected void fireColumnsAdded(int index, int count) {
		for (Object listener : listenerList.getListeners()) {
			((ITableModelListener)listener).columnsAdded(
				new TableModelEvent(TableModelEvent.Type.ADDED, index, count)
			);
		}
	} 
	
	protected void fireColumnsAdded(int count) {
		fireColumnsAdded(TableModelEvent.NO_INDEX, count);
	}
	
	protected void fireColumnsRemoved(int index, int count) {
		for (Object listener : listenerList.getListeners()) {
			((ITableModelListener)listener).columnsRemoved(
				new TableModelEvent(TableModelEvent.Type.REMOVED, index, count)
			);
		}
	}
	
	public boolean isValueEditable(E element, int columnIndex) {
		return false;
	}

	public void setValue(E element, Object value, int columnIndex) {
	}
}
