/**
 * <copyright> 
 *
 * Copyright (c) 2002, 2010 IBM Corporation and others.
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
 * $Id: CommandStack.java,v 1.2 2005/06/08 05:44:08 nickb Exp $
 */
package net.enilink.komma.common.command;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.IOperationApprover2;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

/**
 * A simple and obvious interface for an undoable stack of commands with a
 * listener. See {@link ICommand} for more details about the command methods
 * that this implementation uses and {@link ICommandStackListener} for details
 * about the listener.
 */
public interface ICommandStack {
	/**
	 * <p>
	 * Execute the specified operation and add it to the operations history if
	 * successful. This method is used by clients who wish operation history
	 * listeners to receive notifications before and after the execution of the
	 * operation. Execution of the operation is subject to approval by any
	 * registered {@link IOperationApprover2}. If execution is approved,
	 * listeners will be notified before (<code>ABOUT_TO_EXECUTE</code>) and
	 * after (<code>DONE</code> or <code>OPERATION_NOT_OK</code>).
	 * </p>
	 * <p>
	 * If the operation successfully executes, an additional notification that
	 * the operation has been added to the history (<code>OPERATION_ADDED</code>
	 * ) will be sent.
	 * </p>
	 * 
	 * @param operation
	 *            the operation to be executed and then added to the history
	 * 
	 * @param monitor
	 *            the progress monitor to be used (or <code>null</code>) during
	 *            the operation.
	 * 
	 * @param info
	 *            the IAdaptable (or <code>null</code>) provided by the caller
	 *            in order to supply UI information for prompting the user if
	 *            necessary. When this parameter is not <code>null</code>, it
	 *            should minimally contain an adapter for the
	 *            org.eclipse.swt.widgets.Shell.class.
	 * 
	 * @return the IStatus indicating whether the execution succeeded.
	 * 
	 *         <p>
	 *         The severity code in the returned status describes whether the
	 *         operation succeeded and whether it was added to the history.
	 *         <code>OK</code> severity indicates that the execute operation was
	 *         successful and that the operation has been added to the history.
	 *         Listeners will receive notifications about the operation's
	 *         success (<code>DONE</code>) and about the operation being added
	 *         to the history (<code>OPERATION_ADDED</code>).
	 *         </p>
	 *         <p>
	 *         <code>CANCEL</code> severity indicates that the user cancelled
	 *         the operation and that the operation was not added to the
	 *         history. <code>ERROR</code> severity indicates that the operation
	 *         did not successfully execute and that it was not added to the
	 *         history. Any other severity code is not specifically interpreted
	 *         by the history, and the operation will not be added to the
	 *         history. For all severities other than <code>OK</code>, listeners
	 *         will receive the <code>OPERATION_NOT_OK</code> notification
	 *         instead of the <code>DONE</code> notification if the execution
	 *         was approved and attempted.
	 *         </p>
	 * 
	 * @throws ExecutionException
	 *             if an exception occurred during execution.
	 * 
	 */
	IStatus execute(ICommand command, IProgressMonitor monitor, IAdaptable info)
			throws ExecutionException;

	/**
	 * Returns whether the top command on the stack can be undone.
	 * 
	 * @return whether the top command on the stack can be undone.
	 */
	boolean canUndo();

	/**
	 * <p>
	 * Undo the most recently executed operation.
	 * </p>
	 * 
	 * @param monitor
	 *            the progress monitor to be used for the undo, or
	 *            <code>null</code> if no progress monitor is provided.
	 * @param info
	 *            the IAdaptable (or <code>null</code>) provided by the caller
	 *            in order to supply UI information for prompting the user if
	 *            necessary. When this parameter is not <code>null</code>, it
	 *            should minimally contain an adapter for the
	 *            org.eclipse.swt.widgets.Shell.class.
	 * 
	 * @return the IStatus indicating whether the undo succeeded.
	 * 
	 *         <p>
	 *         The severity code in the returned status describes whether the
	 *         operation succeeded and whether it remains in the history.
	 *         <code>OK</code> severity indicates that the undo operation was
	 *         successful, and (since release 3.2), that the operation will be
	 *         placed on the redo history. (Prior to 3.2, a successfully undone
	 *         operation would not be placed on the redo history if it could not
	 *         be redone. Since 3.2, this is relaxed, and all successfully
	 *         undone operations are placed in the redo history.) Listeners will
	 *         receive the <code>UNDONE</code> notification.
	 *         </p>
	 *         <p>
	 *         Other severity codes (<code>CANCEL</code>, <code>ERROR</code>,
	 *         <code>INFO</code>, etc.) are not specifically interpreted by the
	 *         history. The operation will remain in the history and the
	 *         returned status is simply passed back to the caller. For all
	 *         severities other than <code>OK</code>, listeners will receive the
	 *         <code>OPERATION_NOT_OK</code> notification instead of the
	 *         <code>UNDONE</code> notification if the undo was approved and
	 *         attempted.
	 *         </p>
	 * 
	 * @throws ExecutionException
	 *             if an exception occurred during undo.
	 */

	IStatus undo(IProgressMonitor monitor, IAdaptable info)
			throws ExecutionException;

	/**
	 * Returns whether there are commands past the top of the stack that can be
	 * redone.
	 * 
	 * @return whether there are commands past the top of the stack that can be
	 *         redone.
	 */
	boolean canRedo();

	/**
	 * Returns the command that will be undone if {@link #undo} is called.
	 * 
	 * @return the command that will be undone if {@link #undo} is called.
	 */
	public ICommand getUndoCommand();

	/**
	 * Returns the command that will be redone if {@link #redo} is called.
	 * 
	 * @return the command that will be redone if {@link #redo} is called.
	 */
	public ICommand getRedoCommand();

	/**
	 * Returns the command most recently executed, undone, or redone.
	 * 
	 * @return the command most recently executed, undone, or redone.
	 */
	public ICommand getMostRecentCommand();

	/**
	 * <p>
	 * Redo the most recently undone operation.
	 * </p>
	 * 
	 * @param monitor
	 *            the progress monitor to be used for the redo, or
	 *            <code>null</code> if no progress monitor is provided.
	 * @param info
	 *            the IAdaptable (or <code>null</code>) provided by the caller
	 *            in order to supply UI information for prompting the user if
	 *            necessary. When this parameter is not <code>null</code>, it
	 *            should minimally contain an adapter for the
	 *            org.eclipse.swt.widgets.Shell.class.
	 * @return the IStatus indicating whether the redo succeeded.
	 * 
	 *         <p>
	 *         The severity code in the returned status describes whether the
	 *         operation succeeded and whether it remains in the history.
	 *         <code>OK</code> severity indicates that the redo operation was
	 *         successful and (since release 3.2), that the operation will be
	 *         placed in the undo history. (Prior to 3.2, a successfully redone
	 *         operation would not be placed on the undo history if it could not
	 *         be undone. Since 3.2, this is relaxed, and all successfully
	 *         redone operations are placed in the undo history.) Listeners will
	 *         receive the <code>REDONE</code> notification.
	 *         </p>
	 *         <p>
	 *         Other severity codes (<code>CANCEL</code>, <code>ERROR</code>,
	 *         <code>INFO</code>, etc.) are not specifically interpreted by the
	 *         history. The operation will remain in the history and the
	 *         returned status is simply passed back to the caller. For all
	 *         severities other than <code>OK</code>, listeners will receive the
	 *         <code>OPERATION_NOT_OK</code> notification instead of the
	 *         <code>REDONE</code> notification if the redo was approved and
	 *         attempted.
	 *         </p>
	 * 
	 * @throws ExecutionException
	 *             if an exception occurred during redo.
	 * 
	 */
	IStatus redo(IProgressMonitor monitor, IAdaptable info)
			throws ExecutionException;

	/**
	 * Disposes all the commands in the stack.
	 */
	void flush();

	/**
	 * Adds a listener to the command stack, which will be notified whenever a
	 * command has been processed on the stack.
	 * 
	 * @param listener
	 *            the listener to add.
	 */
	void addCommandStackListener(ICommandStackListener listener);

	/**
	 * Removes a listener from the command stack.
	 * 
	 * @param listener
	 *            the listener to remove.
	 */
	void removeCommandStackListener(ICommandStackListener listener);
}
