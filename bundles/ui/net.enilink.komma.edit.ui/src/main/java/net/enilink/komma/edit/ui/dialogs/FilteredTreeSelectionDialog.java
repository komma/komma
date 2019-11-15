/*******************************************************************************
 * Copyright (c) 2009 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.edit.ui.dialogs;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.SelectionStatusDialog;

/**
 * Shows a list of items to the user with a text entry field for a string
 * pattern used to filter the list of items.
 */
public abstract class FilteredTreeSelectionDialog extends SelectionStatusDialog {
	private static final String TREE_SETTINGS = "TreeSettings"; //$NON-NLS-1$

	private static final String DIALOG_BOUNDS_SETTINGS = "DialogBoundsSettings"; //$NON-NLS-1$

	private static final String DIALOG_HEIGHT = "DIALOG_HEIGHT"; //$NON-NLS-1$

	private static final String DIALOG_WIDTH = "DIALOG_WIDTH"; //$NON-NLS-1$

	private IStatus status;

	private FilteredTree filteredTree;

	/**
	 * Creates a new instance of the class.
	 * 
	 * @param shell
	 *            shell to parent the dialog on
	 * @param multi
	 *            indicates whether dialog allows to select more than one
	 *            position in its list of items
	 */
	public FilteredTreeSelectionDialog(Shell shell, boolean multi) {
		super(shell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		filteredTree = new FilteredTree(new PatternFilter());
	}

	abstract protected Object getTreeInput();

	/**
	 * Creates a new instance of the class. Created dialog won't allow to select
	 * more than one item.
	 * 
	 * @param shell
	 *            shell to parent the dialog on
	 */
	public FilteredTreeSelectionDialog(Shell shell) {
		this(shell, false);
	}

	public FilteredTree getFilteredTree() {
		return filteredTree;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.window.Window#create()
	 */
	public void create() {
		super.create();
		getFilteredTree().getViewer().addSelectionChangedListener(
				new ISelectionChangedListener() {
					@Override
					public void selectionChanged(SelectionChangedEvent event) {
						if (event.getSelection().isEmpty()) {
							updateStatus(Status.CANCEL_STATUS);
						} else {
							updateStatus(Status.OK_STATUS);
						}
					}
				});
	}

	/**
	 * Restores dialog using persisted settings. The default implementation
	 * restores the status of the details line and the selection history.
	 * 
	 * @param settings
	 *            settings used to restore dialog
	 */
	protected void restoreDialog(IDialogSettings settings) {
		// String setting = settings.get(TREE_SETTINGS);
		// if (setting != null) {
		// try {
		// IMemento memento = XmlMemento.createReadRoot(new StringReader(
		// setting));
		// filteredTree.restoreState(memento);
		// } catch (CoreException e) {
		// // Simply don't restore the settings
		// StatusManager
		// .getManager()
		// .handle(
		// new Status(
		// IStatus.ERROR,
		// PlatformUI.PLUGIN_ID,
		// IStatus.ERROR,
		// DialogMessages.FilteredItemsSelectionDialog_restoreError,
		// e));
		// }
		// }
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.window.Window#close()
	 */
	public boolean close() {
		storeDialog(getDialogSettings());
		return super.close();
	}

	/**
	 * Stores dialog settings.
	 * 
	 * @param settings
	 *            settings used to store dialog
	 */
	protected void storeDialog(IDialogSettings settings) {
		// try {
		// XmlMemento memento = XmlMemento.createWriteRoot(TREE_SETTINGS);
		// filteredList.saveState(memento);
		//
		// StringWriter writer = new StringWriter();
		//
		// memento.save(writer);
		// settings.put(TREE_SETTINGS, writer.getBuffer().toString());
		// } catch (IOException e) {
		// // Simply don't store the settings
		// StatusManager
		// .getManager()
		// .handle(
		// new Status(
		// IStatus.ERROR,
		// PlatformUI.PLUGIN_ID,
		// IStatus.ERROR,
		// DialogMessages.FilteredItemsSelectionDialog_storeError,
		// e));
		// }
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets
	 * .Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		Composite dialogArea = (Composite) super.createDialogArea(parent);

		Composite content = new Composite(dialogArea, SWT.NONE);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		content.setLayoutData(gd);

		GridLayout layout = new GridLayout(1, true);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		content.setLayout(layout);

		Control treeControl = filteredTree.createControl(content, SWT.BORDER
				| SWT.V_SCROLL | SWT.H_SCROLL);
		treeControl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		filteredTree.getViewer().setInput(getTreeInput());

		applyDialogFont(content);

		restoreDialog(getDialogSettings());

		return dialogArea;
	}

	/**
	 * This method is a hook for subclasses to override default dialog behavior.
	 * The <code>handleDoubleClick()</code> method handles double clicks on the
	 * list of filtered elements.
	 * <p>
	 * Current implementation makes double-clicking on the list do the same as
	 * pressing <code>OK</code> button on the dialog.
	 */
	protected void handleDoubleClick() {
		okPressed();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.window.Dialog#getDialogBoundsSettings()
	 */
	protected IDialogSettings getDialogBoundsSettings() {
		IDialogSettings settings = getDialogSettings();
		IDialogSettings section = settings.getSection(DIALOG_BOUNDS_SETTINGS);
		if (section == null) {
			section = settings.addNewSection(DIALOG_BOUNDS_SETTINGS);
			section.put(DIALOG_HEIGHT, 500);
			section.put(DIALOG_WIDTH, 600);
		}
		return section;
	}

	/**
	 * Returns the dialog settings. Returned object can't be null.
	 * 
	 * @return return dialog settings for this dialog
	 */
	protected abstract IDialogSettings getDialogSettings();

	protected void computeResult() {
		setResult(((IStructuredSelection) filteredTree.getViewer()
				.getSelection()).toList());
	}

	/*
	 * @see
	 * org.eclipse.ui.dialogs.SelectionStatusDialog#updateStatus(org.eclipse
	 * .core.runtime.IStatus)
	 */
	protected void updateStatus(IStatus status) {
		this.status = status;
		super.updateStatus(status);
	}

	/*
	 * @see Dialog#okPressed()
	 */
	protected void okPressed() {
		if (status != null
				&& (status.isOK() || status.getCode() == IStatus.INFO)) {
			super.okPressed();
		}
	}
}
