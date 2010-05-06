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
 * $Id: EditingDomain.java,v 1.6 2007/03/22 01:44:14 davidms Exp $
 */
package net.enilink.komma.edit.domain;

import java.util.Collection;

import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.common.command.ICommandStack;
import net.enilink.komma.edit.command.CommandParameter;
import net.enilink.komma.edit.command.IOverrideableCommand;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.repository.change.ChangeRecorder;

/**
 * An editing domain manages a self-contained set of interrelated EMF models and
 * the {@link ICommand}s that modify them. The models are maintained in the form
 * of a {@link ResourceSet}. Commands that modify the model are typically
 * created through the domain and are executed using the {@link ICommandStack}.
 * An optional feature of an editing domain, which is used to implement mapping
 * domains, is the ability to override primitive commands, see
 * {@link IOverrideableCommand}.
 * 
 * <p>
 * The domain imposes a hierarchical structure upon the models via the results
 * of the {@link #getChildren getChildren} and {@link #getParent getParent}
 * methods. This is useful for implementing commands such as
 * {@link net.enilink.komma.edit.command.RemoveCommand}, which often needs to
 * deduce the parent from which to remove a particular object, and
 * {@link net.enilink.komma.edit.command.CopyCommand}, which often needs to deduce
 * all the children to copy recursively. This also meshes well with user
 * interfaces, which often present a model hierarchically, i.e., as a tree.
 */
public interface IEditingDomain {
	/**
	 * The clipboard
	 */
	String CLIPBOARD_URI = "clipboard:///";

	/**
	 * This returns the resource set within which all the created and loaded
	 * resources reside.
	 */
	IModelSet getModelSet();

	/**
	 * This creates a command of the type of the specified by the command class
	 * and acting upon the information specified in the given command parameter.
	 */
	ICommand createCommand(Class<? extends ICommand> commandClass,
			CommandParameter commandParameter);

	/**
	 * This creates an override for the given command.
	 */
	ICommand createOverrideCommand(IOverrideableCommand command);

	/**
	 * This returns a command queue for executing commands.
	 */
	ICommandStack getCommandStack();

	/**
	 * This returns a collection of objects describing the different children
	 * that can be added under the specified object. If a sibling is specified
	 * (non-null), the children should be as close to immediately following that
	 * sibling as possible.
	 */
	Collection<?> getNewChildDescriptors(Object object, Object sibling);

	/**
	 * This returns the clipboard of the editing domain.
	 */
	Collection<Object> getClipboard();

	/**
	 * This returns the clipboard model of the editing domain.
	 */
	IModel getClipboardModel();

	/**
	 * This returns the children of the object.
	 */
	Collection<?> getChildren(Object object);

	/**
	 * This returns the parent of the object.
	 */
	Object getParent(Object object);

	/**
	 * This sets the clipboard of the editing domain.
	 */
	void setClipboard(Collection<Object> clipboard);

	/**
	 * This returns whether the model is read only in editing domain.
	 */
	boolean isReadOnly(IModel model);

	/**
	 * Disposes of this editing domain and any resources that it has allocated.
	 * Editing domains must be disposed when they are no longer in use, but only
	 * by the client that created them (in case of sharing of editing domains).
	 * <p>
	 * <b>Note</b> that editing domains registered on the extension point may
	 * not be disposed.
	 * </p>
	 */
	void dispose();

	interface Internal extends IEditingDomain {
		/**
		 * Obtains the change recorder that I use to track changes in my model
		 * set.
		 * 
		 * @return my change recorder
		 */
		ChangeRecorder getChangeRecorder();
	}
}
