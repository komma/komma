package net.enilink.komma.edit.ui.properties.internal.parts;

import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;

public class StatementNode {
	protected boolean inverse;
	protected IStatement statement;

	public StatementNode(IStatement statement, boolean inverse) {
		this.statement = statement;
		this.inverse = inverse;
	}

	public IReference getResource() {
		if (getStatement() == null) {
			return null;
		}
		return inverse ? (IReference) getStatement().getObject()
				: getStatement().getSubject();
	}

	public IStatement getStatement() {
		return statement;
	}

	public Object getValue() {
		if (getStatement() == null) {
			return null;
		}
		return inverse ? (IReference) getStatement().getSubject()
				: getStatement().getObject();
	}

	public boolean isInverse() {
		return inverse;
	}
}