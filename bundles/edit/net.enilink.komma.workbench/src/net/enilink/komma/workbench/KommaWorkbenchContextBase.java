/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 *  $$RCSfile: EMFWorkbenchContextBase.java,v $$
 *  $$Revision: 1.2 $$  $$Date: 2005/02/15 23:04:14 $$ 
 */
package net.enilink.komma.workbench;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

import net.enilink.komma.model.IModel;
import net.enilink.komma.workbench.internal.nls.WorkbenchResourceHandler;

/**
 * ContextBase for Komma Workbench.
 * 
 * <p>
 * This is meant to be subclassed as needed for additional or override function.
 * It will be instantiated by default.
 * </p>
 * 
 * @since 1.0.0
 */
public class KommaWorkbenchContextBase {
	protected IProject project;
	protected IWorkbenchURIConverter uriConverter;

	/**
	 * Construct with a project.
	 * 
	 * @param project
	 * 
	 * @since 1.0.0
	 */
	public KommaWorkbenchContextBase(IProject project) {
		if (project == null)
			throw new IllegalArgumentException(WorkbenchResourceHandler
					.getString("KommaWorkbenchContextBase_ERROR_1")); //$NON-NLS-1$
		this.project = project;

		setURIConverter(new WorkbenchURIConverterImpl(getProject()));
	}

	/**
	 * Delete the file associated with the model.
	 * 
	 * @param model
	 * 
	 * @since 1.0.0
	 */
	public void deleteFile(IModel model) {
		throw new UnsupportedOperationException(WorkbenchResourceHandler
				.getString("KommaWorkbenchContextBase_ERROR_0")); //$NON-NLS-1$
	}

	/**
	 * Delete the model from the workspace.
	 * 
	 * @param model
	 * @throws CoreException
	 * 
	 * @since 1.0.0
	 */
	public void deleteModel(IModel model) throws CoreException {
		if (model != null)
			deleteFile(model);
	}

	/**
	 * Dispose of the context base.
	 * 
	 * 
	 * @since 1.0.0
	 */
	public void dispose() {
		project = null;
	}

	/**
	 * Get the project this context is associated with.
	 * 
	 * @return project
	 * 
	 * @since 1.0.0
	 */
	public IProject getProject() {
		return project;
	}

	public IWorkbenchURIConverter getURIConverter() {
		return uriConverter;
	}

	protected void setURIConverter(IWorkbenchURIConverter uriConverter) {
		this.uriConverter = uriConverter;
	}
}