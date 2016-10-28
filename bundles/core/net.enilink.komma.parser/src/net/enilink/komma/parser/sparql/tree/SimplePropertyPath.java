package net.enilink.komma.parser.sparql.tree;

import net.enilink.komma.parser.sparql.tree.visitor.Visitor;

public class SimplePropertyPath implements PropertyPath {
	protected final String pathExpression;

	public SimplePropertyPath(String pathExpression) {
		this.pathExpression = pathExpression;
	}

	public String getPathExpression() {
		return pathExpression;
	}

	@Override
	public NodeOrPath copy(boolean copyProperties) {
		return this;
	}

	@Override
	public <R, T> R accept(Visitor<R, T> visitor, T data) {
		return visitor.simplePropertyPath(this, data);
	}
}