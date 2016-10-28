package net.enilink.komma.parser.sparql;

import java.util.ArrayList;
import java.util.List;

import org.parboiled.Rule;
import org.parboiled.support.Var;

import net.enilink.komma.parser.sparql.tree.BNodePropertyList;
import net.enilink.komma.parser.sparql.tree.Graph;
import net.enilink.komma.parser.sparql.tree.GraphNode;
import net.enilink.komma.parser.sparql.tree.GraphPattern;
import net.enilink.komma.parser.sparql.tree.IriRef;
import net.enilink.komma.parser.sparql.tree.MinusGraph;
import net.enilink.komma.parser.sparql.tree.NodeOrPath;
import net.enilink.komma.parser.sparql.tree.PropertyList;
import net.enilink.komma.parser.sparql.tree.SimplePropertyPath;
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
	protected final Object MARKER = new Object();

	@Override
	public Rule BuiltInCall() {
		Var<Boolean> not = new Var<>(false);
		return firstOf(
				sequence(optional("NOT", not.set(true)), "EXISTS", GroupGraphPattern(), //
						push(new GraphPatternExpr(
								not.get() ? GraphPatternExpr.Type.NOT_EXISTS : GraphPatternExpr.Type.EXISTS,
								(GraphPattern) pop())) //
		), //
				super.BuiltInCall());
	}

	public Rule GraphNode() {
		return firstOf(VarOrTerm(), TriplesNodePath());
	}

	public Rule TriplesSameSubjectPath() {
		Var<GraphNode> subject = new Var<>();
		return firstOf(sequence(VarOrTerm(), subject.set((GraphNode) peek()), PropertyListPathNotEmpty(subject)),
				sequence(TriplesNodePath(), subject.set((GraphNode) peek()), PropertyListPath(subject)));
	}

	public Rule PropertyListPath(Var<GraphNode> subject) {
		return optional(PropertyListPathNotEmpty(subject));
	}

	@SuppressWarnings("unchecked")
	public Rule PropertyListPathNotEmpty(Var<GraphNode> subject) {
		Var<PropertyList> propertyList = new Var<>();
		return sequence(firstOf(VerbPath(), VerbSimple()), ObjectListPath(),
				propertyList.set(createPropertyList(subject.get())),
				addPropertyPatterns(propertyList.get(), (NodeOrPath) pop(1), (List<GraphNode>) pop()), //
				zeroOrMore(';', optional(firstOf(VerbPath(), VerbSimple()), ObjectListPath(),
						addPropertyPatterns(propertyList.get(), (NodeOrPath) pop(1), (List<GraphNode>) pop()))));
	}

	public Rule VerbPath() {
		return Path();
	}

	public Rule VerbSimple() {
		return Var();
	}

	@Override
	public Rule TriplesBlock() {
		Var<List<GraphNode>> nodes = new Var<>();
		return sequence(TriplesSameSubjectPath(), //
				nodes.set(new ArrayList<GraphNode>()), nodes.get().add((GraphNode) pop()), //
				optional('.', optional(TriplesBlock(), nodes.get().addAll((List<GraphNode>) pop()))), //
				push(nodes.get()));
	}

	public Rule GraphPatternNotTriples() {
		return firstOf(OptionalGraphPattern(), MinusGraphPattern(), GroupOrUnionGraphPattern(), GraphGraphPattern());
	}

	public Rule MinusGraphPattern() {
		return sequence("MINUS", GroupGraphPattern(), push(new MinusGraph((Graph) pop())));
	}

	public Rule TriplesNodePath() {
		return firstOf(CollectionPath(), BlankNodePropertyListPath());
	}

	public Rule BlankNodePropertyListPath() {
		Var<GraphNode> subject = new Var<>();
		return sequence('[', push(new BNodePropertyList()), subject.set((GraphNode) peek()),
				PropertyListPathNotEmpty(subject), ']');
	}

	public Rule CollectionPath() {
		return sequence('(', oneOrMore(GraphNodePath()), ')');
	}

	public Rule GraphNodePath() {
		return firstOf(VarOrTerm(), TriplesNodePath());
	}

	public Rule ObjectListPath() {
		return sequence(ObjectPath(),
				sequence(push(LIST_BEGIN), zeroOrMore(',', ObjectPath()), push(popList(GraphNode.class, 1))));
	}

	public Rule ObjectPath() {
		return GraphNodePath();
	}

	public Rule Path() {
		return sequence(push(MARKER), PathAlternative(), popUntilMarker(),
				push(new SimplePropertyPath(getContext().getMatch())));
	}

	public Rule PathAlternative() {
		return sequence(PathSequence(), zeroOrMore(sequence('|', PathSequence())));
	}

	public Rule PathSequence() {
		return sequence(PathEltOrInverse(), zeroOrMore(sequence('/', PathEltOrInverse())));
	}

	public Rule PathElt() {
		return sequence(PathPrimary(), optional(PathMod()));
	}

	public Rule PathMod() {
		return firstOf('?', '*', '+');
	}

	public Rule PathEltOrInverse() {
		return firstOf(sequence('^', PathElt()), PathElt());
	}

	public Rule PathPrimary() {
		return firstOf(IriRef(), sequence('a', push(new IriRef(RDF_TYPE))), sequence('!', PathNegatedPropertySet()),
				sequence('(', Path(), ')'));
	}

	public Rule PathNegatedPropertySet() {
		return firstOf('(', optional(PathOneInPropertySet(), zeroOrMore('|', PathOneInPropertySet())));
	}

	public Rule PathOneInPropertySet() {
		return firstOf(IriRef(), sequence('a', push(new IriRef(RDF_TYPE))),
				sequence('^', firstOf(IriRef(), sequence('a', push(new IriRef(RDF_TYPE))))));
	}

	/**
	 * Remove all elements from stack up to and including <code>MARKER</code>.
	 */
	public boolean popUntilMarker() {
		while (pop() != MARKER) {
			// remove all elements including MARKER
		}
		return true;
	}
}
