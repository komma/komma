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
 * $Id: UndoAction.java,v 1.4 2006/12/28 06:50:05 marcelop Exp $
 */
package net.enilink.komma.edit.ui.action;

import java.util.EventObject;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;

import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.common.command.ICommandStackListener;
import net.enilink.komma.edit.domain.AdapterFactoryEditingDomain;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.edit.ui.KommaEditUIPlugin;

/**
 * An undo action is implemented by using the
 * {@link org.eclipse.ICommandStack.common.command.CommandStack}.
 */
public class UndoAction extends AbstractActionHandler implements
		ICommandStackListener {
	protected IEditingDomain domain;

	public UndoAction(IWorkbenchPage page, IEditingDomain domain) {
		super(page);
		this.domain = domain;
		setText(KommaEditUIPlugin.INSTANCE.getString("_UI_Undo_menu_item",
				new Object[] { "" }));
	}

	public UndoAction(IWorkbenchPage page) {
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
			if (this.domain != null) {
				this.domain.getCommandStack().addCommandStackListener(this);
			}
		}
	}

	@Override
	protected void doRun(IProgressMonitor progressMonitor) {
		try {
			domain.getCommandStack().undo(progressMonitor, null);
		} catch (ExecutionException e) {
			handle(e);
		}
	}

	public void refresh() {
		if (domain == null) {
			setEnabled(false);
		} else {
			setEnabled(domain.getCommandStack().canUndo());

			ICommand undoCommand = domain.getCommandStack().getUndoCommand();
			if (undoCommand != null && undoCommand.getLabel() != null) {
				setText(KommaEditUIPlugin.INSTANCE.getString("_UI_Undo_menu_item",
						new Object[] { undoCommand.getLabel() }));
			} else {
				setText(KommaEditUIPlugin.INSTANCE.getString("_UI_Undo_menu_item",
						new Object[] { "" }));
			}

			if (undoCommand != null && undoCommand.getDescription() != null) {
				setDescription(KommaEditUIPlugin.INSTANCE.getString(
						"_UI_Undo_menu_item_description",
						new Object[] { undoCommand.getDescription() }));
			} else {
				setDescription(KommaEditUIPlugin.INSTANCE
						.getString("_UI_Undo_menu_item_simple_description"));
			}
		}
	}

	@Override
	protected void setWorkbenchPart(IWorkbenchPart workbenchPart) {
		super.setWorkbenchPart(workbenchPart);
		setEditingDomain(AdapterFactoryEditingDomain.getEditingDomainFor(workbenchPart));
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
