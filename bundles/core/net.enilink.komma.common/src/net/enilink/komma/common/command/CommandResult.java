/*******************************************************************************
 * Copyright (c) 2009, 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.common.command;

import java.util.Arrays;
import java.util.Collection;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import net.enilink.komma.common.CommonPlugin;
import net.enilink.komma.common.internal.CommonStatusCodes;
import net.enilink.komma.common.util.StringStatics;

/**
 * The result of a command execution. Command results have an IStatus and
 * optionally a return value (if applicable). GMF commands are assigned a
 * command result when they are executed, undone, or redone.
 * 
 * @author khussey
 * @author ldamus
 * 
 * @see org.eclipse.core.runtime.IStatus
 * @canBeSeenBy %partners
 */
public final class CommandResult {
	/**
	 * Creates a new command result.
	 * 
	 * @return a new command result
	 */
	public static final CommandResult newCommandResult(IStatus status,
			Object result) {
		return new CommandResult(status, result);
	}

	/**
	 * Creates a new {@link IStatus#OK} command result.
	 * 
	 * @return a new OK command result with no return value
	 * 
	 */
	public static final CommandResult newOKCommandResult() {
		return new CommandResult(new Status(IStatus.OK, CommonPlugin.PLUGIN_ID,
				CommonStatusCodes.OK, StringStatics.BLANK, null), null);
	}

	/**
	 * Creates a new {@link IStatus#OK} command result with the specified return
	 * <code>value</code>.
	 * 
	 * @param value
	 *            the command return result
	 * @return a new OK command result with the return <code>value</code>
	 * 
	 */
	public static final CommandResult newOKCommandResult(Object result) {
		return new CommandResult(new Status(IStatus.OK, CommonPlugin.PLUGIN_ID,
				CommonStatusCodes.OK, StringStatics.BLANK, null), result);
	}

	/**
	 * Creates a new {@link IStatus#CANCEL} command result with no return value.
	 * 
	 * @return a new CANCEL command result with no return value
	 */
	public static final CommandResult newCancelledCommandResult() {
		return new CommandResult(new Status(IStatus.CANCEL,
				CommonPlugin.PLUGIN_ID, CommonStatusCodes.CANCELLED,
				"The operation has been cancelled.", null), null);
	}

	/**
	 * Creates a new {@link IStatus#ERROR} command result with no return value.
	 * 
	 * @param errorMessage
	 *            the error message
	 * @return a new ERROR command result with no return value
	 */
	public static final CommandResult newErrorCommandResult(String errorMessage) {
		return new CommandResult(new Status(IStatus.ERROR,
				CommonPlugin.PLUGIN_ID, CommonStatusCodes.COMMAND_FAILURE,
				errorMessage, null), null);
	}

	/**
	 * Creates a new {@link IStatus#ERROR} command result with no return value.
	 * 
	 * @param errorMessage
	 *            the error message
	 * @return a new ERROR command result with no return value
	 */
	public static final CommandResult newErrorCommandResult(Throwable throwable) {
		return new CommandResult(new Status(IStatus.ERROR,
				CommonPlugin.PLUGIN_ID, CommonStatusCodes.COMMAND_FAILURE,
				throwable.getLocalizedMessage(), throwable), null);
	}

	/**
	 * Creates a new {@link IStatus#WARNING} command result with a return
	 * <code>value</code>.
	 * 
	 * @param warningMessage
	 *            the warning message
	 * @param value
	 *            the command return result
	 * @return a new WARNING command result with the return <code>value</code>
	 */
	public static final CommandResult newWarningCommandResult(
			String warningMessage, Object result) {
		return new CommandResult(new Status(IStatus.WARNING,
				CommonPlugin.PLUGIN_ID, CommonStatusCodes.OK, warningMessage,
				null), result);
	}

	/**
	 * The return value for this command, if applicable.
	 */
	private final Object returnValue;

	/**
	 * The status of executing, undoing, or redoing this command.
	 */
	private final IStatus status;

	/**
	 * Constructs a new command result with the specified status and a default
	 * return value.
	 * 
	 * @param status
	 *            The status for the new command result.
	 */
	public CommandResult(IStatus status) {
		this(status, null);
	}

	/**
	 * Constructs a new command result with the specified status and return
	 * value.
	 * 
	 * @param status
	 *            The status for the new command result.
	 * @param returnValue
	 *            The return value for the new command result.
	 */
	public CommandResult(IStatus status, Object returnValue) {
		super();

		assert null != status : "null status"; //$NON-NLS-1$

		this.status = status;
		this.returnValue = returnValue;
	}

	/**
	 * Retrieves the status of the command that is executed, undone or redone.
	 * 
	 * @return The status.
	 */
	public IStatus getStatus() {
		return status;
	}

	/**
	 * The value returned by the execute, undo or redo of an operation.
	 * 
	 * @return the return value; may be <code>null</code>
	 */
	public Object getReturnValue() {
		return returnValue;
	}

	/**
	 * The values returned by the execute, undo or redo of an operation.
	 * 
	 * @return the return value; may be <code>null</code>
	 */
	public Collection<?> getReturnValues() {
		Object returnValue = getReturnValue();
		if (returnValue instanceof Collection<?>) {
			return (Collection<?>) returnValue;
		}
		return Arrays.asList(returnValue);
	}
}
