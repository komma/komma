/**
 * <copyright> 
 *
 * Copyright (c) 2002, 2010 IBM Corporation and others.
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
 * $Id: CompoundCommand.java,v 1.6 2007/06/12 20:56:17 emerks Exp $
 */
package net.enilink.komma.common.command;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.commands.operations.IUndoableOperation;

/**
 * A command that comprises a sequence of subcommands. Derived classes can
 * control the way results are accumulated from the individual commands; the
 * default behaviour is to return the result of the last command.
 */
public class ExtendedCompositeCommand extends CompositeCommand {
	/**
	 * When {@link #resultIndex} is set to this, {@link #getResult} and
	 * {@link #getAffectedObjects} are delegated to the last command, if any, in
	 * the list.
	 */
	public static final int LAST_COMMAND_ALL = Integer.MIN_VALUE;

	/**
	 * When {@link #resultIndex} is set to this, {@link #getResult} and
	 * {@link #getAffectedObjects} are set to the result of merging the
	 * corresponding collection of each command in the list.
	 */
	public static final int MERGE_COMMAND_ALL = Integer.MIN_VALUE - 1;

	/**
	 * The index of the command whose result and affected objects are forwarded.
	 * Negative values have special meaning, as defined by the static constants.
	 * A value of -1 indicates that the last command in the list should be used.
	 * We could have more special behaviours implemented for other negative
	 * values.
	 */
	protected int resultIndex = MERGE_COMMAND_ALL;

	/**
	 * Creates an empty instance.
	 */
	public ExtendedCompositeCommand() {
		super();
	}

	/**
	 * Creates an instance with the given label.
	 * 
	 * @param label
	 *            the label.
	 */
	public ExtendedCompositeCommand(String label) {
		super(label);
	}

	/**
	 * Creates an instance with the given label and description.
	 * 
	 * @param label
	 *            the label.
	 * @param description
	 *            the description.
	 */
	public ExtendedCompositeCommand(String label, String description) {
		super(label, description, null);
	}

	/**
	 * Creates instance with the given label and list.
	 * 
	 * @param label
	 *            the label.
	 * @param commandList
	 *            the list of commands.
	 */
	public ExtendedCompositeCommand(String label, List<ICommand> commandList) {
		super(label, commandList);
	}

	/**
	 * Creates an instance with the given label, description, and list.
	 * 
	 * @param label
	 *            the label.
	 * @param description
	 *            the description.
	 * @param commandList
	 *            the list of commands.
	 */
	public ExtendedCompositeCommand(String label, String description,
			List<ICommand> commandList) {
		super(label, description, commandList);
	}

	/**
	 * Creates an empty instance with the given result index.
	 * 
	 * @param resultIndex
	 *            the {@link #resultIndex}.
	 */
	public ExtendedCompositeCommand(int resultIndex) {
		super();
		this.resultIndex = resultIndex;
	}

	/**
	 * Creates an instance with the given result index and label.
	 * 
	 * @param resultIndex
	 *            the {@link #resultIndex}.
	 * @param label
	 *            the label.
	 */
	public ExtendedCompositeCommand(int resultIndex, String label) {
		super(label);
		this.resultIndex = resultIndex;
	}

	/**
	 * Creates an instance with the given result index, label, and description.
	 * 
	 * @param resultIndex
	 *            the {@link #resultIndex}.
	 * @param label
	 *            the label.
	 * @param description
	 *            the description.
	 */
	public ExtendedCompositeCommand(int resultIndex, String label,
			String description) {
		super(label, description, null);
		this.resultIndex = resultIndex;
	}

	/**
	 * Creates an instance with the given result index and list.
	 * 
	 * @param resultIndex
	 *            the {@link #resultIndex}.
	 * @param commandList
	 *            the list of commands.
	 */
	public ExtendedCompositeCommand(int resultIndex, List<ICommand> commandList) {
		super();
		this.resultIndex = resultIndex;
		getChildren().addAll(commandList);
	}

	/**
	 * Creates an instance with the given resultIndex, label, and list.
	 * 
	 * @param resultIndex
	 *            the {@link #resultIndex}.
	 * @param label
	 *            the label.
	 * @param commandList
	 *            the list of commands.
	 */
	public ExtendedCompositeCommand(int resultIndex, String label,
			List<ICommand> commandList) {
		super(label, null, commandList);
		this.resultIndex = resultIndex;
	}

	/**
	 * Creates an instance with the given result index, label, description, and
	 * list.
	 * 
	 * @param resultIndex
	 *            the {@link #resultIndex}.
	 * @param label
	 *            the label.
	 * @param description
	 *            the description.
	 * @param commandList
	 *            the list of commands.
	 */
	public ExtendedCompositeCommand(int resultIndex, String label,
			String description, List<ICommand> commandList) {
		super(label, description, commandList);
		this.resultIndex = resultIndex;
	}

	/**
	 * Returns an unmodifiable view of the commands in the list.
	 * 
	 * @return an unmodifiable view of the commands in the list.
	 */
	@SuppressWarnings("unchecked")
	public List<? extends ICommand> getCommandList() {
		return (List<? extends ICommand>) Collections
				.unmodifiableList(getChildren());
	}

	/**
	 * Returns the index of the command whose result and affected objects are
	 * forwarded. Negative values have special meaning, as defined by the static
	 * constants.
	 * 
	 * @return the index of the command whose result and affected objects are
	 *         forwarded.
	 * @see #LAST_COMMAND_ALL
	 * @see #MERGE_COMMAND_ALL
	 */
	public int getResultIndex() {
		return resultIndex;
	}

	/**
	 * Determines the result by composing the results of the commands in the
	 * list; this is affected by the setting of {@link #resultIndex}.
	 * 
	 * @return the result.
	 */
	@Override
	protected Object getReturnValues() {
		if (getChildren().isEmpty()) {
			return null;
		} else if (resultIndex == LAST_COMMAND_ALL) {
			IUndoableOperation command = getChildren().get(
					getChildren().size() - 1);
			if (command instanceof ICommand) {
				return ((ICommand) command).getCommandResult().getReturnValue();
			}
		} else if (resultIndex == MERGE_COMMAND_ALL) {
			return super.getReturnValues();
		} else if (resultIndex < getChildren().size()) {
			IUndoableOperation command = getChildren().get(resultIndex);
			if (command instanceof ICommand) {
				return ((ICommand) command).getCommandResult().getReturnValue();
			}
		}

		return null;
	}

	/**
	 * Determines the affected objects by composing the affected objects of the
	 * commands in the list; this is affected by the setting of
	 * {@link #resultIndex}.
	 * 
	 * @return the affected objects.
	 */
	@Override
	public Collection<?> getAffectedObjects() {
		if (getChildren().isEmpty()) {
			return Collections.EMPTY_LIST;
		} else if (resultIndex == LAST_COMMAND_ALL) {
			return ((ICommand) getChildren().get(getChildren().size() - 1))
					.getAffectedObjects();
		} else if (resultIndex == MERGE_COMMAND_ALL) {
			return super.getAffectedObjects();
		} else if (resultIndex < getChildren().size()) {
			return ((ICommand) getChildren().get(resultIndex))
					.getAffectedObjects();
		} else {
			return Collections.EMPTY_LIST;
		}
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer(super.toString());
		result.append(" (commandList: #" + getChildren().size() + ")");
		result.append(" (resultIndex: " + resultIndex + ")");

		return result.toString();
	}
}
