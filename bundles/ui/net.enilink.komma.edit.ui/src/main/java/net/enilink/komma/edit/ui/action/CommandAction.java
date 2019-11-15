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
 * $Id: CommandAction.java,v 1.5 2008/05/07 19:08:40 emerks Exp $
 */
package net.enilink.komma.edit.ui.action;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IActionDelegate2;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;

import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.common.command.UnexecutableCommand;
import net.enilink.komma.edit.command.ICommandActionDelegate;
import net.enilink.komma.edit.domain.AdapterFactoryEditingDomain;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.edit.domain.IEditingDomainProvider;
import net.enilink.komma.edit.ui.provider.ExtendedImageRegistry;
import net.enilink.komma.edit.ui.util.EditUIUtil;

/**
 * This class is used to implement a selection-based {@link IAction} on the
 * menubar, the toolbar, or a popup menu by delegating all required behaviour to
 * a {@link ICommand}. All aspects of the action are delegated, namely the
 * enablement state, the menu text, the toolbar icon, and the help tip text. A
 * derived class implements {@link #createActionCommand createActionCommand} to
 * return a command based on the {@link EditingDomain} and the collection of
 * selected objects.
 * 
 * <p>
 * This class can also be used to implement actions not based on a selection, in
 * that case the method {@link #selectionChanged selectionChanged} should be
 * overridden to do nothing.
 */
public class CommandAction extends AbstractActionDelegate implements
		IEditorActionDelegate, IViewActionDelegate, IActionDelegate2 {
	/**
	 * This records the editor or view with which the action is currently
	 * associated.
	 */
	protected IWorkbenchPart workbenchPart;

	/**
	 * This records the proxy action created by the platform.
	 */
	protected IAction action;

	/**
	 * This records the editing domain of the current editor. For global popups,
	 * we try to determine the editing domain from the selected objects
	 * themselves.
	 */
	protected IEditingDomain editingDomain;

	/**
	 * This records the collection of selected objects so that a new command can
	 * be easily constructed after the execution of the command previously
	 * constructed from this selection.
	 */
	protected Collection<Object> collection;

	/**
	 * This records the command that is created each time the selection changes.
	 */
	protected ICommand command;

	/**
	 * This constructs an instance.
	 */
	public CommandAction() {
		super();
	}

	/**
	 * This method must be implemented to create the command for this action,
	 * given the editing domain and the collection of selected objects.
	 */
	protected ICommand createActionCommand(IEditingDomain editingDomain,
			Collection<?> collection) {
		return UnexecutableCommand.INSTANCE;
	}

	/**
	 * This returns the image descriptor if the command does not provide an
	 * override.
	 */
	protected ImageDescriptor getDefaultImageDescriptor() {
		return null;
	}

	/**
	 * This is called immediately after this action delegate is created. We use
	 * this as an opportunity to record the proxy action for later use.
	 */
	public void init(IAction action) {
		this.action = action;
	}

	/**
	 * This is called when this action delegate is no longer needed. This
	 * implementation does nothing.
	 */
	public void dispose() {
		// Do nothing
	}

	/**
	 * For editor actions, the framework calls this when the active editor
	 * changes, so that we can connect with it. We call
	 * {@link #setActiveWorkbenchPart} to record it and its editing domain, if
	 * it can provide one.
	 */
	public void setActiveEditor(IAction action, IEditorPart editorPart) {
		setWorkbenchPart(editorPart);
		this.action = action;
	}

	/**
	 * For view actions, the framework calls this when the view is shown, so
	 * that we can connect with it. We call {@link #setActiveWorkbenchPart} to
	 * record it and its editing domain, if it can provide one.
	 */
	public void init(IViewPart view) {
		setWorkbenchPart(view);
	}

	/**
	 * This records the specified workbench part, and if it is an editing domain
	 * provider, its editing domain.
	 */
	public void setWorkbenchPart(IWorkbenchPart workbenchPart) {
		if (this.workbenchPart != workbenchPart) {
			editingDomain = AdapterFactoryEditingDomain.getEditingDomainFor(workbenchPart);
			this.workbenchPart = workbenchPart;
		}
	}

	/**
	 * This is invoked by the framework so that the action state can be updated.
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		// We will only deal with structured selections.
		//
		if (selection instanceof IStructuredSelection) {
			// Convert the selection to a collection of the selected objects.
			//
			List<?> list = ((IStructuredSelection) selection).toList();
			collection = new ArrayList<Object>(list);

			// If we aren't getting the domain from the workbench part...
			// This happens when this action is used for a global popup action.
			// We try to get the editing domain from one of the objects in the
			// selection.
			if (editingDomain == null) {
				for (Object object : collection) {
					editingDomain = AdapterFactoryEditingDomain
							.getEditingDomainFor(object);
					if (editingDomain != null) {
						break;
					}
				}
			}

			// If we have a good editing domain...
			//
			if (editingDomain != null) {
				// Delegate the action for this object to the editing domain.
				//
				command = createActionCommand(editingDomain, collection);

				// We can enable the action as indicated by the command,
				// and we can set all the other values from the command.
				//
				((Action) action).setEnabled(command.canExecute());

				if (command instanceof ICommandActionDelegate) {
					ICommandActionDelegate commandActionDelegate = (ICommandActionDelegate) command;
					Object object = commandActionDelegate.getImage();
					ImageDescriptor imageDescriptor = objectToImageDescriptor(object);
					if (imageDescriptor != null) {
						((Action) action).setImageDescriptor(imageDescriptor);
					} else if (getDefaultImageDescriptor() != null) {
						((Action) action)
								.setImageDescriptor(getDefaultImageDescriptor());
					}

					if (commandActionDelegate.getText() != null) {
						((Action) action).setText(commandActionDelegate
								.getText());
					}

					if (commandActionDelegate.getDescription() != null) {
						((Action) action).setDescription(commandActionDelegate
								.getDescription());
					}

					if (commandActionDelegate.getToolTipText() != null) {
						((Action) action).setToolTipText(commandActionDelegate
								.getToolTipText());
					}
				}

				// Nothing more to do and we don't want to do the default stuff
				// below.
				//
				return;
			}
		}

		// We just can't do it.
		//
		((Action) action).setEnabled(false);

		// No point in keeping garbage.
		//
		command = null;
		collection = null;

		// Show the colourless image.
		//
		if (getDefaultImageDescriptor() != null) {
			((Action) action).setImageDescriptor(getDefaultImageDescriptor());
		}
	}

	protected ImageDescriptor objectToImageDescriptor(Object object) {
		return ExtendedImageRegistry.getInstance().getImageDescriptor(object);
	}

	@Override
	protected void doRun(IProgressMonitor progressMonitor) {
		// This guard is for extra security, but should not be necessary.
		if (editingDomain != null && command != null) {
			// Use up the command.
			// Note that notification will cause a new command to be created.
			//
			try {
				editingDomain.getCommandStack().execute(command,
						progressMonitor, null);
			} catch (ExecutionException e) {
				handle(e);
			}
		}
	}
}
