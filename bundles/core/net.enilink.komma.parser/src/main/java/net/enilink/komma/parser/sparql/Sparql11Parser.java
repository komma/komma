package net.enilink.komma.parser.sparql;

import org.parboiled.Rule;
import org.parboiled.support.Var;

import net.enilink.komma.parser.sparql.tree.Graph;
import net.enilink.komma.parser.sparql.tree.GraphPattern;
import net.enilink.komma.parser.sparql.tree.MinusGraph;
import net.enilink.komma.parser.sparql.tree.expr.GraphPatternExpr;

/**
 * SPARQL 1.1 Parser (incomplete)
 * 
 * <p>
 * implemented features:
 * </p>
 * <ul>
 * <li>FILTER EXISTS + FILTER NOT EXISTS</li>
 * <li>MINUS</li>
 * </ul>
 */
public class Sparql11Parser extends SparqlParser {
	@Override
	public Rule BuiltInCall() {
		Var<Boolean> not = new Var<>(false);
		return FirstOf(
				Sequence(Optional("NOT", not.set(true)), "EXISTS",
						GroupGraphPattern(), //
						push(new GraphPatternExpr(
								not.get() ? GraphPatternExpr.Type.NOT_EXISTS
										: GraphPatternExpr.Type.EXISTS,
								(GraphPattern) pop())) //
				), //
				super.BuiltInCall());
	}

	public Rule GraphPatternNotTriples() {
		return FirstOf(OptionalGraphPattern(), MinusGraphPattern(),
				GroupOrUnionGraphPattern(), GraphGraphPattern());
	}

	public Rule MinusGraphPattern() {
		return Sequence("MINUS", GroupGraphPattern(), push(new MinusGraph(
				(Graph) pop())));
	}
}
