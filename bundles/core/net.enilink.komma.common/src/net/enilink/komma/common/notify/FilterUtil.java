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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * An object that manages the filtering of notifications. This class can
 * implement optimizations to reduce the effort of filtering notification lists
 * for listeners that have similar filters.
 * 
 * @author Christian W. Damus (cdamus)
 */
public final class FilterUtil {
	/**
	 * Not instantiable by clients.
	 */
	private FilterUtil() {
	}

	/**
	 * Selects the notifications in the given list that match the specified
	 * filter.
	 * 
	 * @param notifications
	 *            a list of notifications to select from
	 * @param filter
	 *            a notification filter
	 * @param cache
	 *            A cache list that is precisely the same size as the
	 *            notifications list but is used and reused as a scratch pad.
	 *            Its purpose is to cut down the number of objects created and
	 *            garbage collected while propagating filtered events to a group
	 *            of listeners. Note that it will be repeatedly cleared and
	 *            populated each time it is given to this method.
	 * 
	 * @return the notifications that match the filter
	 * 
	 * @see #selectSingle(List, NotificationFilter)
	 */
	@SuppressWarnings("unchecked")
	public static <T extends INotification> Collection<T> select(
			Collection<? extends T> notifications,
			NotificationFilter<? super T> filter, Collection<T> cache) {
		Collection<T> result;

		if (filter == NotificationFilter.any()) {
			result = (Collection<T>) notifications;
		} else {
			result = cache;
			result.clear();

			if (filter == null) {
				// the default filter
				filter = NotificationFilter.any();
			}

			for (T next : notifications) {
				if (filter.accept(next)) {
					result.add(next);
				}
			}
		}

		return result;
	}

	/**
	 * Selects the notifications in the given list that match the specified
	 * filter.
	 * <p>
	 * For unbatched notifications, it is better to use the
	 * {@link #selectSingle(List, NotificationFilter)} method.
	 * </p>
	 * 
	 * @param notifications
	 *            a list of notifications to select from
	 * @param filter
	 *            a notification filter
	 * 
	 * @return the notifications that match the filter
	 * 
	 * @see #selectSingle(List, NotificationFilter)
	 */
	public static <T extends INotification> Collection<T> select(
			Collection<? extends T> notifications,
			NotificationFilter<? super T> filter) {
		return select(notifications, filter, new ArrayList<T>());
	}
}
