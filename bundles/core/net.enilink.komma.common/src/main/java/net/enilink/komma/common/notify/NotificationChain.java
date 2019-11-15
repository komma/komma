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

import java.util.ArrayList;

/**
 * A list that acts as a notification chain.
 */
public class NotificationChain extends ArrayList<INotification> implements
		INotificationChain {
	private static final long serialVersionUID = 1L;

	protected INotificationBroadcaster<? super INotification> broadcaster;

	/**
	 * Creates an empty instance.
	 */
	public NotificationChain(
			INotificationBroadcaster<? super INotification> broadcaster) {
		this.broadcaster = broadcaster;
	}

	/**
	 * Creates an empty instance with a given capacity.
	 * 
	 * @param initialCapacity
	 *            the initial capacity of the list before it must grow.
	 */
	public NotificationChain(
			INotificationBroadcaster<? super INotification> broadcaster,
			int initialCapacity) {
		super(initialCapacity);
		this.broadcaster = broadcaster;
	}

	/**
	 * Adds or merges a new notification.
	 * 
	 * @param newNotification
	 *            a notification.
	 * @return <code>true</code> when the notification is added and
	 *         <code>false</code> when it is merged.
	 */
	@Override
	public boolean add(INotification newNotification) {
		if (newNotification == null) {
			return false;
		} else {
			for (INotification notification : this) {
				if (notification.merge(newNotification)) {
					return false;
				}
			}

			return super.add(newNotification);
		}
	}

	public void dispatch() {
		if (broadcaster != null) {
			broadcaster.fireNotifications(this);
		}
	}
}
