/*******************************************************************************
 * Copyright (c) 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.parser.sparql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.parboiled.Rule;
import org.parboiled.annotations.Var;

import net.enilink.komma.parser.BaseRdfParser;
import net.enilink.komma.parser.sparql.tree.AbstractGraphNode;
import net.enilink.komma.parser.sparql.tree.AskQuery;
import net.enilink.komma.parser.sparql.tree.BNode;
import net.enilink.komma.parser.sparql.tree.BNodePropertyList;
import net.enilink.komma.parser.sparql.tree.BasicGraphPattern;
import net.enilink.komma.parser.sparql.tree.Collection;
import net.enilink.komma.parser.sparql.tree.ConstructQuery;
import net.enilink.komma.parser.sparql.tree.Dataset;
import net.enilink.komma.parser.sparql.tree.DescribeQuery;
import net.enilink.komma.parser.sparql.tree.Graph;
import net.enilink.komma.parser.sparql.tree.GraphNode;
import net.enilink.komma.parser.sparql.tree.GraphPattern;
import net.enilink.komma.parser.sparql.tree.IntegerLiteral;
import net.enilink.komma.parser.sparql.tree.IriRef;
import net.enilink.komma.parser.sparql.tree.LimitModifier;
import net.enilink.komma.parser.sparql.tree.NamedGraph;
import net.enilink.komma.parser.sparql.tree.NumericLiteral;
import net.enilink.komma.parser.sparql.tree.OffsetModifier;
import net.enilink.komma.parser.sparql.tree.OptionalGraph;
import net.enilink.komma.parser.sparql.tree.OrderCondition;
import net.enilink.komma.parser.sparql.tree.OrderModifier;
import net.enilink.komma.parser.sparql.tree.PrefixDecl;
import net.enilink.komma.parser.sparql.tree.Prologue;
import net.enilink.komma.parser.sparql.tree.PropertyList;
import net.enilink.komma.parser.sparql.tree.PropertyPattern;
import net.enilink.komma.parser.sparql.tree.Query;
import net.enilink.komma.parser.sparql.tree.SelectQuery;
import net.enilink.komma.parser.sparql.tree.SolutionModifier;
import net.enilink.komma.parser.sparql.tree.UnionGraph;
import net.enilink.komma.parser.sparql.tree.Variable;
import net.enilink.komma.parser.sparql.tree.expr.BuiltInCall;
import net.enilink.komma.parser.sparql.tree.expr.Expression;
import net.enilink.komma.parser.sparql.tree.expr.FunctionCall;
import net.enilink.komma.parser.sparql.tree.expr.LogicalExpr;
import net.enilink.komma.parser.sparql.tree.expr.LogicalOperator;
import net.enilink.komma.parser.sparql.tree.expr.NegateExpr;
import net.enilink.komma.parser.sparql.tree.expr.NumericExpr;
import net.enilink.komma.parser.sparql.tree.expr.NumericOperator;
import net.enilink.komma.parser.sparql.tree.expr.RelationalExpr;
import net.enilink.komma.parser.sparql.tree.expr.RelationalOperator;

/**
 * SPARQL Parser
 * 
 * @author Ken Wenzel
 */
public class SparqlParser extends BaseRdfParser {
	public static String RDF_NAMESPACE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	public static String RDF_TYPE = RDF_NAMESPACE + "type";
	public static String RDF_NIL = RDF_NAMESPACE + "nil";

	public Rule Query() {
		return Sequence(
				WS(),
				Prologue(),
				FirstOf(SelectQuery(), ConstructQuery(), DescribeQuery(),
						AskQuery()),
				setPrologue((Prologue) pop(1), (Query) peek()), EOI);
	}

	protected boolean setPrologue(Prologue prologue, Query query) {
		if (query != null) {
			query.setPrologue(prologue);
		}
		return true;
	}

	public Rule Prologue() {
		IriRef base;
		List<PrefixDecl> prefixes;
		return Sequence(prefixes = new ArrayList<PrefixDecl>(),
				Optional(BaseDecl(), base = (IriRef) pop()),
				ZeroOrMore(PrefixDecl(), prefixes.add((PrefixDecl) pop())), //
				push(new Prologue(base, prefixes)));
	}

	public Rule BaseDecl() {
		return Sequence("BASE", IRI_REF());
	}

	public Rule PrefixDecl() {
		return Sequence("PREFIX", PNAME_NS(), IRI_REF(), //
				push(new PrefixDecl((String) pop(1), (IriRef) pop())));
	}

	@SuppressWarnings("unchecked")
	public Rule SelectQuery() {
		SelectQuery.Modifier modifier;
		List<Variable> projection;
		Dataset dataset;
		return Sequence(
				"SELECT",
				Optional(FirstOf(
						//
						Sequence("DISTINCT",
								modifier = SelectQuery.Modifier.DISTINCT),
						Sequence("REDUCED",
								modifier = SelectQuery.Modifier.REDUCED))
				//
				), push(LIST_BEGIN),
				FirstOf(OneOrMore(Var()), '*'),
				projection = popList(Variable.class), //
				dataset = new Dataset(), ZeroOrMore(DatasetClause(dataset)),
				WhereClause(), SolutionModifier(), //
				push(new SelectQuery(modifier, projection, dataset,
						(Graph) pop(1), (List<SolutionModifier>) pop())) //
		);
	}

	@SuppressWarnings("unchecked")
	public Rule ConstructQuery() {
		Dataset dataset;
		return Sequence("CONSTRUCT", ConstructTemplate(),
				dataset = new Dataset(), ZeroOrMore(DatasetClause(dataset)),
				WhereClause(), SolutionModifier(), //
				push(new ConstructQuery((List<GraphNode>) pop(2), dataset,
						(Graph) pop(1), (List<SolutionModifier>) pop())) //
		);
	}

	@SuppressWarnings("unchecked")
	public Rule DescribeQuery() {
		List<GraphNode> subjects;
		Dataset dataset;
		Graph where;
		return Sequence("DESCRIBE", push(LIST_BEGIN),
				FirstOf(OneOrMore(VarOrIRIref()), '*'),
				subjects = popList(GraphNode.class), dataset = new Dataset(),
				ZeroOrMore(DatasetClause(dataset)),
				Optional(WhereClause(), where = (Graph) pop()),
				SolutionModifier(), //
				push(new DescribeQuery(subjects, dataset, where,
						(List<SolutionModifier>) pop())) //
		);
	}

	public Rule AskQuery() {
		Dataset dataset;
		return Sequence("ASK", dataset = new Dataset(),
				ZeroOrMore(DatasetClause(dataset)), WhereClause(), //
				push(new AskQuery(dataset, (Graph) pop())) //
		);
	}

	public Rule DatasetClause(@Var Dataset dataset) {
		return Sequence(
				"FROM",
				FirstOf(//
				Sequence(DefaultGraphClause(),
						dataset.addDefaultGraph((Expression) pop())), //
						Sequence(NamedGraphClause(),
								dataset.addNamedGraph((Expression) pop()))));
	}

	public Rule DefaultGraphClause() {
		return SourceSelector();
	}

	public Rule NamedGraphClause() {
		return Sequence("NAMED", SourceSelector());
	}

	public Rule SourceSelector() {
		return IriRef();
	}

	public Rule WhereClause() {
		return Sequence(Optional("WHERE"), GroupGraphPattern());
	}

	@SuppressWarnings("unchecked")
	public Rule SolutionModifier() {
		List<SolutionModifier> modifiers;
		return Sequence(
				modifiers = new ArrayList<SolutionModifier>(),
				Optional(OrderClause(), modifiers.add((SolutionModifier) pop())),
				Optional(LimitOffsetClauses(),
						modifiers.addAll((List<SolutionModifier>) pop())),
				push(modifiers));
	}

	public Rule LimitOffsetClauses() {
		return FirstOf(
				//
				Sequence(push(LIST_BEGIN), LimitClause(),
						Optional(OffsetClause()),
						push(popList(SolutionModifier.class))), //
				Sequence(push(LIST_BEGIN), OffsetClause(),
						Optional(LimitClause()),
						push(popList(SolutionModifier.class))));
	}

	public Rule OrderClause() {
		return Sequence("ORDER", "BY", push(LIST_BEGIN),
				OneOrMore(OrderCondition()), push(new OrderModifier(
						popList(OrderCondition.class))));
	}

	public Rule OrderCondition() {
		return FirstOf(
				Sequence(
						FirstOf("ASC", "DESC"),
						push(match()),
						BrackettedExpression(),
						push(new OrderCondition("asc".equals(((String) pop(1))
								.toLowerCase()) ? OrderCondition.Direction.ASC
								: OrderCondition.Direction.DESC,
								(Expression) pop()))), //
				Sequence(FirstOf(Constraint(), Var()), push(new OrderCondition(
						OrderCondition.Direction.ASC, (Expression) pop()))));
	}

	public Rule LimitClause() {
		return Sequence("LIMIT", INTEGER(), //
				push(new LimitModifier(((IntegerLiteral) pop()).getValue())) //
		);
	}

	public Rule OffsetClause() {
		return Sequence("OFFSET", INTEGER(), //
				push(new OffsetModifier(((IntegerLiteral) pop()).getValue())) //
		);
	}

	@SuppressWarnings("unchecked")
	public Rule GroupGraphPattern() {
		List<Graph> patterns;
		List<Expression> filters;
		return Sequence(
				'{',
				patterns = new ArrayList<Graph>(),
				filters = new ArrayList<Expression>(),
				Optional(TriplesBlock(), //
						patterns.add(new BasicGraphPattern(
								(List<GraphNode>) pop()))),
				ZeroOrMore(
						FirstOf(Sequence(GraphPatternNotTriples(),
								patterns.add((Graph) pop())),
								Sequence(Filter(),
										filters.add((Expression) pop()))),
						Optional('.'),
						Optional(TriplesBlock(), //
								patterns.add(new BasicGraphPattern(
										(List<GraphNode>) pop())))), '}', //
				push(new GraphPattern(patterns, filters)));
	}

	@SuppressWarnings("unchecked")
	public Rule TriplesBlock() {
		List<GraphNode> nodes;
		return Sequence(
				TriplesSameSubject(), //
				nodes = new ArrayList<GraphNode>(),
				nodes.add((GraphNode) pop()), //
				Optional(
						'.',
						Optional(TriplesBlock(),
								nodes.addAll((List<GraphNode>) pop()))), //
				push(nodes));
	}

	public Rule GraphPatternNotTriples() {
		return FirstOf(OptionalGraphPattern(), GroupOrUnionGraphPattern(),
				GraphGraphPattern());
	}

	public Rule OptionalGraphPattern() {
		return Sequence("OPTIONAL", GroupGraphPattern(),
				push(new OptionalGraph((Graph) pop())));
	}

	public Rule GraphGraphPattern() {
		return Sequence("GRAPH", VarOrIRIref(), GroupGraphPattern(),
				push(new NamedGraph((GraphNode) pop(1), (Graph) pop())));
	}

	public Rule GroupOrUnionGraphPattern() {
		return Sequence(
				GroupGraphPattern(),
				Sequence(push(LIST_BEGIN),
						ZeroOrMore("UNION", GroupGraphPattern()),
						push(new UnionGraph(popList(Graph.class, 1)))));
	}

	public Rule Filter() {
		return Sequence("FILTER", Constraint());
	}

	public Rule Constraint() {
		return FirstOf(BrackettedExpression(), BuiltInCall(), FunctionCall());
	}

	@SuppressWarnings("unchecked")
	public Rule FunctionCall() {
		return Sequence(IriRef(), ArgList(), //
				push(new FunctionCall((Expression) pop(1),
						(List<Expression>) pop())));
	}

	public Rule ArgList() {
		List<Expression> args;
		return FirstOf(
				//
				Sequence('(', ')', push(Collections.emptyList())), //
				Sequence(
						'(',
						args = new ArrayList<Expression>(),
						Expression(),
						args.add((Expression) pop()),
						ZeroOrMore(',', Expression(),
								args.add((Expression) pop())), ')', //
						push(args)) //
		);
	}

	public Rule ConstructTemplate() {
		return Sequence(push(null), '{', Optional(ConstructTriples(), drop(1)),
				'}');
	}

	@SuppressWarnings("unchecked")
	public Rule ConstructTriples() {
		List<GraphNode> nodes;
		return Sequence(
				TriplesSameSubject(),
				nodes = new ArrayList<GraphNode>(), //
				nodes.add((GraphNode) pop()),
				Optional(
						'.',
						Optional(ConstructTriples(),
								nodes.addAll((List<GraphNode>) pop())//
						)), //
				push(nodes));
	}

	public Rule TriplesSameSubject() {
		return FirstOf(
				//
				Sequence(VarOrTerm(),
						PropertyListNotEmpty((AbstractGraphNode) peek())), //
				Sequence(TriplesNode(),
						PropertyList((AbstractGraphNode) peek())) //
		);
	}

	public boolean addPropertyPatterns(PropertyList propertyList,
			GraphNode predicate, List<GraphNode> objects) {
		for (GraphNode object : objects) {
			propertyList.add(new PropertyPattern(predicate, object));
		}
		return true;
	}

	protected PropertyList createPropertyList(GraphNode subject) {
		PropertyList propertyList = subject.getPropertyList();
		if ((propertyList == null || PropertyList.EMPTY_LIST
				.equals(propertyList)) && subject instanceof AbstractGraphNode) {
			propertyList = new PropertyList();
			((AbstractGraphNode) subject).setPropertyList(propertyList);
		}
		return propertyList;
	}

	@SuppressWarnings("unchecked")
	public Rule PropertyListNotEmpty(@Var GraphNode subject) {
		PropertyList propertyList;
		return Sequence(
				Verb(),
				ObjectList(), //
				propertyList = createPropertyList(subject),
				addPropertyPatterns(propertyList, (GraphNode) pop(1),
						(List<GraphNode>) pop()), //
				ZeroOrMore(
						';',
						Optional(Verb(),
								ObjectList(), //
								addPropertyPatterns(propertyList,
										(GraphNode) pop(1),
										(List<GraphNode>) pop()))) //
		);
	}

	public Rule PropertyList(@Var GraphNode subject) {
		return Optional(PropertyListNotEmpty(subject));
	}

	public Rule ObjectList() {
		return Sequence(
				Object(),
				Sequence(push(LIST_BEGIN), ZeroOrMore(',', Object()),
						push(popList(GraphNode.class, 1))));
	}

	public Rule Object() {
		return GraphNode();
	}

	public Rule Verb() {
		return FirstOf(VarOrIRIref(), Sequence('a', push(new IriRef(RDF_TYPE))));
	}

	public Rule TriplesNode() {
		return FirstOf(Collection(), BlankNodePropertyList());
	}

	public Rule BlankNodePropertyList() {
		return Sequence('[', push(new BNodePropertyList()),
				PropertyListNotEmpty((BNode) peek()), ']');
	}

	public Rule Collection() {
		return Sequence('(', push(LIST_BEGIN), OneOrMore(GraphNode()), //
				push(new Collection(popList(GraphNode.class))), ')');
	}

	public Rule GraphNode() {
		return FirstOf(VarOrTerm(), TriplesNode());
	}

	public Rule VarOrTerm() {
		return FirstOf(Var(), GraphTerm());
	}

	public Rule VarOrIRIref() {
		return FirstOf(Var(), IriRef());
	}

	public Rule Var() {
		return FirstOf(VAR1(), VAR2());
	}

	public Rule GraphTerm() {
		return FirstOf(IriRef(), RdfLiteral(), NumericLiteral(),
				BooleanLiteral(), BlankNode(),
				Sequence('(', ')', push(new IriRef(RDF_NIL))));
	}

	public Rule Expression() {
		return ConditionalOrExpression();
	}

	public Rule ConditionalOrExpression() {
		return Sequence(
				ConditionalAndExpression(),
				Optional(
						push(LIST_BEGIN),
						OneOrMore("||", ConditionalAndExpression()), //
						push(new LogicalExpr(LogicalOperator.OR, popList(
								Expression.class, 1)))));
	}

	public Rule ConditionalAndExpression() {
		return Sequence(
				ValueLogical(),
				Optional(push(LIST_BEGIN),
						OneOrMore("&&", ValueLogical()), //
						push(new LogicalExpr(LogicalOperator.AND, popList(
								Expression.class, 1)))));
	}

	public Rule ValueLogical() {
		return RelationalExpression();
	}

	public Rule RelationalExpression() {
		return Sequence(
				NumericExpression(), //
				Optional(RelationalOperator(), NumericExpression(), //
						push(new RelationalExpr((RelationalOperator) pop(1),
								(Expression) pop(1), (Expression) pop()))));
	}

	public Rule RelationalOperator() {
		return Sequence(FirstOf('=', "!=", "<=", ">=", '<', '>'),
				push(RelationalOperator.fromSymbol(match().trim())));
	}

	public Rule NumericExpression() {
		return AdditiveExpression();
	}

	public Rule AdditiveExpression() {
		Expression expr;
		return Sequence(
				MultiplicativeExpression(),
				expr = (Expression) pop(), //
				ZeroOrMore(FirstOf(
						//
						Sequence('+', MultiplicativeExpression(),
								expr = new NumericExpr(NumericOperator.ADD,
										expr, (Expression) pop())), //
						Sequence('-', MultiplicativeExpression(),
								expr = new NumericExpr(NumericOperator.SUB,
										expr, (Expression) pop())), //
						Sequence(NumericLiteralPositive(),
								expr = new NumericExpr(NumericOperator.ADD,
										expr, (Expression) pop())), //
						Sequence(
								NumericLiteralNegative(),
								expr = new NumericExpr(NumericOperator.SUB,
										expr, ((NumericLiteral) pop()).negate())))),
				push(expr));
	}

	public Rule MultiplicativeExpression() {
		Expression expr;
		return Sequence(UnaryExpression(),
				expr = (Expression) pop(), //
				ZeroOrMore(FirstOf(
						//
						Sequence('*', UnaryExpression(),
								expr = new NumericExpr(NumericOperator.MUL,
										expr, (Expression) pop())), //
						Sequence('/', UnaryExpression(),
								expr = new NumericExpr(NumericOperator.DIV,
										expr, (Expression) pop())) //
				)), push(expr));
	}

	public Rule UnaryExpression() {
		return FirstOf(
				Sequence('!',
						PrimaryExpression(), //
						push(new LogicalExpr(LogicalOperator.NOT, Collections
								.singletonList((Expression) pop())))), //
				Sequence('+', PrimaryExpression()), //
				Sequence('-', PrimaryExpression(), //
						push(new NegateExpr((Expression) pop()))), //
				PrimaryExpression());
	}

	public Rule PrimaryExpression() {
		return FirstOf(BrackettedExpression(), BuiltInCall(),
				IriRefOrFunction(), RdfLiteral(), NumericLiteral(),
				BooleanLiteral(), Var());
	}

	public Rule BrackettedExpression() {
		return Sequence('(', Expression(), ')');
	}

	public boolean beginExprList() {
		push(match());
		push(LIST_BEGIN);
		return true;
	}

	public Rule BuiltInCall() {
		List<Expression> args;
		return Sequence(
				FirstOf(Sequence("STR", beginExprList(), '(', Expression(), ')'), //
						Sequence("LANG", beginExprList(), '(', Expression(),
								')'), //
						Sequence("LANGMATCHES", beginExprList(), '(',
								Expression(), ',', Expression(), ')'), //
						Sequence("DATATYPE", beginExprList(), '(',
								Expression(), ')'), //
						Sequence("BOUND", beginExprList(), '(', Var(), ')'), //
						Sequence("SAMETERM", beginExprList(), '(',
								Expression(), ',', Expression(), ')'), //
						Sequence("ISIRI", beginExprList(), '(', Expression(),
								')'), //
						Sequence("ISURI", beginExprList(), '(', Expression(),
								')'), //
						Sequence("ISBLANK", beginExprList(), '(', Expression(),
								')'), //
						Sequence("ISLITERAL", beginExprList(), '(',
								Expression(), ')'), //
						Sequence("REGEX", beginExprList(), '(', Expression(),
								',', Expression(), Optional(',', Expression()),
								')') //
				), //
				args = popList(Expression.class), //
				push(new BuiltInCall((String) pop(), args)) //
		);
	}

	@SuppressWarnings("unchecked")
	public Rule IriRefOrFunction() {
		return Sequence(
				IriRef(), //
				Optional(ArgList(), push(new FunctionCall((Expression) pop(1),
						(List<Expression>) pop()))));
	}

	public Rule VAR1() {
		return Sequence(Ch('?'), VARNAME());
	}

	public Rule VAR2() {
		return Sequence(Ch('$'), VARNAME());
	}

	public Rule VARNAME() {
		return Sequence(
				Sequence(
						FirstOf(PN_CHARS_U(), DIGIT()),
						ZeroOrMore(FirstOf(PN_CHARS_U(), DIGIT(), Ch('\u00B7'),
								CharRange('\u0300', '\u036F'),
								CharRange('\u203F', '\u2040')))),
				push(new Variable(match())), WS());
	}

	@Override
	protected Rule fromStringLiteral(String string) {
		return Sequence(IgnoreCase(string), WS());
	}
}