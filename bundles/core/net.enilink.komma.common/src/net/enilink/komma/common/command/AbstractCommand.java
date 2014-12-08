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
 * $Id: AbstractCommand.java,v 1.5 2006/12/05 20:19:54 emerks Exp $
 */
package net.enilink.komma.common.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import net.enilink.komma.common.CommonPlugin;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;

/**
 * An abstract implementation of a basic command. Each derived class
 * <bold>must</bold> implement {@link ICommand#execute} and
 * {@link ICommand#redo}, <bold>must</bold> either implement {@link #undo} or
 * implement {@link #canUndo} to return false, and <bold>must</bold> either
 * override {@link #prepare} (this is the preferred approach) or can override
 * {@link #canExecute} directly.
 * 
 * <p>
 * It is very convenient to IUndoContextuse prepare, as it is guaranteed to be
 * called only once just before canExecute is to be tested. It can be
 * implemented to create any additional commands that need to be executed, and
 * the result it yields becomes the permanent cached return value for
 * canExecute.
 * 
 */
public abstract class AbstractCommand implements ICommand, IUndoableOperation {
	List<IUndoContext> contexts = new ArrayList<>();

	/**
	 * The label of this command. May be <code>null</code>.
	 */
	protected String label;

	/**
	 * Keeps track of whether prepare needs to be called. It is tested in
	 * {@link #canExecute} so that {@link #prepare} is called exactly once to
	 * ready the command for execution.
	 */
	protected boolean isPrepared;

	/**
	 * Keeps track of whether the command is executable. It is set in
	 * {@link #canExecute} to the result of calling {@link #prepare}.
	 */
	protected boolean isExecutable;

	/**
	 * Holds a short textual description of the command as returned by
	 * {@link #getDescription} and set by {@link #setDescription}.
	 */
	protected String description;

	private CommandResult commandResult;

	/**
	 * Creates an instance with a default label.
	 * 
	 */
	protected AbstractCommand() {
	}

	/**
	 * Creates an instance with the given label.
	 * 
	 * @param label
	 *            the label.
	 */
	protected AbstractCommand(String label) {
		this.label = label;
	}

	/**
	 * Creates and instance with the given label and description.
	 * 
	 * @param label
	 *            the label.
	 * @param description
	 *            the description.
	 */
	protected AbstractCommand(String label, String description) {
		this(label);
		this.description = description;
	}

	public void addContext(IUndoContext context) {
		if (!contexts.contains(context)) {
			contexts.add(context);
		}
	}

	public final IUndoContext[] getContexts() {
		return (IUndoContext[]) contexts.toArray(new IUndoContext[contexts
				.size()]);
	}

	public final boolean hasContext(IUndoContext context) {
		Assert.isNotNull(context);
		for (int i = 0; i < contexts.size(); i++) {
			IUndoContext otherContext = (IUndoContext) contexts.get(i);
			// have to check both ways because one context may be more general
			// in
			// its matching rules than another.
			if (context.matches(otherContext) || otherContext.matches(context)) {
				return true;
			}
		}
		return false;
	}

	public void removeContext(IUndoContext context) {
		contexts.remove(context);
	}

	/**
	 * Called at most once in {@link #canExecute} to give the command an
	 * opportunity to ready itself for execution. The returned value is stored
	 * in {@link #canExecute}. In other words, you can override this method to
	 * initialize and to yield a cached value for the all subsequent calls to
	 * canExecute.
	 * 
	 * @return whether the command is executable.
	 */
	protected boolean prepare() {
		return false;
	}

	/**
	 * Calls {@link #prepare}, caches the result in {@link #isExecutable}, and
	 * sets {@link #isPrepared} to <code>true</code>; from then on, it will
	 * yield the value of isExecutable.
	 * 
	 * @return whether the command can execute.
	 */
	@Override
	public boolean canExecute() {
		if (!isPrepared) {
			isExecutable = prepare();
			isPrepared = true;
		}

		return isExecutable;
	}

	/**
	 * Returns <code>true</code> because most command should be undoable.
	 * 
	 * @return <code>true</code>.
	 */
	public boolean canUndo() {
		return true;
	}

	/**
	 * Returns <code>true</code> because most command should be redoable.
	 * 
	 * @return <code>true</code>.
	 */
	public boolean canRedo() {
		return true;
	}

	/**
	 * Delegates to {@link #doExecuteWithResult(IProgressMonitor, IAdaptable)}
	 * and sets the command result.
	 */
	@Override
	public IStatus execute(IProgressMonitor progressMonitor, IAdaptable info)
			throws ExecutionException {
		IProgressMonitor monitor = progressMonitor != null ? progressMonitor
				: new NullProgressMonitor();

		CommandResult result = doExecuteWithResult(monitor, info);
		setResult(result);
		return result != null ? result.getStatus() : Status.OK_STATUS;
	}

	/**
	 * Performs the actual work of executing this command. Subclasses must
	 * implement this method to perform some operation.
	 * 
	 * @param progressMonitor
	 *            the progress monitor provided by the operation history. Must
	 *            never be <code>null</code>.
	 * @param info
	 *            the IAdaptable (or <code>null</code>) provided by the caller
	 *            in order to supply UI information for prompting the user if
	 *            necessary. When this parameter is not <code>null</code>, it
	 *            should minimally contain an adapter for the
	 *            org.eclipse.swt.widgets.Shell.class.
	 * 
	 * @return The result of executing this command. May be <code>null</code> if
	 *         the execution status is OK, but there is no meaningful result to
	 *         be returned.
	 * 
	 * @throws ExecutionException
	 *             if, for some reason, I fail to complete the operation
	 */
	protected abstract CommandResult doExecuteWithResult(
			IProgressMonitor progressMonitor, IAdaptable info)
			throws ExecutionException;

	@Override
	public Collection<?> getAffectedObjects() {
		CommandResult result = getCommandResult();
		if (result == null) {
			return Collections.emptyList();
		} else {
			List<Object> objects = new ArrayList<>();
			for (Object value : result.getReturnValues()) {
				if (value != null) {
					objects.add(value);
				}
			}
			return objects;
		}
	}

	@Override
	public Collection<?> getAffectedResources(Object type) {
		return Collections.emptyList();
	}

	/*
	 * Javadoc copied from interface.
	 */
	@Override
	public String getDescription() {
		return description == null ? CommonPlugin.INSTANCE
				.getString("_UI_AbstractCommand_description") : description;
	}

	/**
	 * Sets the description after construction.
	 * 
	 * @param description
	 *            the new description.
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Delegates to {@link #doRedoWithResult(IProgressMonitor, IAdaptable)} and
	 * sets the command result.
	 */
	public IStatus redo(IProgressMonitor progressMonitor, IAdaptable info)
			throws ExecutionException {

		IProgressMonitor monitor = progressMonitor != null ? progressMonitor
				: new NullProgressMonitor();

		CommandResult result = doRedoWithResult(monitor, info);
		setResult(result);
		return result != null ? result.getStatus() : Status.OK_STATUS;
	}

	/**
	 * Performs the actual work of redoing this command. Subclasses must
	 * implement this method to perform the redo.
	 * 
	 * @param progressMonitor
	 *            the progress monitor provided by the operation history. Must
	 *            never be <code>null</code>.
	 * @param info
	 *            the IAdaptable (or <code>null</code>) provided by the caller
	 *            in order to supply UI information for prompting the user if
	 *            necessary. When this parameter is not <code>null</code>, it
	 *            should minimally contain an adapter for the
	 *            org.eclipse.swt.widgets.Shell.class.
	 * 
	 * @return The result of redoing this command. May be <code>null</code> if
	 *         the execution status is OK, but there is no meaningful result to
	 *         be returned.
	 * 
	 * @throws ExecutionException
	 *             on failure to redo
	 */
	protected abstract CommandResult doRedoWithResult(
			IProgressMonitor progressMonitor, IAdaptable info)
			throws ExecutionException;

	/**
	 * Delegates to {@link #doUndoWithResult(IProgressMonitor, IAdaptable)} and
	 * sets the command result.
	 */
	public IStatus undo(IProgressMonitor progressMonitor, IAdaptable info)
			throws ExecutionException {
		IProgressMonitor monitor = progressMonitor != null ? progressMonitor
				: new NullProgressMonitor();

		CommandResult result = doUndoWithResult(monitor, info);
		setResult(result);
		return result != null ? result.getStatus() : Status.OK_STATUS;
	}

	/**
	 * Performs the actual work of undoing this command. Subclasses must
	 * implement this method to perform the undo.
	 * 
	 * @param progressMonitor
	 *            the progress monitor provided by the operation history. Must
	 *            never be <code>null</code>.
	 * @param info
	 *            the IAdaptable (or <code>null</code>) provided by the caller
	 *            in order to supply UI information for prompting the user if
	 *            necessary. When this parameter is not <code>null</code>, it
	 *            should minimally contain an adapter for the
	 *            org.eclipse.swt.widgets.Shell.class.
	 * 
	 * @return The result of undoing this command. May be <code>null</code> if
	 *         the execution status is OK, but there is no meaningful result to
	 *         be returned.
	 * 
	 * @throws ExecutionException
	 *             on failure to undo
	 */
	protected abstract CommandResult doUndoWithResult(
			IProgressMonitor progressMonitor, IAdaptable info)
			throws ExecutionException;

	/**
	 * Creates a new compound command, containing this command and the given
	 * command, that delegates chain to {@link ExtendedCompositeCommand#append}.
	 * 
	 * @param command
	 *            the command to chain with this one.
	 * @return a new chained compound command.
	 */
	@Override
	public ICommand compose(IUndoableOperation operation) {
		CompositeCommand result = new CompositeCommand();
		result.add(this);
		result.add(operation);
		return result;
	}

	// Documentation copied from the interface
	@Override
	public CommandResult getCommandResult() {
		return commandResult;
	}

	/**
	 * Sets the command result.
	 * 
	 * @param result
	 *            the new result for this command.
	 */
	protected final void setResult(CommandResult result) {
		this.commandResult = result;
	}

	public String getLabel() {
		return label != null ? label : CommonPlugin.INSTANCE
				.getString("_UI_AbstractCommand_label");
	}

	/**
	 * Set the label of the command to the specified name.
	 * 
	 * @param name
	 *            the string to be used for the label.
	 */
	public void setLabel(String name) {
		label = name;
	}

	/*
	 * Javadoc copied from interface.
	 */
	@Override
	public void dispose() {
		// Do nothing.
	}

	/**
	 * Returns an abbreviated name using this object's own class' name, without
	 * package qualification, followed by a space separated list of
	 * <tt>field:value</tt> pairs.
	 * 
	 * @return string representation.
	 */
	@Override
	public String toString() {
		String className = getClass().getName();
		int lastDotIndex = className.lastIndexOf('.');
		StringBuffer result = new StringBuffer(lastDotIndex == -1 ? className
				: className.substring(lastDotIndex + 1));
		result.append(" (label: " + getLabel() + ")");
		result.append(" (description: " + description + ")");
		result.append(" (isPrepared: " + isPrepared + ")");
		result.append(" (isExecutable: " + isExecutable + ")");

		return result.toString();
	}

	@Override
	public ICommand reduce() {
		return this;
	}

	/**
	 * A marker interface implemented by commands that don't dirty the model.
	 */
	public static interface INonDirtying {
		// This is just a marker interface.
	}

	/**
	 * A marker interface implemented by commands that don't want their changes
	 * to be recorded.
	 */
	public static interface INoChangeRecording {
		// This is just a marker interface.
	}
}
