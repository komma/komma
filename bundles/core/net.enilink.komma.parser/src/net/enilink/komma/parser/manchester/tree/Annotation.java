package net.enilink.komma.parser.manchester.tree;

import net.enilink.komma.parser.sparql.tree.GraphNode;

public class Annotation {
	protected GraphNode predicate;
	protected GraphNode object;

	public Annotation(GraphNode predicate, GraphNode object) {
		this.predicate = predicate;
		this.object = object;
	}

	public GraphNode getPredicate() {
		return predicate;
	}

	public GraphNode getObject() {
		return object;
	}

	public Annotation copy() {
		return new Annotation(predicate.copy(true), object.copy(true));
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((object == null) ? 0 : object.hashCode());
		result = prime * result
				+ ((predicate == null) ? 0 : predicate.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Annotation))
			return false;
		Annotation other = (Annotation) obj;
		if (object == null) {
			if (other.object != null)
				return false;
		} else if (!object.equals(other.object))
			return false;
		if (predicate == null) {
			if (other.predicate != null)
				return false;
		} else if (!predicate.equals(other.predicate))
			return false;
		return true;
	}
}
