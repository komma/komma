/*******************************************************************************
 * Copyright (c) 2009, 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.dm.change;

import net.enilink.komma.dm.IDataManager;

public interface IDataChangeStack extends IDataChangeTracker {
	/**
	 * Flushes the entire stack and resets the save location to zero. This
	 * method might be called when performing "revert to saved".
	 */
	void flush();

	/**
	 * Returns the undo limit. The undo limit is the maximum number of atomic
	 * operations that the User can undo. <code>-1</code> is used to indicate no
	 * limit.
	 * 
	 * @return the undo limit
	 */
	int getUndoLimit();

	/**
	 * Returns true if the stack is dirty. The stack is dirty whenever the last
	 * executed or redone command is different than the command that was at the
	 * top of the undo stack when {@link #markSaveLocation()} was last called.
	 * 
	 * @return <code>true</code> if the stack is dirty
	 */
	boolean isDirty();

	/**
	 * Marks the last executed or redone Command as the point at which the
	 * changes were saved. Calculation of {@link #isDirty()} will be based on
	 * this checkpoint.
	 */
	void markSaveLocation();

	/**
	 * After this object has recorded changes to a {@link IDataManager}, it can
	 * be repeated with this method. This {@link IDataManager} does not have to
	 * be the same data manager that this object was attached to.
	 * 
	 * @param dm
	 */
	void redo(IDataManager dm);

	/**
	 * Sets the undo limit. The undo limit is the maximum number of atomic
	 * operations that the User can undo. <code>-1</code> is used to indicate no
	 * limit.
	 * 
	 * @param undoLimit
	 *            the undo limit
	 */
	void setUndoLimit(int undoLimit);

	/**
	 * After this object has recorded changes to a {@link IDataManager}, it can
	 * be reverted with this method. This {@link IDataManager} does not have to
	 * be the same data manager that this object was attached to.
	 * 
	 * @param dm
	 */
	void undo(IDataManager dm);

}