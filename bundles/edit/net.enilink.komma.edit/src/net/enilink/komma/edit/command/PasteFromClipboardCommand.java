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
 * $Id: PasteFromClipboardCommand.java,v 1.4 2007/10/02 19:24:58 emerks Exp $
 */
package net.enilink.komma.edit.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import net.enilink.komma.common.command.CommandResult;
import net.enilink.komma.common.command.CommandWrapper;
import net.enilink.komma.common.command.CompositeCommand;
import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.edit.KommaEditPlugin;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IObject;

/**
 * This works exactly like an {@link AddCommand} but the things to be added are
 * copied from the {@link IEditingDomain} clipboard. If the copied clipboard
 * instance is of the same type as the original clipboard instance, the
 * clipboard instance is replaced by the copied instance and the original
 * instance is used for the add.
 */
public class PasteFromClipboardCommand extends AbstractOverrideableCommand {
	/**
	 * This creates a command to add copies from the clipboard to the specified
	 * feature of the owner.
	 */
	public static ICommand create(IEditingDomain domain, Object owner,
			Object feature) {
		return create(domain, owner, feature, CommandParameter.NO_INDEX);
	}

	/**
	 * This creates a command to add copies from the clipboard to the specified
	 * feature of the owner and at the given index.
	 */
	public static ICommand create(IEditingDomain domain, Object owner,
			Object feature, int index) {
		if (domain == null) {
			return new PasteFromClipboardCommand(domain, owner, feature, index);
		} else {
			ICommand command = domain.createCommand(
					PasteFromClipboardCommand.class, new CommandParameter(
							owner, feature, Collections.emptyList(), index));
			return command;
		}
	}

	/**
	 * This caches the label.
	 */
	protected static final String LABEL = KommaEditPlugin.INSTANCE
			.getString("_UI_PasteFromClipboardCommand_label");

	/**
	 * This caches the description.
	 */
	protected static final String DESCRIPTION = KommaEditPlugin.INSTANCE
			.getString("_UI_PasteFromClipboardCommand_description");

	/**
	 * This is the command that does the actual pasting.
	 */
	protected CompositeCommand command;

	/**
	 * This is object where the clipboard copy is pasted.
	 */
	protected Object owner;

	/**
	 * This is feature of the owner where the clipboard copy is pasted.
	 */
	protected Object property;

	/**
	 * This is index in the feature of the owner where the clipboard copy is
	 * pasted.
	 */
	protected int index;

	/**
	 * This constructs an instance from the domain, which provides access the
	 * clipboard collection via {@link IEditingDomain#getCommandStack}.
	 */
	public PasteFromClipboardCommand(IEditingDomain domain, Object owner,
			Object property, int index) {
		super(domain, LABEL, DESCRIPTION);

		this.owner = owner;
		this.property = property;
		this.index = index;
	}

	public Object getOwner() {
		return owner;
	}

	public Object getProperty() {
		return property;
	}

	public int getIndex() {
		return index;
	}

	@Override
	protected boolean prepare() {
		// Create a strict compound command to do a copy and then add the result
		command = new CompositeCommand();

		IModel targetModel = null;
		if (owner instanceof IObject) {
			targetModel = ((IObject) owner).getModel();
		} else if (owner instanceof IModel) {
			targetModel = (IModel) owner;
		}
		// Create a command to copy the clipboard.
		final ICommand copyCommand = CopyCommand.create(getDomain(), getDomain()
				.getClipboard(), targetModel);
		command.add(copyCommand);

		boolean result = command.canExecute();

		return result;
	}

	@Override
	protected CommandResult doExecuteWithResult(
			IProgressMonitor progressMonitor, IAdaptable info)
			throws ExecutionException {
		IStatus status = command.execute(progressMonitor, info);
		if (status.isOK()) {
			command.addAndExecute(new CommandWrapper() {
				@Override
				protected ICommand createCommand() {
					return AddCommand
							.create(
									getDomain(),
									owner,
									property,
									new ArrayList<Object>(
											((ICommand) PasteFromClipboardCommand.this.command
													.iterator().next())
													.getCommandResult()
													.getReturnValues()), index);
				}
			}, progressMonitor, info);
		}
		return command.getCommandResult();
	}

	@Override
	protected CommandResult doUndoWithResult(IProgressMonitor progressMonitor,
			IAdaptable info) throws ExecutionException {
		command.undo(progressMonitor, info);
		return command.getCommandResult();
	}

	@Override
	protected CommandResult doRedoWithResult(IProgressMonitor progressMonitor,
			IAdaptable info) throws ExecutionException {
		command.redo(progressMonitor, info);
		return command.getCommandResult();
	}

	@Override
	public Collection<?> doGetAffectedObjects() {
		return command.getAffectedObjects();
	}

	@Override
	public void doDispose() {
		if (command != null)
			command.dispose();
	}

	/**
	 * This gives an abbreviated name using this object's own class' name,
	 * without package qualification, followed by a space separated list of
	 * <tt>field:value</tt> pairs.
	 */
	@Override
	public String toString() {
		StringBuffer result = new StringBuffer(super.toString());
		result.append(" (domain: " + getDomain() + ")");

		return result.toString();
	}
}
