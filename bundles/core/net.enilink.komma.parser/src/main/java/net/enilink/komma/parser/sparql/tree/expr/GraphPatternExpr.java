package net.enilink.komma.parser.sparql.tree.expr;

import net.enilink.komma.parser.sparql.tree.GraphPattern;
import net.enilink.komma.parser.sparql.tree.visitor.Visitor;

public class GraphPatternExpr extends CallExpr {
	public enum Type {
		EXISTS, NOT_EXISTS
	}

	protected Type type;
	protected GraphPattern pattern;

	public GraphPatternExpr(Type type, GraphPattern pattern) {
		this.type = type;
		this.pattern = pattern;
	}

	public Type getType() {
		return type;
	}

	public GraphPattern getPattern() {
		return pattern;
	}

	@Override
	public <R, T> R accept(Visitor<R, T> visitor, T data) {
		return visitor.graphPatternExpr(this, data);
	}
}
