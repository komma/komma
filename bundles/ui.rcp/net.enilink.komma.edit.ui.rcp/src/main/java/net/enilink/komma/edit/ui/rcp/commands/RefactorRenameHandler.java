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
package net.enilink.komma.edit.ui.rcp.commands;

import net.enilink.komma.edit.ui.commands.AbstractHandler;
import net.enilink.komma.edit.ui.wizards.RefactorRenameWizard;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

/**
 * Command handler to refactor/rename resources within their models.
 */
public class RefactorRenameHandler extends AbstractHandler {
	@Override
	public void execute(ExecutionEvent event, IProgressMonitor monitor)
			throws ExecutionException {
		IWorkbench workbench = PlatformUI.getWorkbench();
		RefactorRenameWizard wizard = new RefactorRenameWizard(
				getEditingDomainChecked(event), workbench,
				(IStructuredSelection) workbench.getActiveWorkbenchWindow()
						.getSelectionService().getSelection());

		// create and open the wizard dialog
		WizardDialog dialog = new WizardDialog(workbench
				.getActiveWorkbenchWindow().getShell(), wizard);
		dialog.open();
	}
}
