/*******************************************************************************
 * Copyright (c) 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
 * 
 * @author Ken Wenzel
 */
public abstract class SimpleCommand extends AbstractCommand {

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
