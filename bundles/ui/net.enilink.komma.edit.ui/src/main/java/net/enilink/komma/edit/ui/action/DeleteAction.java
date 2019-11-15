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
 * $Id: DeleteAction.java,v 1.5 2006/12/28 06:50:04 marcelop Exp $
 */
package net.enilink.komma.edit.ui.action;

import java.util.Collection;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.ui.IWorkbenchPage;

import net.enilink.komma.common.command.CompositeCommand;
import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.edit.command.DeleteCommand;
import net.enilink.komma.edit.command.RemoveCommand;
import net.enilink.komma.edit.ui.KommaEditUIPlugin;

/**
 * A delete action removes objects from their parent containers, optionally
 * cleaning up other references to the objects. It is implemented by creating a
 * {@link RemoveCommand} or {@link DeleteCommand}.
 */
public class DeleteAction extends CommandActionHandler {
	/**
	 * Whether the action should clean up all references to deleted objects.
	 */
	protected boolean removeAllReferences;

	public DeleteAction(IWorkbenchPage page, boolean removeAllReferences) {
		super(page, KommaEditUIPlugin.INSTANCE
				.getString(removeAllReferences ? "_UI_Delete_menu_item" : "_UI_DeleteFromParent_menu_item"));
		this.removeAllReferences = removeAllReferences;
	}

	public DeleteAction(IWorkbenchPage page) {
		this(page, false);
	}

	@Override
	protected boolean updateSelection(IStructuredSelection selection) {
		// use knowledge about current tree view to create a "remove this from
		// its parent element" command
		if (selection instanceof ITreeSelection && !removeAllReferences) {
			ITreeSelection treeSelection = (ITreeSelection) selection;
			CompositeCommand command = new CompositeCommand();
			for (TreePath treePath : treeSelection.getPaths()) {
				int segments = treePath.getSegmentCount();
				if (segments > 1) {
					command.add(RemoveCommand.create(domain, treePath.getSegment(segments - 2), null,
							treePath.getSegment(segments - 1)));
				} else {
					command.add(RemoveCommand.create(domain, treePath.getLastSegment()));
				}
			}
			this.command = command;
			return command.canExecute();
		}
		return super.updateSelection(selection);
	}

	@Override
	public ICommand createCommand(Collection<?> selection) {
		CompositeCommand command = new CompositeCommand();
		command.add(RemoveCommand.create(domain, selection));
		if (removeAllReferences) {
			command.add(DeleteCommand.create(domain, selection));
		}
		return command;
	}
}
