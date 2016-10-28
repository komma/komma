package net.enilink.komma.parser.sparql.tree;

import net.enilink.komma.parser.sparql.tree.visitor.Visitable;

/**
 * Represents either a {@link GraphNode} or a {@link PropertyPath}.
 */
public interface NodeOrPath extends Visitable {
	NodeOrPath copy(boolean copyProperties);
}
