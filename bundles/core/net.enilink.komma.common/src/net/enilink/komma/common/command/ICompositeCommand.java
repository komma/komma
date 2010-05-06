/*******************************************************************************
 * Copyright (c) 2009, 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.common.command;

import java.util.Iterator;
import java.util.ListIterator;

import org.eclipse.core.commands.operations.ICompositeOperation;
import org.eclipse.core.commands.operations.IUndoableOperation;

/**
 * A self-composing undoable operation that is has a {@link CommandResult}, a
 * list of affected {@link IFile}s, and is composed of child operations.
 * <P>
 * Does not extend <code>ICompositeOperation</code> because
 * <UL>
 * <LI> {@link #remove(IUndoableOperation)} does not dispose the removed
 * operation</LI>
 * <LI>Children are explicitely composed by the client. Adding to an open
 * composite through the operation history is not supported.</LI>
 * </UL>
 * 
 * @author ldamus
 */
public interface ICompositeCommand extends ICommand {

	/**
	 * <p>
	 * Add the specified operation as a child of this operation.
	 * </p>
	 * 
	 * @param operation
	 *            the operation to be added. If the operation instance has
	 *            already been added, this method will have no effect.
	 */
	public abstract void add(IUndoableOperation operation);

	/**
	 * <p>
	 * Remove the specified operation from this operation.
	 * </p>
	 * <p>
	 * Unlike {@link ICompositeOperation}, this does not dispose of the removed
	 * operation since the composite did not create the operation.
	 * </p>
	 * 
	 * @param operation
	 *            the operation to be removed. The operation should be disposed
	 *            by the receiver. This method will have no effect if the
	 *            operation instance is not already a child.
	 */
	public abstract void remove(IUndoableOperation operation);

	/**
	 * Answers whether or not this composite operation has children.
	 * 
	 * @return <code>true</code> if the operation does not have children,
	 *         <code>false</code> otherwise.
	 */
	public abstract boolean isEmpty();

	/**
	 * Queries the number of child operations that I contain.
	 * 
	 * @return my size
	 */
	public abstract int size();

	/**
	 * Obtains an iterator to traverse my child operations. Removing children
	 * via this iterator correctly maintains my undo contexts.
	 * 
	 * @return an iterator of my children
	 */
	public abstract Iterator<IUndoableOperation> iterator();

	/**
	 * Obtains an iterator to traverse my child operations in either direction.
	 * Adding and removing children via this iterator correctly maintains my
	 * undo contexts.
	 * <p>
	 * <b>Note</b> that, unlike list iterators generally, this one does not
	 * permit the addition of an operation that I already contain (the composite
	 * does not permit duplicates). Moreover, only {@link IUndoableOperation}s
	 * may be added, otherwise <code>ClassCastException</code>s will result.
	 * </p>
	 * 
	 * @return an iterator of my children
	 */
	public abstract ListIterator<IUndoableOperation> listIterator();

	/**
	 * Obtains an iterator to traverse my child operations in either direction,
	 * starting from the specified <code>index</code>. Adding and removing
	 * children via this iterator correctly maintains my undo contexts.
	 * <p>
	 * <b>Note</b> that, unlike list iterators generally, this one does not
	 * permit the addition of an operation that I already contain (the composite
	 * does not permit duplicates). Moreover, only {@link IUndoableOperation}s
	 * may be added, otherwise <code>ClassCastException</code>s will result.
	 * </p>
	 * 
	 * @param index
	 *            the index in my children at which to start iterating
	 * 
	 * @return an iterator of my children
	 */
	public abstract ListIterator<IUndoableOperation> listIterator(int index);
}
