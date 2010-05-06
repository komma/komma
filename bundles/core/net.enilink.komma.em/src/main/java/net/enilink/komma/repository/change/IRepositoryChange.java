/*
 * Copyright James Leigh (c) 2007.
 *
 * Licensed under the Aduna BSD-style license.
 */
package net.enilink.komma.repository.change;

import org.openrdf.repository.RepositoryConnection;
import org.openrdf.store.StoreException;

/**
 * Implemented by internal objects used to track changes.
 * 
 * @author James Leigh
 * 
 */
public interface IRepositoryChange {
	void undo(RepositoryConnection conn) throws StoreException;

	void redo(RepositoryConnection conn) throws StoreException;
}
