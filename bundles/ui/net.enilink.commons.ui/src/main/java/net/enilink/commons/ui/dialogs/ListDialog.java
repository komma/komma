/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package net.enilink.commons.ui.dialogs;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.dialogs.SelectionDialog;

public class ListDialog extends SelectionDialog {
	private IStructuredContentProvider contentProvider;

	private ILabelProvider labelProvider;

	private Object input;

	private Object[] elements;

	private TableViewer tableViewer;

	private boolean addCancelButton = true;

	private int selectionStyle = SWT.SINGLE;

	public ListDialog(Shell parent, int shellStyle) {
		super(parent);
		setShellStyle(shellStyle);
	}

	public ListDialog(Shell parent) {
		super(parent);
	}

	public void setInput(Object input) {
		this.input = input;
	}

	public void setContentProvider(IStructuredContentProvider contentProvider) {
		this.contentProvider = contentProvider;
	}

	public void setElements(Object... elements) {
		this.elements = elements;
	}

	public void setLabelProvider(ILabelProvider labelprovider) {
		this.labelProvider = labelprovider;
	}

	public void setAddCancelButton(boolean addCancelButton) {
		this.addCancelButton = addCancelButton;
	}

	public TableViewer getTableViewer() {
		return tableViewer;
	}

	public boolean hasFilters() {
		return tableViewer.getFilters() != null
				&& tableViewer.getFilters().length != 0;
	}

	protected Label createMessageArea(Composite composite) {
		Label label = new Label(composite, SWT.WRAP);
		label.setText(getMessage());
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = convertWidthInCharsToPixels(55);
		label.setLayoutData(gd);
		applyDialogFont(label);
		return label;
	}

	protected Control createDialogArea(Composite container) {
		Composite parent = (Composite) super.createDialogArea(container);
		createMessageArea(parent);
		tableViewer = new TableViewer(parent, selectionStyle | getTableStyle());
		tableViewer.setLabelProvider(labelProvider);
		if (elements == null) {
			tableViewer.setContentProvider(contentProvider);
			tableViewer.setInput(input);
		} else {
			tableViewer.add(elements);
		}

		if (getInitialElementSelections() != null) {
			tableViewer.setSelection(new StructuredSelection(
					getInitialElementSelections()));
		}

		Table table = tableViewer.getTable();

		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.widthHint = convertWidthInCharsToPixels(55);
		gridData.heightHint = convertHeightInCharsToPixels(15);
		table.setLayoutData(gridData);
		applyDialogFont(parent);
		return parent;
	}

	protected void createButtonsForButtonBar(Composite parent) {
		if (!addCancelButton) {
			createButton(parent, IDialogConstants.OK_ID, DialogHelper.get()
					.getButtonLabel(IDialogConstants.OK_ID), true);
		} else {
			super.createButtonsForButtonBar(parent);
		}
	}

	public void setSingleSelectionMode(boolean enabled) {
		if (enabled) {
			selectionStyle &= ~SWT.MULTI;
			selectionStyle |= SWT.SINGLE;
		} else {
			selectionStyle &= ~SWT.SINGLE;
			selectionStyle |= SWT.MULTI;
		}
	}

	protected int getTableStyle() {
		return SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER;
	}

	/**
	 * The <code>ListSelectionDialog</code> implementation of this
	 * <code>Dialog</code> method builds a list of the selected elements for
	 * later retrieval by the client and closes this dialog.
	 */
	protected void okPressed() {
		setSelectionResult(((IStructuredSelection) getTableViewer()
				.getSelection()).toArray());

		super.okPressed();
	}
}
