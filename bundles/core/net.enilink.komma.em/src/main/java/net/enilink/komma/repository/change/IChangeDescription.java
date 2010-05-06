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
package net.enilink.komma.repository.change;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;


/**
 * Description of an model change
 */
public interface IChangeDescription {
	IStatus undo(IProgressMonitor monitor, IAdaptable info);

	IStatus redo(IProgressMonitor monitor, IAdaptable info);

	/**
	 * Queries whether I can {@link IChangeDescription#undo() undo} my changes.
	 * 
	 * @return <code>true</code> if my changes can be undone; <code>false</code>
	 *         otherwise
	 */
	boolean canUndo();

	/**
	 * Queries whether I have no changes.
	 * 
	 * @return <code>true</code> if I have no changes (undoing me would have no
	 *         effect on anything); <code>false</code>, otherwise
	 */
	boolean isEmpty();
}
