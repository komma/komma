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