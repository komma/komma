/*******************************************************************************
 * Copyright (c) 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.common.command;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Base class for simple commands which are not aware of undo or redo by
 * themselves.
 */
public abstract class SimpleCommand extends AbstractCommand {
	public SimpleCommand() {
	}

	public SimpleCommand(String label) {
		super(label);
	}

	public SimpleCommand(String label, String description) {
		super(label, description);
	}

	@Override
	protected CommandResult doUndoWithResult(IProgressMonitor progressMonitor,
			IAdaptable info) throws ExecutionException {
		return CommandResult.newOKCommandResult();
	}

	@Override
	protected CommandResult doRedoWithResult(IProgressMonitor progressMonitor,
			IAdaptable info) throws ExecutionException {
		return CommandResult.newOKCommandResult();
	}

	@Override
	protected boolean prepare() {
		return true;
	}
}
