/******************************************************************************
 * Copyright (c) 2002, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *    IBM Corporation - initial API and implementation 
 ****************************************************************************/

package net.enilink.komma.edit.ui.action;

import org.eclipse.jface.action.IAction;

/**
 * Interface extension to <code>IAction</code> adding lifecycle methods.
 * 
 * @author melaasar
 */
public interface IDisposableAction extends IAction {

	/**
	 * init should be called after an action instance gets constructed 
	 */
	public void init();

	/**
	 * dispose should be called as soon as the action is no longer needed
	 */
	public void dispose();
	
	/**
	 * Answers whether or not this action has been disposed and has not
	 * been re-initialized.
	 * 
	 * @return <code>true</code> if the action has been disposed, 
	 * 	 	   <code>false</code> otherwise.
	 */
	public boolean isDisposed();
	
}
