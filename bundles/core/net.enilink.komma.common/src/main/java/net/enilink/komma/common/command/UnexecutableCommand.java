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
 * $Id: UnexecutableCommand.java,v 1.3 2006/12/05 20:19:54 emerks Exp $
 */
package net.enilink.komma.common.command;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import net.enilink.komma.common.util.StringStatics;

/**
 * A singleton {@link UnexecutableCommand#INSTANCE} that cannot execute.
 */
public class UnexecutableCommand extends AbstractCommand {
	/**
	 * The shared instance.
	 */
	public static final UnexecutableCommand INSTANCE = new UnexecutableCommand();

	/**
	 * Creates an instance of an unexecutable command.
	 */
	protected UnexecutableCommand() {
		super(StringStatics.BLANK, null);
	}

	/**
	 * Specific instances of the UnexecutableCommand can be created in order to
	 * provide feedback. If no feedback is required, the shared instance should
	 * be used.
	 * 
	 * @since 1.2
	 */
	public UnexecutableCommand(IStatus status) {
		super(StringStatics.BLANK);
		setResult(new CommandResult(status));
	}

	/**
	 * @throws UnsupportedOperationException
	 */
	protected CommandResult doExecuteWithResult(
			IProgressMonitor progressMonitor, IAdaptable info)
			throws ExecutionException {

		throw new UnsupportedOperationException("doExecuteWithResult"); //$NON-NLS-1$
	}

	/**
	 * @throws UnsupportedOperationException
	 */
	protected CommandResult doRedoWithResult(IProgressMonitor progressMonitor,
			IAdaptable info) throws ExecutionException {

		throw new UnsupportedOperationException("doRedoWithResult"); //$NON-NLS-1$
	}

	/**
	 * @throws UnsupportedOperationException
	 */
	protected CommandResult doUndoWithResult(IProgressMonitor progressMonitor,
			IAdaptable info) throws ExecutionException {

		throw new UnsupportedOperationException("doUndoWithResult"); //$NON-NLS-1$
	}

	/**
	 * @return false.
	 */
	public boolean canExecute() {
		return false;
	}

	/**
	 * @return false.
	 */
	public boolean canRedo() {
		return false;
	}

	/**
	 * @return false.
	 */
	public boolean canUndo() {
		return false;
	}
}
