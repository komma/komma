/*
 * Copyright James Leigh (c) 2007.
 *
 * Licensed under the Aduna BSD-style license.
 */
package net.enilink.komma.dm.internal.change;

import net.enilink.komma.dm.IDataManager;
import net.enilink.komma.dm.change.IDataChange;
import net.enilink.komma.dm.change.IStatementChange;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IValue;
import net.enilink.komma.core.Statement;

/**
 * Command object for statements that are removed from the repository.
 * 
 */
public class RemoveChange extends Statement implements IDataChange,
		IStatementChange {
	public RemoveChange(IReference subj, IReference pred, IValue obj,
			IReference ctx) {
		super(subj, pred, obj, ctx);
	}

	public void redo(IDataManager dm) {
		dm.remove(this);
	}

	@Override
	public String toString() {
		return new StringBuilder().append("statement removed ")
				.append(super.toString()).toString();
	}

	public void undo(IDataManager dm) {
		dm.add(this);
	}

	@Override
	public boolean isAdd() {
		return false;
	}
}
