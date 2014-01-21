/*
 * Copyright James Leigh (c) 2007.
 *
 * Licensed under the Aduna BSD-style license.
 */
package net.enilink.komma.dm.internal.change;

import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.Statement;
import net.enilink.komma.dm.IDataManager;
import net.enilink.komma.dm.change.IDataChange;
import net.enilink.komma.dm.change.IStatementChange;

/**
 * Command object for statements that are removed from the repository.
 * 
 */
public class RemoveChange extends Statement implements IDataChange,
		IStatementChange {
	public RemoveChange(IStatement stmt) {
		super(stmt.getSubject(), stmt.getPredicate(), stmt.getObject(), stmt
				.getContext(), stmt.isInferred());
	}

	protected IReference[] getModifyContexts() {
		IReference context = getContext();
		return context == null ? new IReference[0]
				: new IReference[] { context };
	}

	public void redo(IDataManager dm) {
		dm.remove(this, getModifyContexts());
	}

	@Override
	public String toString() {
		return new StringBuilder().append("statement removed ")
				.append(super.toString()).toString();
	}

	public void undo(IDataManager dm) {
		dm.add(this, getModifyContexts());
	}

	@Override
	public boolean isAdd() {
		return false;
	}
}
