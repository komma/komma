/*
 * Copyright James Leigh (c) 2007.
 *
 * Licensed under the Aduna BSD-style license.
 */
package net.enilink.komma.dm.internal.change;

import java.util.Collections;

import net.enilink.komma.dm.IDataManager;
import net.enilink.komma.dm.change.IDataChange;
import net.enilink.komma.dm.change.IStatementChange;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IValue;
import net.enilink.komma.core.Statement;

/**
 * Internal command object representing a statement being added to the
 * connection.
 * 
 */
public class AddChange extends Statement implements IDataChange,
		IStatementChange {
	public AddChange(IReference subj, IReference pred, IValue obj,
			IReference ctx) {
		super(subj, pred, obj, ctx);
	}

	public void redo(IDataManager dm) {
		dm.setModifyContexts(Collections.singleton(getContext().getURI()));
		dm.add(this);
	}

	@Override
	public String toString() {
		return new StringBuilder().append("statement added ")
				.append(super.toString()).toString();
	}

	public void undo(IDataManager dm) {
		dm.setModifyContexts(Collections.singleton(getContext().getURI()));
		dm.remove(this);
	}

	@Override
	public boolean isAdd() {
		return true;
	}
}
