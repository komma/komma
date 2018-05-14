/*******************************************************************************
 * Copyright (c) 2014 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.edit.ui.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import net.enilink.komma.core.IReference;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIs;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.edit.refactor.Change;
import net.enilink.komma.edit.refactor.RefactoringProcessor;
import net.enilink.komma.model.IObject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;

public class RefactorRenameWizard extends Wizard {

	protected WizardPage configureRenamesPage;
	protected WizardPage showPreviewPage;

	protected Map<IObject, IReference> renameMap;

	protected Collection<Change> changes;

	protected Composite containerComposite;

	protected IWorkbench workbench;
	protected IEditingDomain domain;

	protected IStructuredSelection currentSelection;

	@Override
	public void createPageControls(Composite pageContainer) {
		super.createPageControls(pageContainer);

		containerComposite = pageContainer;
	}

	public RefactorRenameWizard(IEditingDomain domain, IWorkbench workbench,
			IStructuredSelection currentSelection) {
		this.domain = domain;
		this.workbench = workbench;
		this.currentSelection = currentSelection;

		this.renameMap = new LinkedHashMap<IObject, IReference>();
		for (Object object : currentSelection.toArray()) {
			if (object instanceof IObject) {
				renameMap.put((IObject) object, null);
			}
		}

		setWindowTitle("Refactor - Rename content within models");
		setNeedsProgressMonitor(true);

		createPages();
	}

	static class TableLabelProvider extends ColumnLabelProvider {
		public static enum ColumnType {
			BEFORE, AFTER
		};

		ColumnType column;

		public TableLabelProvider(ColumnType column) {
			this.column = column;
		}

		@Override
		public Image getImage(Object element) {
			return null;
		}

		@SuppressWarnings("rawtypes")
		@Override
		public String getText(Object element) {
			if (element instanceof Map.Entry) {
				Map.Entry entry = (Map.Entry) element;
				switch (column) {
				case BEFORE:
					return entry.getKey().toString();
				case AFTER:
					IReference value = (IReference) entry.getValue();
					return value != null ? value.toString() : null;
				default:
					return null;
				}
			}
			return null;
		}
	};

	protected void createPages() {
		configureRenamesPage = new WizardPage("Configure the new URIs to use.") {
			@Override
			public void createControl(Composite parent) {

				Composite composite = new Composite(parent, SWT.NONE);
				composite.setLayout(new GridLayout());

				// nested composite for "one namespace to rule them all"
				Composite nsComposite = new Composite(composite, SWT.NONE);
				GridLayout ncLayout = new GridLayout(2, false);
				ncLayout.marginHeight = 0;
				ncLayout.marginWidth = 0;
				nsComposite.setLayout(ncLayout);

				// toggle button for generic or individual renaming
				final Button useSameButton = new Button(nsComposite, SWT.CHECK);
				useSameButton.setSelection(false);
				useSameButton.setText("Move all elements into this namespace:");

				// FIXME: add validation (URI) for text input field
				final Text namespace = new Text(nsComposite, SWT.BORDER);
				namespace.setLayoutData(new GridData(300, SWT.DEFAULT));
				namespace.setEnabled(useSameButton.getSelection());

				// the table viewer for the rename-mappings
				final TableViewer tableViewer = new TableViewer(composite,
						SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL
								| SWT.FULL_SELECTION);
				tableViewer.getTable()
						.setEnabled(!useSameButton.getSelection());
				tableViewer.getTable().setHeaderVisible(true);
				tableViewer.getTable().setLinesVisible(true);
				tableViewer.setContentProvider(ArrayContentProvider
						.getInstance());

				TableViewerColumn column = new TableViewerColumn(tableViewer,
						SWT.LEFT);
				column.getColumn().setText("Current URI");
				column.getColumn().setWidth(300);
				column.setLabelProvider(new TableLabelProvider(
						TableLabelProvider.ColumnType.BEFORE));

				column = new TableViewerColumn(tableViewer, SWT.LEFT);
				column.getColumn().setText("New URI");
				column.getColumn().setWidth(300);
				column.setLabelProvider(new TableLabelProvider(
						TableLabelProvider.ColumnType.AFTER));

				// FIXME: add validation (URI) for text input field
				final CellEditor cellEditor = new TextCellEditor(
						(Composite) tableViewer.getControl());
				column.setEditingSupport(new EditingSupport(tableViewer) {

					@Override
					protected CellEditor getCellEditor(Object element) {
						return cellEditor;
					}

					@Override
					protected boolean canEdit(Object element) {
						return (element instanceof Map.Entry);
					}

					@SuppressWarnings("rawtypes")
					@Override
					protected Object getValue(Object element) {
						if (element instanceof Map.Entry) {
							IReference value = (IReference) ((Map.Entry) element)
									.getValue();
							return value != null ? value.toString() : "";
						}
						return "";
					}

					@SuppressWarnings({ "unchecked", "rawtypes" })
					@Override
					protected void setValue(Object element, Object value) {
						if (element instanceof Map.Entry) {
							if (!(value instanceof IReference)) {
								if (value.toString().isEmpty()) {
									value = null;
								} else {
									value = URIs.createURI(value.toString());
								}
							}
							((Map.Entry) element).setValue(value);
							tableViewer.refresh(element);
						}
						setPageComplete(!renameMap.values().contains(null));
						getContainer().updateButtons();
					}
				});

				// button selection toggles text input and table viewer states
				useSameButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						namespace.setEnabled(useSameButton.getSelection());
						tableViewer.getTable().setEnabled(
								!useSameButton.getSelection());
					}
				});
				// leaving the text input sets the namespace on all elements
				namespace.addFocusListener(new FocusAdapter() {
					@Override
					public void focusLost(FocusEvent e) {
						URI namespaceURI = URIs.createURI(namespace.getText());
						for (Map.Entry<IObject, IReference> entry : renameMap
								.entrySet()) {
							entry.setValue(namespaceURI.appendFragment(entry
									.getKey().getURI().fragment()));
						}
						tableViewer.refresh();
						setPageComplete(true);
						getContainer().updateButtons();
					}
				});

				tableViewer.setInput(renameMap.entrySet());

				setDescription("Set the new URIs for your selected elements.");
				setControl(composite);
				setPageComplete(false);
			}
		};

		showPreviewPage = new RefactorPreviewPage("Preview") {
			@Override
			public Collection<Change> collectChanges() {
				changes = new RefactoringProcessor(domain)
						.createRenameChanges(renameMap);
				return changes;
			}
		};
	}

	@Override
	public void addPages() {
		addPage(configureRenamesPage);
		addPage(showPreviewPage);
	}

	@Override
	public IWizardPage getPreviousPage(IWizardPage currentPage) {
		if (currentPage == showPreviewPage) {
			return configureRenamesPage;
		}
		return null;
	}

	@Override
	public IWizardPage getNextPage(IWizardPage currentPage) {
		if (currentPage == configureRenamesPage) {
			return showPreviewPage;
		}
		return null;
	}

	public boolean canFinish() {
		return showPreviewPage.isPageComplete()
				&& getContainer().getCurrentPage() == showPreviewPage;
	}

	@Override
	public boolean performFinish() {
		// call RefactoringProcessor again to apply the confirmed changes
		try {
			getContainer().run(true, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					IStatus result = new RefactoringProcessor(domain)
							.applyChanges(changes, monitor, null);
					if (!result.isOK()) {
						if (result.getException() != null) {
							result.getException().printStackTrace();
						}
					}
				}
			});

			return true;
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return false;
	}
}
