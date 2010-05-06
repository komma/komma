package net.enilink.komma.edit.ui.properties.internal.parts;

import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;

public class PropertyNode {
	private IStatement statement;
	private boolean hasChildren;

	public PropertyNode(IStatement statement, boolean hasChildren) {
		this.statement = statement;
		this.hasChildren = hasChildren;
	}

	public boolean hasChildren() {
		return hasChildren
				|| (statement != null && statement.getObject() instanceof IReference);
	}

	public IStatement getStatement() {
		return statement;
	}

	public void setStatement(IStatement statement) {
		this.statement = statement;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (hasChildren ? 1231 : 1237);
		result = prime * result
				+ ((statement == null) ? 0 : statement.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PropertyNode other = (PropertyNode) obj;
		if (hasChildren != other.hasChildren)
			return false;
		if (statement == null) {
			if (other.statement != null)
				return false;
		} else if (!statement.equals(other.statement))
			return false;
		return true;
	}
}
