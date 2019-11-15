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
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;

public class NotificationSupport<T extends INotification> implements
		INotificationBroadcaster<T>, INotifier<T> {
	private CopyOnWriteArraySet<INotificationListener<T>> listeners = new CopyOnWriteArraySet<INotificationListener<T>>();

	/* (non-Javadoc)
	 * @see net.enilink.komma.common.notify.INotificationSupport#notify(java.util.Collection)
	 */
	public void fireNotifications(Collection<? extends T> notifications) {
		List<T> cache = new ArrayList<T>();
		for (INotificationListener<T> listener : listeners) {
			Collection<T> filtered = FilterUtil.select(notifications, listener
					.getFilter(), cache);

			if (!filtered.isEmpty()) {
				listener.notifyChanged(filtered);
			}
		}
	}

	/* (non-Javadoc)
	 * @see net.enilink.komma.common.notify.INotificationSupport#addListener(net.enilink.komma.common.notify.INotificationListener)
	 */
	public void addListener(INotificationListener<T> listener) {
		listeners.add(listener);
	}

	/* (non-Javadoc)
	 * @see net.enilink.komma.common.notify.INotificationSupport#removeListener(net.enilink.komma.common.notify.INotificationListener)
	 */
	public void removeListener(INotificationListener<T> listener) {
		listeners.remove(listener);
	}
}