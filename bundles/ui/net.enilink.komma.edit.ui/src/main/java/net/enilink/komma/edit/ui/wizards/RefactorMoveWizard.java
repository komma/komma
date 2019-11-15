/*******************************************************************************
 * Copyright (c) 2014 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.edit.ui.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.edit.refactor.Change;
import net.enilink.komma.edit.refactor.RefactoringProcessor;
import net.enilink.komma.model.IModel;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbench;

public class RefactorMoveWizard extends Wizard {

	protected WizardPage selectModelPage;
	protected WizardPage showPreviewPage;

	protected IModel targetModel;
	protected boolean keepNamespace;

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

	public RefactorMoveWizard(IEditingDomain domain, IWorkbench workbench,
			IStructuredSelection currentSelection) {
		this.domain = domain;
		this.workbench = workbench;
		this.currentSelection = currentSelection;

		this.keepNamespace = false;

		setWindowTitle("Refactor - Move content between models");
		setNeedsProgressMonitor(true);

		createPages();
	}

	protected void createPages() {
		selectModelPage = new WizardPage("Select Target Model") {
			@Override
			public void createControl(Composite parent) {

				Composite composite = new Composite(parent, SWT.NONE);
				composite.setLayout(new GridLayout());

				Label label = new Label(composite, SWT.NONE);
				label.setText("Select target model:");

				final Combo combo = new Combo(composite, SWT.DROP_DOWN);
				final ComboViewer cViewer = new ComboViewer(combo);
				cViewer.addSelectionChangedListener(new ISelectionChangedListener() {
					@Override
					public void selectionChanged(SelectionChangedEvent event) {
						targetModel = (IModel) cViewer.getElementAt(combo
								.getSelectionIndex());
						setPageComplete(true);
					}
				});

				// get potential target models from list of active editors
				IEditorReference[] openEditors = workbench
						.getActiveWorkbenchWindow().getActivePage()
						.getEditorReferences();
				for (IEditorReference ref : openEditors) {
					IEditorPart editor = ref.getEditor(true);
					if (editor != null
							&& editor != workbench.getActiveWorkbenchWindow()
									.getActivePage().getActiveEditor()) {
						IModel model = (IModel) editor.getAdapter(IModel.class);
						if (model != null) {
							cViewer.add(model);
						}
					}
				}

				final Button keepNamespaceButton = new Button(composite,
						SWT.CHECK);
				keepNamespaceButton.setSelection(keepNamespace);
				keepNamespaceButton.setText("Keep namespaces");
				keepNamespaceButton
						.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent e) {
								keepNamespace = keepNamespaceButton
										.getSelection();
							}
						});

				setDescription("Select the target model from the list of open editors.");
				setControl(composite);
				setPageComplete(false);
			}
		};

		showPreviewPage = new RefactorPreviewPage("Preview") {
			@Override
			public Collection<Change> collectChanges() {
				changes = new RefactoringProcessor(domain).createMoveChanges(
						currentSelection.toList(), targetModel, keepNamespace);
				return changes;
			}
		};
	}

	@Override
	public void addPages() {
		addPage(selectModelPage);
		addPage(showPreviewPage);
	}

	@Override
	public IWizardPage getPreviousPage(IWizardPage currentPage) {
		if (currentPage == showPreviewPage) {
			return selectModelPage;
		}
		return null;
	}

	@Override
	public IWizardPage getNextPage(IWizardPage currentPage) {
		if (currentPage == selectModelPage) {
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
