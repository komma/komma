/*
 * Copyright James Leigh (c) 2007.
 *
 * Licensed under the Aduna BSD-style license.
 */
package net.enilink.komma.dm.internal.change;

import java.util.Collections;

import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.dm.IDataManager;
import net.enilink.komma.dm.change.IDataChange;
import net.enilink.komma.dm.change.IStatementChange;

/**
 * Command object for statements that are removed from the repository.
 * 
 */
public class RemoveChange implements IDataChange, IStatementChange {
	protected final IStatement stmt;

	public RemoveChange(IStatement stmt) {
		this.stmt = stmt;
	}

	protected IReference[] getModifyContexts() {
		IReference context = stmt.getContext();
		return context == null ? new IReference[0]
				: new IReference[] { context };
	}

	@Override
	public IStatement getStatement() {
		return stmt;
	}

	public void redo(IDataManager dm) {
		dm.remove(Collections.singleton(stmt), getModifyContexts());
	}

	@Override
	public String toString() {
		return new StringBuilder().append("statement removed ")
				.append(super.toString()).toString();
	}

	public void undo(IDataManager dm) {
		dm.add(Collections.singleton(stmt), getModifyContexts());
	}

	@Override
	public boolean isAdd() {
		return false;
	}
}
