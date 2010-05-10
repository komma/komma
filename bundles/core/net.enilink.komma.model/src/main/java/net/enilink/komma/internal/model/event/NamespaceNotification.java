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
package net.enilink.komma.internal.model.event;

import net.enilink.komma.common.notify.INotification;
import net.enilink.komma.model.event.INamespaceNotification;

/**
 * Notification for a namespace change.
 * 
 * @author Ken Wenzel
 * 
 */
public class NamespaceNotification implements INamespaceNotification {
	private String newNS;

	private String oldNS;

	private String prefix;

	public NamespaceNotification(String prefix, String oldNS, String newNS) {
		this.prefix = prefix;
		this.oldNS = oldNS;
		this.newNS = newNS;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.komma.repository.event.INamespaceChange#getNewNS()
	 */
	public String getNewNS() {
		return newNS;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.komma.repository.event.INamespaceChange#getOldNS()
	 */
	public String getOldNS() {
		return oldNS;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.enilink.komma.repository.event.INamespaceChange#getPrefix()
	 */
	public String getPrefix() {
		return prefix;
	}

	@Override
	public Object getSubject() {
		return prefix;
	}

	@Override
	public boolean merge(INotification notification) {
		return false;
	}

	@Override
	public String toString() {
		return new StringBuilder().append("namespace ").append(
				newNS == null ? "deleted" : "added").append(" [prefix=")
				.append(prefix).append(", oldNS=").append(oldNS).append(
						", newNS=").append(newNS).append("]").toString();
	}
}
