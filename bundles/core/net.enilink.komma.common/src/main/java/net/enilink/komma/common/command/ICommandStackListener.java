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
 * $Id: CommandStackListener.java,v 1.3 2005/06/08 05:44:08 nickb Exp $
 */
package net.enilink.komma.common.command;

import java.util.EventObject;

/**
 * A listener to a {@link net.enilink.komma.common.command.ICommandStack}.
 */
public interface ICommandStackListener {
	/**
	 * Called when the {@link net.enilink.komma.common.command.ICommandStack}'s
	 * state has changed.
	 * 
	 * @param event
	 *            the event.
	 */
	void commandStackChanged(EventObject event);
}
