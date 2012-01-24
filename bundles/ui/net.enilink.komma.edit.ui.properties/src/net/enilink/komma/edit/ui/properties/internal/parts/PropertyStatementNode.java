package net.enilink.komma.edit.ui.properties.internal.parts;

import net.enilink.komma.core.IStatement;

public class PropertyStatementNode extends StatementNode {
	protected PropertyNode propertyNode;
	protected int index;

	public PropertyStatementNode(PropertyNode propertyNode, int index,
			IStatement statement, boolean inverse) {
		super(statement, inverse);
		this.propertyNode = propertyNode;
		this.index = index;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + index;
		result = prime * result
				+ ((propertyNode == null) ? 0 : propertyNode.hashCode());
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
		PropertyStatementNode other = (PropertyStatementNode) obj;
		if (index != other.index)
			return false;
		if (propertyNode == null) {
			if (other.propertyNode != null)
				return false;
		} else if (!propertyNode.equals(other.propertyNode))
			return false;
		return true;
	}
}
