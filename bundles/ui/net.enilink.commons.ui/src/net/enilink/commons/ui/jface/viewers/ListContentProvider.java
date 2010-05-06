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
package net.enilink.commons.ui.jface.viewers;

import org.eclipse.jface.viewers.AbstractListViewer;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import net.enilink.commons.models.IListModel;
import net.enilink.commons.models.IListModelListener;
import net.enilink.commons.models.ListModelEvent;

public class ListContentProvider implements IStructuredContentProvider {
	IListModelListener listListener = new IListModelListener() {
		public void contentsChanged(ListModelEvent e) {
			listContentsChanged(e);
		}

		public void elementsAdded(ListModelEvent e) {
			listElementsAdded(e);
		}

		public void elementsRemoved(ListModelEvent e) {
			listElementsRemoved(e);
		}
	};
	AbstractListViewer viewer;
	IListModel<?> model;
	
	public Object[] getElements(Object inputElement) {
		if (model != null) {
			return model.getElements().toArray();
		}
		return new Object[0];
	}

	public void dispose() {
		if (model != null) {
			model.removeListModelListener(listListener);
		}
		listListener = null;
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (viewer instanceof AbstractListViewer) {
			this.viewer = (AbstractListViewer)viewer;
		} else {
			this.viewer = null;
		}
		
		if (oldInput instanceof IListModel<?>) {
			((IListModel<?>)oldInput).removeListModelListener(listListener);
		}
		if (newInput instanceof IListModel<?>) {
			model = (IListModel<?>)newInput;
			if (this.viewer != null) {
				model.addListModelListener(listListener);
			}
		} else {
			model = null;
		}
	}
	
	protected void listContentsChanged(ListModelEvent e) {
		Object[] elements = e.elements;
		if (elements == null) {
			viewer.refresh();
		} else {
			for (Object element : elements) {
				viewer.refresh(element);
			}
		}
	}

	protected void listElementsAdded(ListModelEvent e) {
		Object[] elements = e.elements;
		if (elements == null || e.index != ListModelEvent.NO_INDEX) {
			viewer.refresh();
		} else {
			viewer.add(elements);
		}
	}

	protected void listElementsRemoved(ListModelEvent e) {
		Object[] elements = e.elements;
		if (elements == null) {
			viewer.refresh();
		} else {
			viewer.remove(elements);
		}
	}
}
