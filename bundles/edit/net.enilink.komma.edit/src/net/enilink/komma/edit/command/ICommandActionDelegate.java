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
 * $Id: CommandActionDelegate.java,v 1.2 2005/06/08 06:17:05 nickb Exp $
 */
package net.enilink.komma.edit.command;

/**
 * This is the interface used by a CommandAction to delegate all of the set
 * methods of an IAction to a {@link net.enilink.komma.common.command.ICommand}.
 */
public interface ICommandActionDelegate {
	/**
	 * This returns whether the action should be enabled.
	 */
	public boolean canExecute();

	/**
	 * This returns the decoration, if any, of the action.
	 */
	public Object getImage();

	/**
	 * This returns the menu text, if any, of the action.
	 */
	public String getText();

	/**
	 * This returns the description, if any, of the action.
	 */
	public String getDescription();

	/**
	 * This returns the tool tip text, if any, of the action.
	 */
	public String getToolTipText();
}
