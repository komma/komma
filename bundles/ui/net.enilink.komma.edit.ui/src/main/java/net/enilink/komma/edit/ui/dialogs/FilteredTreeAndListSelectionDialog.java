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
package net.enilink.komma.edit.ui.dialogs;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.dialogs.SelectionStatusDialog;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * Shows a list of items to the user with a text entry field for a string
 * pattern used to filter the list of items.
 */
public abstract class FilteredTreeAndListSelectionDialog extends
		SelectionStatusDialog implements IFilteredTreeAndListDescriptor {
	private static final String TREE_SETTINGS = "TreeSettings"; //$NON-NLS-1$

	private static final String LIST_SETTINGS = "ListSettings"; //$NON-NLS-1$

	private static final String DIALOG_BOUNDS_SETTINGS = "DialogBoundsSettings"; //$NON-NLS-1$

	private static final String DIALOG_HEIGHT = "DIALOG_HEIGHT"; //$NON-NLS-1$

	private static final String DIALOG_WIDTH = "DIALOG_WIDTH"; //$NON-NLS-1$

	private IStatus status;

	private FilteredTreeAndListSelectionWidget treeAndListSelectionPanel;

	/**
	 * Creates a new instance of the class.
	 * 
	 * @param shell
	 *            shell to parent the dialog on
	 * @param multi
	 *            indicates whether dialog allows to select more than one
	 *            position in its list of items
	 */
	public FilteredTreeAndListSelectionDialog(Shell shell, boolean multi) {
		super(shell);
		setShellStyle(getShellStyle() | SWT.RESIZE);

		treeAndListSelectionPanel = new FilteredTreeAndListSelectionWidget(
				this, this, multi);

		/*
		 * filteredTree = new FilteredTree(new PatternFilter()); filteredList =
		 * new FilteredList(multi) {
		 * 
		 * @Override protected ItemsFilter createFilter() { return new
		 * ItemsFilter() {
		 * 
		 * @Override public boolean isConsistentItem(Object item) { return true;
		 * }
		 * 
		 * @Override public boolean matchItem(Object item) { return
		 * matches(getListItemName(item)); } }; }
		 * 
		 * @Override protected void fillContentProvider( AbstractContentProvider
		 * contentProvider, ItemsFilter itemsFilter, IProgressMonitor
		 * progressMonitor) throws CoreException {
		 * fillListContentProvider(contentProvider, itemsFilter,
		 * progressMonitor); }
		 * 
		 * @Override public String getElementName(Object item) { return
		 * getListItemName(item); }
		 * 
		 * @Override protected Comparator<Object> getItemsComparator() { return
		 * getListItemsComparator(); }
		 * 
		 * @Override protected IStatus validateItem(Object item) { return
		 * validateListItem(item); }
		 * 
		 * @Override protected void updateStatus(IStatus status) {
		 * FilteredTreeAndListSelectionDialog.this.updateStatus(status); } };
		 */
	}

	/**
	 * Creates a new instance of the class. Created dialog won't allow to select
	 * more than one item.
	 * 
	 * @param shell
	 *            shell to parent the dialog on
	 */
	public FilteredTreeAndListSelectionDialog(Shell shell) {
		this(shell, false);
	}

	public FilteredList getFilteredList() {
		return treeAndListSelectionPanel.getFilteredList();
	}

	public FilteredTree getFilteredTree() {
		return treeAndListSelectionPanel.getFilteredTree();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.window.Window#create()
	 */
	public void create() {
		super.create();
	}

	/**
	 * Restores dialog using persisted settings. The default implementation
	 * restores the status of the details line and the selection history.
	 * 
	 * @param settings
	 *            settings used to restore dialog
	 */
	protected void restoreDialog(IDialogSettings settings) {
		String setting = settings.get(LIST_SETTINGS);
		if (setting != null) {
			try {
				IMemento memento = XMLMemento.createReadRoot(new StringReader(
						setting));
				treeAndListSelectionPanel.getFilteredList().restoreState(
						memento);
			} catch (CoreException e) {
				// Simply don't restore the settings
				StatusManager
						.getManager()
						.handle(
								new Status(
										IStatus.ERROR,
										PlatformUI.PLUGIN_ID,
										IStatus.ERROR,
										DialogMessages.FilteredItemsSelectionDialog_restoreError,
										e));
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.window.Window#close()
	 */
	public boolean close() {
		treeAndListSelectionPanel.getFilteredList().composite.dispose();
		treeAndListSelectionPanel.getFilteredTree().composite.dispose();

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
		try {
			XMLMemento memento = XMLMemento.createWriteRoot(LIST_SETTINGS);
			treeAndListSelectionPanel.getFilteredList().saveState(memento);

			StringWriter writer = new StringWriter();

			memento.save(writer);
			settings.put(LIST_SETTINGS, writer.getBuffer().toString());
		} catch (IOException e) {
			// Simply don't store the settings
			StatusManager
					.getManager()
					.handle(
							new Status(
									IStatus.ERROR,
									PlatformUI.PLUGIN_ID,
									IStatus.ERROR,
									DialogMessages.FilteredItemsSelectionDialog_storeError,
									e));
		}
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

		/*
		 * Composite content = new Composite(dialogArea, SWT.NONE); GridData gd
		 * = new GridData(SWT.FILL, SWT.FILL, true, true);
		 * content.setLayoutData(gd);
		 * 
		 * GridLayout layout = new GridLayout(2, true); layout.marginWidth = 0;
		 * layout.marginHeight = 0; content.setLayout(layout);
		 * 
		 * Control treeControl = filteredTree.createControl(content, SWT.BORDER
		 * | SWT.V_SCROLL | SWT.H_SCROLL); treeControl.setLayoutData(new
		 * GridData(SWT.FILL, SWT.FILL, true, true));
		 * filteredTree.getViewer().setInput(getTreeInput());
		 * 
		 * Control listControl = filteredList.createControl(content);
		 * listControl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
		 * true));
		 */

		Composite content = treeAndListSelectionPanel.createControl(dialogArea);

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

	/**
	 * Refreshes the dialog - has to be called in UI thread.
	 */
	public void refresh() {
		treeAndListSelectionPanel.getFilteredList().refresh();
	}

	protected void computeResult() {
		setResult(treeAndListSelectionPanel.getFilteredList()
				.getSelectedItems().toList());
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
