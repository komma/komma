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
 * $Id: IdentityCommand.java,v 1.3 2006/12/05 20:19:53 emerks Exp $
 */
package net.enilink.komma.common.command;

import java.util.Collection;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;

import net.enilink.komma.common.CommonPlugin;

/**
 * A command that always produces the same result.
 */
public class IdentityCommand extends AbstractCommand {
	/**
	 * An empty instance of this object.
	 */
	public static final IdentityCommand INSTANCE = new IdentityCommand();

	{
		// This ensures that these useless state variables at least reflect the
		// right value.
		//
		isPrepared = true;
		isExecutable = true;
	}

	/**
	 * Creates an empty instance.
	 */
	public IdentityCommand() {
		super();
	}

	/**
	 * Creates an instance with a result collection containing the given result
	 * object.
	 * 
	 * @param result
	 *            the one object in the result collection.
	 */
	public IdentityCommand(Object result) {
		setResult(CommandResult.newOKCommandResult(result));
	}

	/**
	 * Creates an instance with the given result collection.
	 * 
	 * @param result
	 *            the result collection.
	 */
	public IdentityCommand(Collection<?> result) {
		this((Object) result);
	}

	/**
	 * Creates an instance with the given label.
	 * 
	 * @param label
	 *            the label.
	 */
	public IdentityCommand(String label) {
		super(label);
		setResult(CommandResult.newOKCommandResult());
	}

	/**
	 * Creates an instance with the given label and a result collection
	 * containing the given result object.
	 * 
	 * @param label
	 *            the label.
	 * @param result
	 *            the one object in the result collection.
	 */
	public IdentityCommand(String label, Object result) {
		super(label);
		setResult(CommandResult.newOKCommandResult(result));
	}

	/**
	 * Creates an instance with the given label the result collection.
	 * 
	 * @param label
	 *            the label.
	 * @param result
	 *            the result collection.
	 */
	public IdentityCommand(String label, Collection<?> result) {
		this(label, (Object) result);
	}

	/**
	 * Creates an instance with the given label and description.
	 * 
	 * @param label
	 *            the label.
	 * @param description
	 *            the description.
	 */
	public IdentityCommand(String label, String description) {
		this(label);
		this.description = description;
	}

	/**
	 * Creates an instance with the given label, description, and a result
	 * collection containing the given result object.
	 * 
	 * @param label
	 *            the label.
	 * @param description
	 *            the description.
	 * @param result
	 *            the one object in the result collection.
	 */
	public IdentityCommand(String label, String description, Object result) {
		this(label, result);
		this.description = description;
	}

	/**
	 * Creates an instance with the given label, description, result collection.
	 * 
	 * @param label
	 *            the label.
	 * @param description
	 *            the description.
	 * @param result
	 *            the result collection.
	 */
	public IdentityCommand(String label, String description,
			Collection<?> result) {
		this(label, result);
		this.description = description;
	}

	/**
	 * Returns <code>true</code>.
	 * 
	 * @return <code>true</code>.
	 */
	@Override
	public boolean canExecute() {
		return true;
	}

	@Override
	public String getDescription() {
		return description == null ? CommonPlugin.INSTANCE
				.getString("_UI_IdentityCommand_description") : description;
	}

	@Override
	protected CommandResult doExecuteWithResult(
			IProgressMonitor progressMonitor, IAdaptable info)
			throws ExecutionException {
		return getCommandResult();
	}

	@Override
	protected CommandResult doRedoWithResult(IProgressMonitor progressMonitor,
			IAdaptable info) throws ExecutionException {
		return getCommandResult();
	}

	@Override
	protected CommandResult doUndoWithResult(IProgressMonitor progressMonitor,
			IAdaptable info) throws ExecutionException {
		return getCommandResult();
	}
}
