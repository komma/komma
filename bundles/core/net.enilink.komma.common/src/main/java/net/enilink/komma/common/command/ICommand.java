/*******************************************************************************
 * Copyright (c) 2009, 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.common.command;

import java.util.Collection;

import org.eclipse.core.commands.operations.IOperationApprover;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.commands.operations.OperationHistoryFactory;

/**
 * A self-composing undoable operation that has a {@link CommandResult} and a
 * list of affected {@link IFile}s.
 * <P>
 * Executing, undoing or redoing a command can have a result which clients can
 * obtain by using the {@link #getCommandResult()} method. For example,
 * executing a command that create a new entity may wish to make the new entity
 * accessible to clients through the {@link #getCommandResult()} method.
 * <P>
 * The command provides a list of {@link IFile}s that are expected to be
 * modified when the it is executed, undone or redone. An
 * {@link IOperationApprover} is registered with the
 * {@link OperationHistoryFactory#getOperationHistory()} to validate the
 * modification to these resources.
 * <P>
 * If an error occurs, or the progress monitor is canceled during execute, undo
 * or redo, the command should make every effort to roll back the changes it has
 * made up to that point.
 * 
 * @author khussey
 * @author ldamus
 * 
 * @canBeSeenBy %partners
 */
public interface ICommand extends IUndoableOperation {

	/**
	 * Retrieves the result of executing, undoing, or redoing this command,
	 * depending on which of these operations was last performed. This value can
	 * be <code>null</code> if the operation has no meaningful result.
	 * <P>
	 * The value of this result is undefined if the command has not yet been
	 * executed, undone or redone.
	 * 
	 * @return The result of executing, undoing or redoing this command.
	 */
	public abstract CommandResult getCommandResult();

	/**
	 * Returns the collection of things which this command wishes to present as
	 * the objects affected by the command. Typically could be used as the
	 * selection that should be highlighted to best illustrate the effect of the
	 * command. The result of calling this before an <code>execute</code>,
	 * <code>redo</code>, or <code>undo</code> is undefined. The result may be
	 * different after an <code>undo</code> than it is after an
	 * <code>execute</code> or <code>redo</code>, but the result should be the
	 * same (equivalent) after either an <code>execute</code> or
	 * <code>redo</code>.
	 * 
	 * @return the collection of things which this command wishes to present as
	 *         the objects affected by the command.
	 */
	Collection<?> getAffectedObjects();

	/**
	 * Returns a collection of resources of a given <code>type</code> that are
	 * expected to be modified by this command.
	 * 
	 * @param type
	 *            The type of resource which is expected to be modified
	 * 
	 * @return the list of resources that will be modified
	 */
	Collection<?> getAffectedResources(Object type);

	/**
	 * Returns a new command object that represents a composition of this
	 * command with the specified <code>command</code> parameter.
	 * 
	 * @param operation
	 *            The operation that is to be composed with this command.
	 * @return A command that represents a composition of this command with the
	 *         specified command.
	 */
	ICommand compose(IUndoableOperation operation);

	/**
	 * Returns the simplest form of this command that is equivalent. Use this
	 * method to remove unnecessary nesting of commands.
	 * 
	 * @return the simplest form of this command that is equivalent
	 */
	ICommand reduce();

	/**
	 * Since not all commands have names, reduce() should propogate label from
	 * an upper command that may be thrown away to the resultant reduced
	 * command. The method is needed to assign the label to a nameless command,
	 * because <code>IUndoableOperation</code> is missing this method.
	 * 
	 * @param label
	 *            command's new label
	 */
	void setLabel(String label);

	/**
	 * Returns a string suitable to help describe the effect of this command.
	 * 
	 * @return a string suitable to help describe the effect of this command.
	 */
	String getDescription();
}
