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
 * $Id: OverrideableCommand.java,v 1.4 2007/06/14 18:32:42 emerks Exp $
 */
package net.enilink.komma.edit.command;

import java.util.Collection;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import net.enilink.komma.common.command.CommandResult;
import net.enilink.komma.common.command.ICommand;

/**
 * This represents a command that can be overridden by another command. The
 * intended use of this is that an overrideable command should call
 * {@link net.enilink.komma.edit.domain.IEditingDomain#createOverrideCommand
 * EditingDomain.createOverrideCommand} in its constructor to set up the
 * override command. All its {@link ICommand} methods should then be guarded as
 * follows:
 * 
 * <pre>
 * public void execute() {
 * 	if (getOverride() != null) {
 * 		getOverride().execute();
 * 	} else {
 * 		doExecute();
 * 	}
 * }
 * </pre>
 * 
 * The contract with the overriding command is that the overrideable command
 * will implement all its methods in corresponding doXxx methods, e.g.,
 * execute() is implemented in doExecute(), so that the overriding command can
 * call back to the overrideable command's doXxx methods if it wants to extend
 * rather than replace the original implementation.
 * {@link AbstractOverrideableCommand} provides a convenient base implementation
 * for overrideable commands.
 */
public interface IOverrideableCommand extends ICommand {
	/**
	 * This returns the command that overrides this command.
	 */
	ICommand getOverride();

	/**
	 * This sets the command that overrides this command.
	 */
	void setOverride(ICommand overrideCommand);

	/**
	 * This is overrideable command's implementation of canExecute.
	 */
	boolean doCanExecute();

	/**
	 * This is overrideable command's implementation of execute.
	 */
	IStatus doExecute(IProgressMonitor progressMonitor, IAdaptable info)
			throws ExecutionException;

	/**
	 * This is overrideable command's implementation of canUndo.
	 */
	boolean doCanUndo();

	/**
	 * This is overrideable command's implementation of undo.
	 */
	IStatus doUndo(IProgressMonitor progressMonitor, IAdaptable info)
			throws ExecutionException;

	/**
	 * This is overrideable command's implementation of redo.
	 */
	IStatus doRedo(IProgressMonitor progressMonitor, IAdaptable info)
			throws ExecutionException;

	/**
	 * This is overrideable command's implementation of getAffectedObjects.
	 */
	Collection<?> doGetAffectedObjects();

	/**
	 * This is overrideable command's implementation of getLabel.
	 */
	String doGetLabel();

	/**
	 * This is overrideable command's implementation of getDescription.
	 */
	String doGetDescription();

	/**
	 * This is overrideable command's implementation of getAffectedResources.
	 */
	Collection<?> doGetAffectedResources(Object type);

	/**
	 * This is overrideable command's implementation of getCommandResult.
	 */
	CommandResult doGetCommandResult();

	/**
	 * This is overrideable command's implementation of dispose.
	 */
	void doDispose();
}
