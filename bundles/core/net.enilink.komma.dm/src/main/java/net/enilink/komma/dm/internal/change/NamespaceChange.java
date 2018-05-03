/*
 * Copyright James Leigh (c) 2007.
 *
 * Licensed under the Aduna BSD-style license.
 */
package net.enilink.komma.dm.internal.change;

import net.enilink.komma.dm.IDataManager;
import net.enilink.komma.dm.change.IDataChange;
import net.enilink.komma.dm.change.INamespaceChange;
import net.enilink.komma.core.URI;

/**
 * Saves a single change to the namespace.
 * 
 */
public class NamespaceChange implements INamespaceChange, IDataChange {
	private URI newNS;

	private URI oldNS;

	private String prefix;

	public NamespaceChange(String prefix, URI oldNS, URI newNS) {
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

	public void redo(IDataManager dm) {
		if (newNS == null) {
			dm.removeNamespace(prefix);
		} else {
			dm.setNamespace(prefix, newNS);
		}
	}

	@Override
	public String toString() {
		return new StringBuilder().append("namespace ")
				.append(newNS == null ? "deleted" : "added")
				.append(" [prefix=").append(prefix).append(", oldNS=")
				.append(oldNS).append(", newNS=").append(newNS).append("]")
				.toString();
	}

	public void undo(IDataManager dm) {
		if (oldNS == null) {
			dm.removeNamespace(prefix);
		} else {
			dm.setNamespace(prefix, oldNS);
		}
	}
}
