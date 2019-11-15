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
 * An accumulator of notifications. As notifications are produced, they are
 * {@link #add accumulated} in a chain, and possibly even merged, before finally
 * being {@link #dispatch dispatched} to the notifier.
 */
public interface INotificationChain {
	/**
	 * Adds a notification to the chain.
	 * 
	 * @return whether the notification was added.
	 */
	boolean add(INotification notification);

	/**
	 * Dispatches each notification to the appropriate listeners.
	 */
	void dispatch();
}
