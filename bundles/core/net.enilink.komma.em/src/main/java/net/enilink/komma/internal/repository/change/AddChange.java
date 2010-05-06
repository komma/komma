/*
 * Copyright James Leigh (c) 2007.
 *
 * Licensed under the Aduna BSD-style license.
 */
package net.enilink.komma.internal.repository.change;

import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.store.StoreException;

import net.enilink.komma.repository.change.IRepositoryChange;

/**
 * Internal command object representing a statement being added to the
 * connection.
 * 
 * @author James Leigh
 * 
 */
class AddChange extends StatementChange implements IRepositoryChange {
	public AddChange(Resource subj, URI pred, Value obj, Resource... ctx) {
		super(subj, pred, obj, ctx);
	}

	public void redo(RepositoryConnection conn) throws StoreException {
		conn.add(getSubject(), getPredicate(), getObject(), getContexts());
	}

	@Override
	public String toString() {
		return new StringBuilder().append("statement added ").append(
				super.toString()).toString();
	}

	public void undo(RepositoryConnection conn) throws StoreException {
		conn.removeMatch(getSubject(), getPredicate(), getObject(),
				getContexts());
	}

	@Override
	public boolean isAdd() {
		return true;
	}
}
