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
 * $Id: ExtendedDialogCellEditor.java,v 1.3 2006/12/28 06:42:02 marcelop Exp $
 */
package net.enilink.komma.common.ui.celleditor;

import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.widgets.Composite;

/**
 * This uses a label provider to display a dialog cell editor.
 */
public abstract class ExtendedDialogCellEditor extends DialogCellEditor {
	protected ILabelProvider labelProvider;

	public ExtendedDialogCellEditor(Composite composite,
			ILabelProvider labelProvider) {
		super(composite);
		this.labelProvider = labelProvider;
	}

	@Override
	protected void updateContents(Object object) {
		if (getDefaultLabel() != null && labelProvider != null) {
			getDefaultLabel().setText(labelProvider.getText(object));
		}
	}
}
