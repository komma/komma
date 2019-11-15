/**
 * <copyright> 
 *
 * Copyright (c) 2002, 2010 IBM Corporation and others.
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
 * $Id: CommandWrapper.java,v 1.3 2006/12/05 20:19:54 emerks Exp $
 */
package net.enilink.komma.common.command;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;

import net.enilink.komma.common.CommonPlugin;

/**
 * A command that wraps another command. All the {@link ICommand} methods are
 * delegated to the wrapped command.
 * 
 * <p>
 * There are two typical usage patterns. One typical use for this command is to
 * modify the behaviour of a command that you can't subclass, i.e., a decorator
 * pattern:
 * 
 * <pre>
 * Command decoratedCommand = new CommandWrapper(someOtherCommand) {
 * 	public void execute() {
 * 		doSomethingBeforeExecution();
 * 		super.execute();
 * 		doSomethingAfterExecution();
 * 	}
 * 
 * 	public Collection getResult() {
 * 		return someOtherResult();
 * 	}
 * };
 *</pre>
 * 
 * The other typical use is to act as a proxy for a command who's creation is
 * delayed:
 * 
 * <pre>
 * Command proxyCommand = new CommandWrapper() {
 * 	public Command createCommand() {
 * 		return createACommandSomehow();
 * 	}
 * };
 *</pre>
 */
public class CommandWrapper extends AbstractCommand {
	/**
	 * The command for which this is a proxy or decorator.
	 */
	protected ICommand command;

	/**
	 * Creates a decorator instance for the given command.
	 * 
	 * @param command
	 *            the command to wrap.
	 */
	public CommandWrapper(ICommand command) {
		super(command.getLabel(), command.getDescription());
		this.command = command;
	}

	/**
	 * Creates a decorator instance with the given label for the given command.
	 * 
	 * @param label
	 *            the label of the wrapper
	 * @param command
	 *            the command to wrap.
	 */
	protected CommandWrapper(String label, ICommand command) {
		super(label, command.getDescription());
		this.command = command;
	}

	/**
	 * Creates a decorator instance with the given label and description for the
	 * given command.
	 * 
	 * @param label
	 *            the label of the wrapper
	 * @param description
	 *            the description of the wrapper
	 * @param command
	 *            the command to wrap.
	 */
	public CommandWrapper(String label, String description, ICommand command) {
		super(label, description);
		this.command = command;
	}

	/**
	 * Creates a commandless proxy instance. The wrapped command will be created
	 * by a {@link #createCommand} callback. Since a proxy command like this is
	 * pointless unless you override some method, this constructor is protected.
	 */
	protected CommandWrapper() {
		super();
	}

	/**
	 * Creates a commandless proxy instance, with the given label. The command
	 * will be created by a {@link #createCommand} callback. Since a proxy
	 * command like this is pointless unless you override some method, this
	 * constructor is protected.
	 * 
	 * @param label
	 *            the label of the wrapper
	 */
	protected CommandWrapper(String label) {
		super(label);
	}

	/**
	 * Creates a commandless proxy instance, with the given label and
	 * description. The command will be created by a {@link #createCommand}
	 * callback. Since a proxy command like this is pointless unless you
	 * override some method, this constructor is protected.
	 * 
	 * @param label
	 *            the label of the wrapper
	 * @param description
	 *            the description of the wrapper
	 */
	protected CommandWrapper(String label, String description) {
		super(label, description);
	}

	/**
	 * Returns the command for which this is a proxy or decorator. This may be
	 * <code>null</code> before {@link #createCommand} is called.
	 * 
	 * @return the command for which this is a proxy or decorator.
	 */
	public ICommand getCommand() {
		return command;
	}

	/**
	 * Create the command being proxied. This implementation just return
	 * <code>null</code>. It is called by {@link #prepare}.
	 * 
	 * @return the command being proxied.
	 */
	protected ICommand createCommand() {
		return null;
	}

	/**
	 * Returns whether the command can execute. This implementation creates the
	 * command being proxied using {@link #createCommand}, if the command wasn't
	 * given in the constructor.
	 * 
	 * @return whether the command can execute.
	 */
	@Override
	protected boolean prepare() {
		if (command == null) {
			command = createCommand();
		}

		boolean result = command.canExecute();
		return result;
	}

	/**
	 * Delegates to the execute method of the command.
	 */
	protected CommandResult doExecuteWithResult(
			IProgressMonitor progressMonitor, IAdaptable info)
			throws ExecutionException {
		if (command != null) {
			command.execute(progressMonitor, info);
			return command.getCommandResult();
		}
		return CommandResult.newOKCommandResult();
	}

	/**
	 * Delegates to the canUndo method of the command.
	 */
	@Override
	public boolean canUndo() {
		return command == null || command.canUndo();
	}

	/**
	 * Delegates to the undo method of the command.
	 */
	protected CommandResult doUndoWithResult(IProgressMonitor progressMonitor,
			IAdaptable info) throws ExecutionException {
		if (command != null) {
			command.undo(progressMonitor, info);
			return command.getCommandResult();
		}
		return CommandResult.newOKCommandResult();
	}

	/**
	 * Delegates to the redo method of the command.
	 */
	protected CommandResult doRedoWithResult(IProgressMonitor progressMonitor,
			IAdaptable info) throws ExecutionException {
		if (command != null) {
			command.redo(progressMonitor, info);
			return command.getCommandResult();
		}
		return CommandResult.newOKCommandResult();
	}

	/**
	 * Delegates to the getAffectedObjects method of the command.
	 * 
	 * @return the result.
	 */
	@Override
	public Collection<?> getAffectedObjects() {
		return command == null ? Collections.EMPTY_LIST : command
				.getAffectedObjects();
	}

	/**
	 * Delegates to the getLabel method of the command.
	 * 
	 * @return the label.
	 */
	@Override
	public String getLabel() {
		return command == null ? super.getLabel() : command.getLabel();
	}

	/**
	 * Delegates to the getDescription method of the command.
	 * 
	 * @return the description.
	 */
	@Override
	public String getDescription() {
		return description == null ? command == null ? CommonPlugin.INSTANCE
				.getString("_UI_CommandWrapper_description") : command
				.getDescription() : description;
	}

	/**
	 * Delegates to the dispose method of the command.
	 */
	@Override
	public void dispose() {
		if (command != null) {
			command.dispose();
		}
	}

	/*
	 * Javadoc copied from base class.
	 */
	@Override
	public String toString() {
		StringBuffer result = new StringBuffer(super.toString());
		result.append(" (command: " + command + ")");

		return result.toString();
	}
}
