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
package net.enilink.komma.common.notify;

/**
 * A general interface for notifications within KOMMA
 */
public interface INotification {
	/**
	 * Returns the object affected by the change.
	 * 
	 * @return the object affected by the change.
	 */
	Object getSubject();

	/**
	 * Returns whether the notification can be and has been merged with this
	 * one.
	 * 
	 * @return whether the notification can be and has been merged with this
	 *         one.
	 */
	boolean merge(INotification notification);
}