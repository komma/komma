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
 * $Id: CutToClipboardCommand.java,v 1.4 2008/05/07 19:08:46 emerks Exp $
 */
package net.enilink.komma.edit.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;

import net.enilink.komma.common.command.CommandResult;
import net.enilink.komma.common.command.CommandWrapper;
import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.edit.KommaEditPlugin;
import net.enilink.komma.edit.domain.IEditingDomain;

/**
 * This works just like {@link RemoveCommand} but also sets the removed objects
 * to the {@link IEditingDomain}. In fact, the implementation is just a proxy
 * for remove command.
 */
public class CutToClipboardCommand extends CommandWrapper {
	/**
	 * This creates a command to remove an object and set it to the clipboard.
	 */
	public static ICommand create(IEditingDomain domain, Object value) {
		if (domain == null) {
			return new CutToClipboardCommand(domain, RemoveCommand.create(
					domain, value));
		} else {
			return domain.createCommand(CutToClipboardCommand.class,
					new CommandParameter(null, null, Collections
							.singleton(value)));
		}
	}

	/**
	 * This creates a command to remove a particular value from the specified
	 * feature of the owner and set it to the clipboard.
	 */
	public static ICommand create(IEditingDomain domain, Object owner,
			Object feature, Object value) {
		if (domain == null) {
			return new CutToClipboardCommand(domain, RemoveCommand.create(
					domain, owner, feature, value));
		} else {
			return domain.createCommand(CutToClipboardCommand.class,
					new CommandParameter(owner, feature, Collections
							.singleton(value)));
		}
	}

	/**
	 * This creates a command to remove multiple objects and set it to the
	 * clipboard.
	 */
	public static ICommand create(IEditingDomain domain,
			Collection<?> collection) {
		if (domain == null) {
			return new CutToClipboardCommand(domain, RemoveCommand.create(
					domain, collection));
		} else {
			return domain.createCommand(CutToClipboardCommand.class,
					new CommandParameter(null, null, collection));
		}
	}

	/**
	 * This creates a command to remove a collection of values from the
	 * specified feature of the owner and set it to the clipboard.
	 */
	public static ICommand create(IEditingDomain domain, Object owner,
			Object property, Collection<?> collection) {
		if (domain == null) {
			return new CutToClipboardCommand(domain, RemoveCommand.create(
					domain, owner, property, collection));
		} else {
			return domain.createCommand(CutToClipboardCommand.class,
					new CommandParameter(owner, property, collection));
		}
	}

	/**
	 * This caches the label.
	 */
	protected static final String LABEL = KommaEditPlugin.INSTANCE
			.getString("_UI_CutToClipboardCommand_label");

	/**
	 * This caches the description.
	 */
	protected static final String DESCRIPTION = KommaEditPlugin.INSTANCE
			.getString("_UI_CutToClipboardCommand_description");

	/**
	 * This is the editing domain in which this command operates.
	 */
	protected IEditingDomain domain;

	/**
	 * This is the original clipboard value before execute.
	 */
	protected Collection<Object> oldClipboard;

	/**
	 * This constructs an instance that yields the result of the given command
	 * as its clipboard.
	 */
	public CutToClipboardCommand(IEditingDomain domain, ICommand command) {
		super(LABEL, DESCRIPTION, command);

		this.domain = domain;
	}

	@Override
	protected CommandResult doExecuteWithResult(
			IProgressMonitor progressMonitor, IAdaptable info)
			throws ExecutionException {
		CommandResult result = super.doExecuteWithResult(progressMonitor, info);

		if (domain != null) {
			oldClipboard = domain.getClipboard();
			domain
					.setClipboard(new ArrayList<Object>(result
							.getReturnValues()));
		}

		return result;
	}

	@Override
	protected CommandResult doUndoWithResult(IProgressMonitor progressMonitor,
			IAdaptable info) throws ExecutionException {
		CommandResult result = super.doUndoWithResult(progressMonitor, info);

		if (domain != null) {
			domain.setClipboard(oldClipboard);
		}

		return result;
	}

	@Override
	protected CommandResult doRedoWithResult(IProgressMonitor progressMonitor,
			IAdaptable info) throws ExecutionException {
		CommandResult result = super.doRedoWithResult(progressMonitor, info);

		if (domain != null) {
			oldClipboard = domain.getClipboard();
			domain
					.setClipboard(new ArrayList<Object>(result
							.getReturnValues()));
		}

		return result;
	}

	/**
	 * This gives an abbreviated name using this object's own class' name,
	 * without package qualification, followed by a space separated list of
	 * <tt>field:value</tt> pairs.
	 */
	@Override
	public String toString() {
		String result = super.toString() + " (domain: " + domain + ")" +
				" (oldClipboard: " + oldClipboard + ")";

		return result;
	}
}
