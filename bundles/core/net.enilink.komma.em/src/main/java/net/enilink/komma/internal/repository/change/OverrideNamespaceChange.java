/*
 * Copyright James Leigh (c) 2007.
 *
 * Licensed under the Aduna BSD-style license.
 */
package net.enilink.komma.internal.repository.change;

import org.openrdf.repository.RepositoryConnection;
import org.openrdf.store.StoreException;

import net.enilink.komma.common.notify.INotification;
import net.enilink.komma.model.event.INamespaceNotification;
import net.enilink.komma.repository.change.INamespaceChange;
import net.enilink.komma.repository.change.IRepositoryChange;

/**
 * Saves a single change to the namespace.
 * 
 * @author James Leigh
 * 
 */
class OverrideNamespaceChange implements INamespaceChange,
		INamespaceNotification, IRepositoryChange {
	private String newNS;

	private String oldNS;

	private String prefix;

	public OverrideNamespaceChange(String prefix, String oldNS, String newNS) {
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
	 * @see net.enilink.komma.repository.event.INamespaceChange#getPrefix()
	 */
	public String getPrefix() {
		return prefix;
	}

	@Override
	public Object getSubject() {
		return getPrefix();
	}

	@Override
	public boolean merge(INotification notification) {
		return false;
	}

	public void redo(RepositoryConnection conn) throws StoreException {
		if (newNS == null) {
			conn.removeNamespace(prefix);
		} else {
			conn.setNamespace(prefix, newNS);
		}
	}

	@Override
	public String toString() {
		return new StringBuilder().append("namespace ").append(
				newNS == null ? "deleted" : "added").append(" [prefix=")
				.append(prefix).append(", oldNS=").append(oldNS).append(
						", newNS=").append(newNS).append("]").toString();
	}

	public void undo(RepositoryConnection conn) throws StoreException {
		if (oldNS == null) {
			conn.removeNamespace(prefix);
		} else {
			conn.setNamespace(prefix, oldNS);
		}
	}
}
