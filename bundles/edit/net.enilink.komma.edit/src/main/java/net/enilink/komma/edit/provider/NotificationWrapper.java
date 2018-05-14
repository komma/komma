/*******************************************************************************
 * Copyright (c) 2009 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.edit.provider;

import net.enilink.komma.common.notify.INotification;

public class NotificationWrapper implements INotificationWrapper {
	private Object subject;
	private INotification wrappedNotification;

	public NotificationWrapper(Object subject, INotification wrappedNotification) {
		this.subject = subject;
		this.wrappedNotification = wrappedNotification;
	}

	@Override
	public INotification getWrappedNotification() {
		return wrappedNotification;
	}

	@Override
	public Object getSubject() {
		return subject;
	}

	@Override
	public boolean merge(INotification notification) {
		return false;
	}

}
