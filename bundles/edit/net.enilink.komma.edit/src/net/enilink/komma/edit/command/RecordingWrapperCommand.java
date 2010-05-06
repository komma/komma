/*******************************************************************************
 * Copyright (c) 2009 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.edit.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import net.enilink.komma.common.command.AbstractCommand;
import net.enilink.komma.common.command.CommandResult;
import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.model.IModel;
import net.enilink.komma.repository.change.IChangeDescription;
import net.enilink.komma.core.IKommaManager;
import net.enilink.komma.core.IKommaTransaction;

/**
 * A partial command implementation
 * that records the changes made by a subclass's direct manipulation of objects
 * via the metamodel's API. This simplifies the programming model for complex
 * commands (not requiring composition of set/add/remove commands) while still
 * providing automatic undo/redo support.
 * <p>
 * Subclasses are simply required to implement the {@link #doExecute()} method
 * to make the desired changes to the model. Note that, because changes are
 * recorded for automatic undo/redo, the concrete command must not make any
 * changes that cannot be recorded (unless it does not matter that they will not
 * be undone).
 * </p>
 * 
 * @author Ken Wenzel
 */
public class RecordingWrapperCommand extends AbstractCommand {
	private IChangeDescription change;
	private ICommand command;
	private IEditingDomain domain;

	/**
	 * Initializes me with the editing domain in which I am to be executed.
	 * 
	 * @param domain
	 *            my domain
	 */
	public RecordingWrapperCommand(IEditingDomain domain, ICommand command) {
		this.domain = domain;
		this.command = command;
	}

	private boolean canApplyChange() {
		return change == null || change.canUndo();
	}

	/**
	 * I can be redone if I successfully recorded the changes that I executed.
	 * Subclasses would not normally need to override this method.
	 */
	public boolean canRedo() {
		return canApplyChange() || command.canRedo();
	}

	/**
	 * I can be undone if I successfully recorded the changes that I executed.
	 * Subclasses would not normally need to override this method.
	 */
	@Override
	public boolean canUndo() {
		return canApplyChange() || command.canUndo();
	}

	/**
	 * Extends the inherited implementation by disposing my change description,
	 * if any.
	 */
	@Override
	public void dispose() {
		super.dispose();

		change = null;
	}

	/**
	 * Implements the execution with automatic recording of undo information.
	 * Delegates the actual model changes to the subclass's implementation of
	 * the {@link #doExecute()} method.
	 * 
	 * @see #doExecute()
	 */
	@Override
	public CommandResult doExecuteWithResult(IProgressMonitor progressMonitor,
			IAdaptable info) throws ExecutionException {
		IEditingDomain.Internal internalDomain = (IEditingDomain.Internal) domain;

		Collection<?> affectedModels = new HashSet<Object>(command
				.getAffectedResources(IModel.class));
		List<IKommaTransaction> startedTransactions = new ArrayList<IKommaTransaction>(
				affectedModels.size());
		for (Object model : affectedModels) {
			IKommaManager modelManager = ((IModel) model).getManager();
			IKommaTransaction transaction = modelManager.getTransaction();
			if (!transaction.isActive()) {
				// TODO check if next Sesame version supports nested
				// transactions
				// TODO check if transaction can be executed in READ_UNCOMMITED
				// mode
				// transaction.begin();
				// startedTransactions.add(transaction);
			}
		}

		boolean rollback = true;
		try {
			internalDomain.getChangeRecorder().beginRecording();

			IStatus status = command.execute(progressMonitor, info);

			if (status.isOK()) {
				rollback = false;

				for (IKommaTransaction transaction : startedTransactions) {
					if (transaction.isActive()) {
						transaction.commit();
					}
				}
			}
			return command.getCommandResult();
		} finally {
			change = internalDomain.getChangeRecorder().endRecording();

			if (rollback) {
				for (IKommaTransaction transaction : startedTransactions) {
					if (transaction.isActive()) {
						transaction.rollback();
					}
				}
			}
		}
	}

	/**
	 * Redoes the changes that I recorded. Subclasses would not normally need to
	 * override this method.
	 * 
	 * @throws IllegalStateException
	 *             if I am not {@linkplain #canRedo() redoable}
	 * 
	 * @see #canRedo()
	 */
	@Override
	public CommandResult doRedoWithResult(IProgressMonitor progressMonitor,
			IAdaptable info) throws ExecutionException {
		IStatus status = command.redo(progressMonitor, info);
		if (status.isOK() && change != null) {
			status = change.redo(progressMonitor, info);
		}

		return CommandResult.newCommandResult(status, command
				.getCommandResult().getReturnValue());
	}

	/**
	 * Undoes the changes that I recorded. Subclasses would not normally need to
	 * override this method.
	 * 
	 * @throws IllegalStateException
	 *             if I am not {@linkplain #canUndo() undoable}
	 * 
	 * @see #canUndo()
	 */
	@Override
	public CommandResult doUndoWithResult(IProgressMonitor progressMonitor,
			IAdaptable info) throws ExecutionException {
		IStatus status = command.undo(progressMonitor, info);
		if (status.isOK() && change != null) {
			status = change.undo(progressMonitor, info);
		}

		return CommandResult.newCommandResult(status, command
				.getCommandResult().getReturnValue());
	}

	@Override
	public Collection<?> getAffectedObjects() {
		return command.getAffectedObjects();
	}

	@Override
	public Collection<?> getAffectedResources(Object type) {
		return command.getAffectedResources(type);
	}

	@Override
	public String getDescription() {
		return command.getDescription();
	}

	@Override
	public String getLabel() {
		return command.getLabel();
	}

	/**
	 * Subclasses should override this if they have more preparation to do. By
	 * default, the result is just <code>true</code>.
	 */
	@Override
	protected boolean prepare() {
		return command.canExecute();
	}
}
