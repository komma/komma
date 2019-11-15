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
package net.enilink.komma.model.event;

import net.enilink.komma.common.notify.INotification;
import net.enilink.komma.core.URI;

/**
 * Notification for a namespace change.
 * 
 * @author Ken Wenzel
 * 
 */
public class NamespaceNotification implements INamespaceNotification {
	private URI newNS;

	private URI oldNS;

	private String prefix;

	public NamespaceNotification(String prefix, URI oldNS, URI newNS) {
		this.prefix = prefix;
		this.oldNS = oldNS;
		this.newNS = newNS;
	}

	public URI getNewNS() {
		return newNS;
	}

	public URI getOldNS() {
		return oldNS;
	}

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
		return new StringBuilder().append("namespace ")
				.append(newNS == null ? "deleted" : "added")
				.append(" [prefix=").append(prefix).append(", oldNS=")
				.append(oldNS).append(", newNS=").append(newNS).append("]")
				.toString();
	}
}
