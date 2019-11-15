/**
 * <copyright> 
 *
 * Copyright (c) 2002, 2009 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 * 
 * Contributors: 
 *   IBM - Initial API and implementation
 *
 * </copyright>
 *
 * $Id: CommandActionHandler.java,v 1.6 2008/05/07 19:08:40 emerks Exp $
 */
package net.enilink.komma.edit.ui.action;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;

import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.common.command.UnexecutableCommand;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.edit.domain.IEditingDomainProvider;

/*
 * This base action class implements an action by creating a command and delegating to it;
 * it's main use is as a base class for action handlers.
 */
public class CommandActionHandler extends AbstractActionHandler {
	/**
	 * This keeps track of the command delegate that is created by
	 * {@link #createCommand}.
	 */
	protected ICommand command;

	/**
	 * This keeps track of the editing domain of the action.
	 */
	protected IEditingDomain domain;

	/**
	 * This constructs and instance in this editing domain.
	 */
	public CommandActionHandler(IWorkbenchPage page) {
		super(page);
	}

	/**
	 * This constructs and instance in this editing domain.
	 */
	public CommandActionHandler(IWorkbenchPage page, String label) {
		super(page);
		setText(label);
	}

	/**
	 * This default implementation simply returns
	 * {@link org.eclipse.emf.common.command.UnexecutableCommand#INSTANCE}.
	 */
	public ICommand createCommand(Collection<?> selection) {
		return UnexecutableCommand.INSTANCE;
	}

	@Override
	protected void doRun(IProgressMonitor progressMonitor) {
		try {
			domain.getCommandStack().execute(command, progressMonitor, null);
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This returns the action's domain.
	 */
	public IEditingDomain getEditingDomain() {
		return domain;
	}

	@Override
	protected boolean isSelectionListener() {
		return true;
	}

	@Override
	public void refresh() {
		setEnabled(domain != null && updateSelection(getStructuredSelection()));
	}

	/**
	 * This sets the action's domain.
	 */
	public void setEditingDomain(IEditingDomain domain) {
		this.domain = domain;
	}

	/**
	 * When the selection changes, this will call {@link #createCommand} with
	 * the appropriate collection of selected objects.
	 */
	protected boolean updateSelection(IStructuredSelection selection) {
		if (selection == null) {
			return false;
		}
		List<?> list = selection.toList();
		Collection<Object> collection = new ArrayList<Object>(list);
		command = createCommand(collection);
		return command.canExecute();
	}

	@Override
	protected void setWorkbenchPart(IWorkbenchPart workbenchPart) {
		super.setWorkbenchPart(workbenchPart);
		IEditingDomainProvider provider = null;
		if (workbenchPart != null) {
			provider = workbenchPart.getAdapter(IEditingDomainProvider.class);
		}
		if (provider != null) {
			setEditingDomain(provider.getEditingDomain());
		} else {
			// ensures that action is disabled for parts without editing domain
			setEditingDomain(null);
		}
	}
}
