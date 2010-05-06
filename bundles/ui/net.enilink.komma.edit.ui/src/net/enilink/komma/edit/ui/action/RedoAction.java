/**
 * <copyright> 
 *
 * Copyright (c) 2002, 2009 IBM Corporation and others.
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
 * $Id: RedoAction.java,v 1.4 2006/12/28 06:50:05 marcelop Exp $
 */
package net.enilink.komma.edit.ui.action;

import java.util.EventObject;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;

import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.common.command.ICommandStackListener;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.edit.domain.IEditingDomainProvider;
import net.enilink.komma.edit.ui.KommaEditUIPlugin;

/**
 * An redo action is implemented by using the
 * {@link org.eclipse.ICommandStack.common.command.CommandStack}.
 */
public class RedoAction extends AbstractActionHandler implements
		ICommandStackListener {
	protected IEditingDomain domain;

	public RedoAction(IWorkbenchPage page, IEditingDomain domain) {
		super(page);
		this.domain = domain;
		setText(KommaEditUIPlugin.INSTANCE.getString("_UI_Redo_menu_item",
				new Object[] { "" }));
	}

	public RedoAction(IWorkbenchPage page) {
		this(page, null);
	}

	/**
	 * This returns the action's domain.
	 */
	public IEditingDomain getEditingDomain() {
		return domain;
	}

	/**
	 * This sets the action's domain.
	 */
	public void setEditingDomain(IEditingDomain domain) {
		if (this.domain != domain) {
			if (this.domain != null) {
				this.domain.getCommandStack().removeCommandStackListener(this);
			}

			this.domain = domain;
			this.domain.getCommandStack().addCommandStackListener(this);
		}
	}

	/**
	 * @since 2.1.0
	 */
	public void setWorkbenchPart(IWorkbenchPart workbenchPart) {
		super.setWorkbenchPart(workbenchPart);
		if (workbenchPart instanceof IEditingDomainProvider) {
			setEditingDomain(((IEditingDomainProvider) workbenchPart)
					.getEditingDomain());
		}
	}

	@Override
	protected void doRun(IProgressMonitor progressMonitor) {
		try {
			domain.getCommandStack().redo(progressMonitor, null);
		} catch (ExecutionException e) {
			handle(e);
		}
	}

	@Override
	public void refresh() {
		if (domain == null) {
			setEnabled(false);
		} else {
			setEnabled(domain.getCommandStack().canRedo());

			ICommand redoCommand = domain.getCommandStack().getRedoCommand();
			if (redoCommand != null && redoCommand.getLabel() != null) {
				setText(KommaEditUIPlugin.INSTANCE.getString("_UI_Redo_menu_item",
						new Object[] { redoCommand.getLabel() }));
			} else {
				setText(KommaEditUIPlugin.INSTANCE.getString("_UI_Redo_menu_item",
						new Object[] { "" }));
			}

			if (redoCommand != null && redoCommand.getDescription() != null) {
				setDescription(KommaEditUIPlugin.INSTANCE.getString(
						"_UI_Redo_menu_item_description",
						new Object[] { redoCommand.getDescription() }));
			} else {
				setDescription(KommaEditUIPlugin.INSTANCE
						.getString("_UI_Redo_menu_item_simple_description"));
			}
		}
	}

	@Override
	public void dispose() {
		if (this.domain != null) {
			this.domain.getCommandStack().removeCommandStackListener(this);
			this.domain = null;
		}
		super.dispose();
	}

	@Override
	public void commandStackChanged(EventObject event) {
		if (this.domain != null) {
			refresh();
		}
	}
}
