package net.enilink.komma.parser.sparql.tree;

import net.enilink.komma.parser.sparql.tree.visitor.Visitor;

public class MinusGraph extends Graph {
	protected Graph graph;

	public MinusGraph(Graph graph) {
		this.graph = graph;
	}

	public Graph getGraph() {
		return graph;
	}

	@Override
	public <R, T> R accept(Visitor<R, T> visitor, T data) {
		return visitor.minusGraph(this, data);
	}
}
