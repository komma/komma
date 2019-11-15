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

import java.util.List;

import javax.management.Notification;

/**
 * A managed list that dispatches feature change notification to a notifier.
 */
public interface INotifyingList<E> extends List<E> {
	/**
	 * Returns the notifier that manages this list.
	 * 
	 * @return the notifier of the list.
	 */
	public Object getNotifier();

	/**
	 * Returns the notifier's feature that this list represents.
	 * 
	 * @see Notification#getFeature
	 * @return the feature of the list.
	 */
	public Object getProperty();
}
