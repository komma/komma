/**
 * <copyright>
 *
 * Copyright (c) 2004, 2010 IBM Corporation and others.
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
 * $Id: WorkbenchWindowActionDelegate.java,v 1.2 2005/06/08 06:24:33 nickb Exp $
 */
package net.enilink.komma.common.ui.action;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

/**
 * Simple implementation of {@link IWorkbenchWindowActionDelegate}.
 */
public abstract class WorkbenchWindowActionDelegate implements
		IWorkbenchWindowActionDelegate {
	private IWorkbenchWindow window;
	private ISelection selection;

	/**
	 * @return the {@link IWorkbenchWindow} associated with this instance.
	 */
	protected IWorkbenchWindow getWindow() {
		return window;
	}

	/**
	 * @return the {@link ISelection}.
	 */
	protected ISelection getSelection() {
		return selection;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.ui.IWorkbenchWindowActionDelegate#init(org.eclipse.ui.
	 * IWorkbenchWindow)
	 */
	public void init(IWorkbenchWindow window) {
		this.window = window;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action
	 * .IAction, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		this.selection = selection;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#dispose()
	 */
	public void dispose() {
		window = null;
		selection = null;
	}
}
