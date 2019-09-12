/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Remy Chi Jian Suen <remy.suen@gmail.com>
 *     		- Bug 44162 [Wizards]  Define constants for wizard ids of new.file, new.folder, and new.project
 *     Fraunhofer IWU - extended basic wizard for ontology creation
 *******************************************************************************/
package net.enilink.komma.owl.editor.rcp.wizards;

import java.net.URL;

import net.enilink.komma.owl.editor.OWLEditorPlugin;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.wizards.newresource.BasicNewFileResourceWizard;

/**
 * Simple wizard to create a new Ontology using standard workbench wizard
 * components for resource creation.
 * 
 * @see BasicNewFileResourceWizard
 */
public class NewOntologyWizard extends BasicNewFileResourceWizard {

	public static final String WIZARD_ID = "net.enilink.komma.owl.editor.wizards.new.ontology"; //$NON-NLS-1$

	private WizardConfigureOntologyPage mainPage;

	/**
	 * Creates a wizard for creating a new file resource in the workspace.
	 */
	public NewOntologyWizard() {
		super();
	}

	/*
	 * (non-Javadoc) Method declared on IWizard.
	 */
	public void addPages() {
		mainPage = new WizardConfigureOntologyPage("newOntologyPage", //$NON-NLS-1$
				getSelection());
		mainPage.setTitle("New Ontology");
		mainPage.setDescription("Create a new Ontology");
		addPage(mainPage);
	}

	/*
	 * (non-Javadoc) Method declared on IWorkbenchWizard.
	 */
	public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
		super.init(workbench, currentSelection);
		setWindowTitle("New Ontology");
		setNeedsProgressMonitor(true);
	}

	/*
	 * (non-Javadoc) Method declared on BasicNewResourceWizard.
	 */
	protected void initializeDefaultPageImageDescriptor() {
		super.initializeDefaultPageImageDescriptor();
		try {
			setDefaultPageImageDescriptor(ImageDescriptor
					.createFromURL(new URL(OWLEditorPlugin.INSTANCE.getImage(
							"full/wizban/newont_wiz").toString()))); //$NON-NLS-1$
		} catch (Throwable ignored) {
		}
	}

	/*
	 * (non-Javadoc) Method declared on IWizard.
	 */
	@Override
	public boolean performFinish() {
		IFile file = mainPage.createNewFile();
		if (file == null) {
			return false;
		}

		selectAndReveal(file);

		// Open editor on new file.
		IWorkbenchWindow dw = getWorkbench().getActiveWorkbenchWindow();
		try {
			if (dw != null) {
				IWorkbenchPage page = dw.getActivePage();
				if (page != null) {
					IDE.openEditor(page, file, true);
				}
			}
		} catch (PartInitException e) {
			MessageDialog.openError(dw.getShell(),
					"Problem opening OWL editor", e.getMessage());
		}

		return true;
	}
}
