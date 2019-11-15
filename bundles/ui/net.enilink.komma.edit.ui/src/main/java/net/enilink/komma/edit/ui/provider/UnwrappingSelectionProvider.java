/**
 * <copyright> 
 *
 * Copyright (c) 2007, 2009 IBM Corporation and others.
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
 * $Id: UnwrappingSelectionProvider.java,v 1.2 2009/01/21 05:26:08 davidms Exp $
 */
package net.enilink.komma.edit.ui.provider;

import java.util.List;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;

import net.enilink.komma.common.util.UniqueExtensibleList;
import net.enilink.komma.edit.domain.AdapterFactoryEditingDomain;

public class UnwrappingSelectionProvider implements ISelectionProvider {
	protected ISelection selection;
	protected List<ISelectionChangedListener> listeners = new UniqueExtensibleList.FastCompare<ISelectionChangedListener>();
	protected ISelectionChangedListener selectionChangedListener = new ISelectionChangedListener() {
		public void selectionChanged(SelectionChangedEvent event) {
			setSelection(event.getSelection());
		}
	};

	public UnwrappingSelectionProvider(ISelectionProvider selectionProvider) {
		selectionProvider.addSelectionChangedListener(selectionChangedListener);
		setSelection(selectionProvider.getSelection());
	}

	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		listeners.add(listener);
	}

	public ISelection getSelection() {
		return selection;
	}

	public void removeSelectionChangedListener(
			ISelectionChangedListener listener) {
		listeners.remove(listener);
	}

	public void setSelection(ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			Object[] objects = ((IStructuredSelection) selection).toArray();
			for (int i = 0; i < objects.length; ++i) {
				objects[i] = unwrap(objects[i]);
			}
			this.selection = new StructuredSelection(objects);
		} else {
			this.selection = selection;
		}
		fireSelectionChanged();
	}

	protected Object unwrap(Object object) {
		return AdapterFactoryEditingDomain.unwrap(object);
	}

	protected void fireSelectionChanged() {
		for (ISelectionChangedListener selectionChangedListener : listeners) {
			selectionChangedListener
					.selectionChanged(new SelectionChangedEvent(this, selection));
		}
	}
}
