package net.enilink.komma.parser.sparql;

import org.parboiled.Rule;

import net.enilink.komma.parser.sparql.tree.GraphPattern;
import net.enilink.komma.parser.sparql.tree.expr.GraphPatternExpr;

/**
 * SPARQL 1.1 Parser (incomplete)
 * 
 * <p>implemented featues:</p>
 * <ul>
 * <li>FILTER EXISTS + FILTER NOT EXISTS</li>
 * </ul>
 */
public class Sparql11Parser extends SparqlParser {
	@Override
	public Rule BuiltInCall() {
		boolean not = false;
		return FirstOf(
				Sequence(Optional("NOT", not = true), "EXISTS",
						GroupGraphPattern(), //
						push(new GraphPatternExpr(
								not ? GraphPatternExpr.Type.NOT_EXISTS
										: GraphPatternExpr.Type.EXISTS,
								(GraphPattern) pop())) //
				), //
				super.BuiltInCall());
	}
}
