/**
 * <copyright> 
 *
 * Copyright (c) 2002, 2009 IBM Corporation and others.
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
 * $Id: CopyToClipboardCommand.java,v 1.4 2006/12/28 06:48:55 marcelop Exp $
 */
package net.enilink.komma.edit.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;

import net.enilink.komma.common.command.AbstractCommand;
import net.enilink.komma.common.command.CommandResult;
import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.edit.KommaEditPlugin;
import net.enilink.komma.edit.domain.IEditingDomain;

/**
 * This works exactly like a {@link CopyCommand} but set the copy result to the
 * {@link IEditingDomain}. In fact, the implementation is just a proxy for copy
 * command.
 */
public class CopyToClipboardCommand extends AbstractOverrideableCommand
		implements AbstractCommand.INonDirtying {
	/**
	 * This creates a command that copies the given collection of objects to the
	 * clipboard.
	 */
	public static ICommand create(IEditingDomain domain,
			final Collection<?> collection) {
		if (domain == null) {
			CopyToClipboardCommand command = new CopyToClipboardCommand(domain,
					collection);
			return command;
		} else {
			ICommand command = domain.createCommand(
					CopyToClipboardCommand.class, new CommandParameter(null,
							null, collection));
			return command;
		}
	}

	/**
	 * This caches the label.
	 */
	protected static final String LABEL = KommaEditPlugin.INSTANCE
			.getString("_UI_CopyToClipboardCommand_label");

	/**
	 * This caches the description.
	 */
	protected static final String DESCRIPTION = KommaEditPlugin.INSTANCE
			.getString("_UI_CopyToClipboardCommand_description");

	/**
	 * This constructs a command that copies the given collection of objects to
	 * the clipboard.
	 */
	public CopyToClipboardCommand(IEditingDomain domain,
			Collection<?> collection) {
		super(domain, LABEL, DESCRIPTION);

		this.sourceObjects = collection;
	}

	/**
	 * This is the collection of objects to be copied to the clipboard.
	 */
	protected Collection<?> sourceObjects;

	/**
	 * This is the original clipboard value before execute.
	 */
	protected Collection<Object> oldClipboard;

	/**
	 * This is the command that does the actual copying.
	 */
	protected ICommand copyCommand;

	/**
	 * This creates a command that copies the given object to the clipboard.
	 */
	public static ICommand create(IEditingDomain domain, Object owner) {
		return create(domain, Collections.singleton(owner));
	}

	/**
	 * This returns the collection of objects to be copied to the clipboard.
	 */
	public Collection<?> getSourceObjects() {
		return sourceObjects;
	}

	@Override
	protected boolean prepare() {
		copyCommand = CopyCommand.create(getDomain(), sourceObjects, getDomain()
				.getClipboardModel());
		return copyCommand.canExecute();
	}

	@Override
	protected CommandResult doExecuteWithResult(
			IProgressMonitor progressMonitor, IAdaptable info)
			throws ExecutionException {
		getDomain().getClipboardModel().getManager().clear();
		copyCommand.execute(progressMonitor, info);

		oldClipboard = getDomain().getClipboard();
		getDomain().setClipboard(new ArrayList<Object>(new ArrayList<Object>(
				copyCommand.getCommandResult().getReturnValues())));

		return copyCommand.getCommandResult();
	}

	@Override
	protected CommandResult doUndoWithResult(IProgressMonitor progressMonitor,
			IAdaptable info) throws ExecutionException {
		copyCommand.undo(progressMonitor, info);

		getDomain().setClipboard(oldClipboard);

		return copyCommand.getCommandResult();
	}

	@Override
	protected CommandResult doRedoWithResult(IProgressMonitor progressMonitor,
			IAdaptable info) throws ExecutionException {
		copyCommand.redo(progressMonitor, info);

		oldClipboard = getDomain().getClipboard();
		getDomain().setClipboard(new ArrayList<Object>(copyCommand
				.getCommandResult().getReturnValues()));

		return copyCommand.getCommandResult();
	}

	@Override
	public Collection<?> doGetAffectedObjects() {
		return copyCommand.getAffectedObjects();
	}

	@Override
	public void doDispose() {
		if (copyCommand != null) {
			copyCommand.dispose();
		}
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
		result.append(" (sourceObjects: " + sourceObjects + ")");
		result.append(" (oldClipboard: " + oldClipboard + ")");

		return result.toString();
	}
}
