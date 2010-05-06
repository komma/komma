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

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Item;

import net.enilink.commons.models.IColumnBasedModel;

public class TableCellModifier<T> implements ICellModifier {
	private Viewer viewer;

	public TableCellModifier(Viewer viewer) {
		Assert.isNotNull(viewer);
		this.viewer = viewer;
	}

	@SuppressWarnings({ "unchecked" })
	public boolean canModify(Object element, String property) {
		if (property == null) {
			return false;
		}
		int index;
		try {
			index = Integer.valueOf(property);
		} catch (NumberFormatException nfe) {
			return false;
		}
		Object viewerInput = viewer.getInput();
		if (viewerInput instanceof IColumnBasedModel<?>) {
			return ((IColumnBasedModel<T>) viewerInput).isValueEditable(
					(T) element, index);
		}

		return false;
	}

	@SuppressWarnings({ "unchecked" })
	public Object getValue(Object element, String property) {
		if (property == null) {
			return null;
		}
		int index;
		try {
			index = Integer.valueOf(property);
		} catch (NumberFormatException nfe) {
			return null;
		}
		Object viewerInput = viewer.getInput();
		if (viewerInput instanceof IColumnBasedModel<?>) {
			return ((IColumnBasedModel<T>) viewerInput).getValue((T) element,
					index);
		}

		return null;
	}

	@SuppressWarnings({ "unchecked" })
	public void modify(Object element, String property, Object value) {
		if (element instanceof Item) {
			element = ((Item) element).getData();
		}

		int index;
		try {
			index = Integer.valueOf(property);
		} catch (NumberFormatException nfe) {
			return;
		}
		Object viewerInput = viewer.getInput();
		if (viewerInput instanceof IColumnBasedModel<?>) {
			((IColumnBasedModel<T>) viewerInput).setValue((T) element, value,
					index);
		}
	}

}
