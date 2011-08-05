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
 * $Id: AbstractOverrideableCommand.java,v 1.5 2008/05/07 19:08:46 emerks Exp $
 */
package net.enilink.komma.edit.command;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import net.enilink.komma.common.command.AbstractCommand;
import net.enilink.komma.common.command.CommandResult;
import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.model.IObject;
import net.enilink.komma.core.IReference;
import net.enilink.komma.util.Pair;

/**
 * This is a convenient base class for classes that implement
 * {@link IOverrideableCommand}. Subclasses of AbstractOverrideableCommand
 * should provide implementations of the doXxx methods (e.g., doExecute) from
 * OverrideableCommand instead of the base command methods (e.g., execute),
 * which have final implementations here.
 */
public abstract class AbstractOverrideableCommand extends AbstractCommand
		implements IOverrideableCommand {
	@SuppressWarnings("unchecked")
	public static Collection<Object> getOwnerList(IObject owner,
			IReference property) {
		Pair<Integer, Integer> cardinality = owner
				.getApplicableCardinality(property);
		if (cardinality.getSecond() != 1) {
			Object value = owner.get(property);
			if (value instanceof Collection<?>) {
				return (Collection<Object>) value;
			}
		}
		return null;
	}

	/**
	 * This is the editing domain in which this command operates.
	 */
	private IEditingDomain domain;

	/**
	 * This is the command that overrides this command.
	 */
	protected ICommand overrideCommand;

	/**
	 * This constructs an instance in this editing domain.
	 */
	protected AbstractOverrideableCommand(IEditingDomain domain) {
		this(domain, null, null);
	}

	/**
	 * This constructs an instance with the given label and in this editing
	 * domain.
	 */
	protected AbstractOverrideableCommand(IEditingDomain domain, String label) {
		this(domain, label, null);
	}

	/**
	 * This constructs an instance with the given label and description, in this
	 * editing domain.
	 */
	protected AbstractOverrideableCommand(IEditingDomain domain, String label,
			String description) {
		super(label, description);

		this.domain = domain;
	}

	@Override
	public final boolean canExecute() {
		if (getDomain() != null && !isPrepared) {
			ICommand newOverrideCommand = getDomain().createOverrideCommand(
					this);
			setOverride(newOverrideCommand);
		}

		boolean result = overrideCommand != null ? overrideCommand.canExecute()
				: doCanExecute();

		return result;
	}

	@Override
	public final boolean canRedo() {
		boolean result = overrideCommand != null ? overrideCommand.canRedo()
				: doCanRedo();

		return result;
	}

	@Override
	public final boolean canUndo() {
		boolean result = overrideCommand != null ? overrideCommand.canUndo()
				: doCanUndo();

		return result;
	}

	@Override
	public final void dispose() {
		if (overrideCommand != null) {
			overrideCommand.dispose();
		} else {
			doDispose();
		}
	}

	public boolean doCanExecute() {
		return super.canExecute();
	}

	public boolean doCanRedo() {
		return super.canRedo();
	}

	public boolean doCanUndo() {
		return super.canUndo();
	}

	public void doDispose() {
		super.dispose();
	}

	public IStatus doExecute(IProgressMonitor progressMonitor, IAdaptable info)
			throws ExecutionException {
		return super.execute(progressMonitor, info);
	}

	public Collection<?> doGetAffectedObjects() {
		return super.getAffectedObjects();
	}

	@Override
	public Collection<?> doGetAffectedResources(Object type) {
		return super.getAffectedResources(type);
	}

	public Collection<?> doGetChildrenToCopy() {
		return Collections.EMPTY_LIST;
	}

	public CommandResult doGetCommandResult() {
		return super.getCommandResult();
	}

	public String doGetDescription() {
		return super.getDescription();
	}

	public String doGetLabel() {
		return super.getLabel();
	}

	public IStatus doRedo(IProgressMonitor progressMonitor, IAdaptable info)
			throws ExecutionException {
		return super.redo(progressMonitor, info);
	}

	@Override
	protected CommandResult doRedoWithResult(IProgressMonitor progressMonitor,
			IAdaptable info) throws ExecutionException {
		return CommandResult.newOKCommandResult();
	}

	public IStatus doUndo(IProgressMonitor progressMonitor, IAdaptable info)
			throws ExecutionException {
		return super.undo(progressMonitor, info);
	}

	@Override
	protected CommandResult doUndoWithResult(IProgressMonitor progressMonitor,
			IAdaptable info) throws ExecutionException {
		return CommandResult.newOKCommandResult();
	}

	@Override
	public IStatus execute(IProgressMonitor progressMonitor, IAdaptable info)
			throws ExecutionException {
		if (overrideCommand != null) {
			return overrideCommand.execute(progressMonitor, info);
		} else {
			return doExecute(progressMonitor, info);
		}
	}

	@Override
	public final Collection<?> getAffectedObjects() {
		return overrideCommand != null ? overrideCommand.getAffectedObjects()
				: doGetAffectedObjects();
	}

	@Override
	public Collection<?> getAffectedResources(Object type) {
		return overrideCommand != null ? overrideCommand
				.getAffectedResources(type) : doGetAffectedResources(type);
	}

	public final Collection<?> getChildrenToCopy() {
		Collection<?> result = overrideCommand instanceof IChildrenToCopyProvider ? ((IChildrenToCopyProvider) overrideCommand)
				.getChildrenToCopy() : doGetChildrenToCopy();

		return result;
	}

	@Override
	public final CommandResult getCommandResult() {
		return overrideCommand != null ? overrideCommand.getCommandResult()
				: doGetCommandResult();
	}

	@Override
	public final String getDescription() {
		return overrideCommand != null ? overrideCommand.getDescription()
				: doGetDescription();
	}

	/**
	 * This returns the editing domain that contains this.
	 */
	public IEditingDomain getDomain() {
		return domain;
	}

	@Override
	public final String getLabel() {
		return overrideCommand != null ? overrideCommand.getLabel()
				: doGetLabel();
	}

	/**
	 * This returns the command that overrides this command.
	 */
	public ICommand getOverride() {
		return overrideCommand;
	}

	@Override
	public IStatus redo(IProgressMonitor progressMonitor, IAdaptable info)
			throws ExecutionException {
		if (overrideCommand != null) {
			return overrideCommand.redo(progressMonitor, info);
		} else {
			return doRedo(progressMonitor, info);
		}
	}

	/**
	 * This sets the command that overrides this command.
	 */
	public void setOverride(ICommand overrideCommand) {
		this.overrideCommand = overrideCommand;
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
		result.append(" (overrideCommand: " + overrideCommand + ")");
		return result.toString();
	}

	@Override
	public IStatus undo(IProgressMonitor progressMonitor, IAdaptable info)
			throws ExecutionException {
		if (overrideCommand != null) {
			return overrideCommand.undo(progressMonitor, info);
		} else {
			return doUndo(progressMonitor, info);
		}
	}
}
