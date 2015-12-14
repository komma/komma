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

import com.github.fge.grappa.rules.Rule;
import com.github.fge.grappa.support.Var;

import net.enilink.komma.parser.BaseRdfParser;
import net.enilink.komma.parser.sparql.tree.AbstractGraphNode;
import net.enilink.komma.parser.sparql.tree.AskQuery;
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
 */
public class SparqlParser extends BaseRdfParser {
	public static String RDF_NAMESPACE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	public static String RDF_TYPE = RDF_NAMESPACE + "type";
	public static String RDF_NIL = RDF_NAMESPACE + "nil";

	public Rule Query() {
		return sequence(
				WS(),
				Prologue(),
				firstOf(SelectQuery(), ConstructQuery(), DescribeQuery(),
						AskQuery()),
				setPrologue((Prologue) pop(1), (Query) peek()), EOI);
	}

	public boolean setPrologue(Prologue prologue, Query query) {
		if (query != null) {
			query.setPrologue(prologue);
		}
		return true;
	}

	public Rule Prologue() {
		Var<IriRef> base = new Var<>();
		Var<List<PrefixDecl>> prefixes = new Var<>();
		return sequence(
				prefixes.set(new ArrayList<PrefixDecl>()),
				optional(BaseDecl(), base.set((IriRef) pop())),
				zeroOrMore(PrefixDecl(), prefixes.get().add((PrefixDecl) pop())), //
				push(new Prologue(base.get(), prefixes.get())));
	}

	public Rule BaseDecl() {
		return sequence("BASE", IRI_REF());
	}

	public Rule PrefixDecl() {
		return sequence("PREFIX", PNAME_NS(), WS(), IRI_REF(), //
				push(new PrefixDecl((String) pop(1), (IriRef) pop())));
	}

	@SuppressWarnings("unchecked")
	public Rule SelectQuery() {
		Var<SelectQuery.Modifier> modifier = new Var<>();
		Var<List<Variable>> projection = new Var<>();
		Var<Dataset> dataset = new Var<>();
		return sequence(
				"SELECT",
				optional(firstOf(
						//
						sequence("DISTINCT",
								modifier.set(SelectQuery.Modifier.DISTINCT)),
						sequence("REDUCED",
								modifier.set(SelectQuery.Modifier.REDUCED)))
				//
				),
				push(LIST_BEGIN),
				firstOf(oneOrMore(Var()), '*'),
				projection.set(popList(Variable.class)), //
				dataset.set(new Dataset()),
				zeroOrMore(DatasetClause(dataset)),
				WhereClause(),
				SolutionModifier(), //
				push(new SelectQuery(modifier.get(), projection.get(), dataset
						.get(), (Graph) pop(1), (List<SolutionModifier>) pop())) //
		);
	}

	@SuppressWarnings("unchecked")
	public Rule ConstructQuery() {
		Var<Dataset> dataset = new Var<>();
		return sequence("CONSTRUCT", ConstructTemplate(),
				dataset.set(new Dataset()), zeroOrMore(DatasetClause(dataset)),
				WhereClause(), SolutionModifier(), //
				push(new ConstructQuery((List<GraphNode>) pop(2),
						dataset.get(), (Graph) pop(1),
						(List<SolutionModifier>) pop())) //
		);
	}

	@SuppressWarnings("unchecked")
	public Rule DescribeQuery() {
		Var<List<GraphNode>> subjects = new Var<>();
		Var<Dataset> dataset = new Var<>();
		Var<Graph> where = new Var<>();
		return sequence(
				"DESCRIBE",
				push(LIST_BEGIN),
				firstOf(oneOrMore(VarOrIRIref()), '*'),
				subjects.set(popList(GraphNode.class)),
				dataset.set(new Dataset()),
				zeroOrMore(DatasetClause(dataset)),
				optional(WhereClause(), where.set((Graph) pop())),
				SolutionModifier(), //
				push(new DescribeQuery(subjects.get(), dataset.get(), where
						.get(), (List<SolutionModifier>) pop())) //
		);
	}

	public Rule AskQuery() {
		Var<Dataset> dataset = new Var<>();
		return sequence("ASK", dataset.set(new Dataset()),
				zeroOrMore(DatasetClause(dataset)), WhereClause(), //
				push(new AskQuery(dataset.get(), (Graph) pop())) //
		);
	}

	public Rule DatasetClause(Var<Dataset> dataset) {
		return sequence(
				"FROM",
				firstOf(//
				sequence(DefaultGraphClause(),
						dataset.get().addDefaultGraph((Expression) pop())), //
						sequence(NamedGraphClause(), dataset.get()
								.addNamedGraph((Expression) pop()))));
	}

	public Rule DefaultGraphClause() {
		return SourceSelector();
	}

	public Rule NamedGraphClause() {
		return sequence("NAMED", SourceSelector());
	}

	public Rule SourceSelector() {
		return IriRef();
	}

	public Rule WhereClause() {
		return sequence(optional("WHERE"), GroupGraphPattern());
	}

	@SuppressWarnings("unchecked")
	public Rule SolutionModifier() {
		Var<List<SolutionModifier>> modifiers = new Var<>();
		return sequence(
				modifiers.set(new ArrayList<SolutionModifier>()),
				optional(OrderClause(),
						modifiers.get().add((SolutionModifier) pop())),
				optional(LimitOffsetClauses(),
						modifiers.get().addAll((List<SolutionModifier>) pop())),
				push(modifiers.get()));
	}

	public Rule LimitOffsetClauses() {
		return firstOf(
				//
				sequence(push(LIST_BEGIN), LimitClause(),
						optional(OffsetClause()),
						push(popList(SolutionModifier.class))), //
				sequence(push(LIST_BEGIN), OffsetClause(),
						optional(LimitClause()),
						push(popList(SolutionModifier.class))));
	}

	public Rule OrderClause() {
		return sequence("ORDER", "BY", push(LIST_BEGIN),
				oneOrMore(OrderCondition()), push(new OrderModifier(
						popList(OrderCondition.class))));
	}

	public Rule OrderCondition() {
		return firstOf(
				sequence(
						firstOf("ASC", "DESC"),
						push(match()),
						BrackettedExpression(),
						push(new OrderCondition("asc".equals(((String) pop(1))
								.toLowerCase()) ? OrderCondition.Direction.ASC
								: OrderCondition.Direction.DESC,
								(Expression) pop()))), //
				sequence(firstOf(Constraint(), Var()), push(new OrderCondition(
						OrderCondition.Direction.ASC, (Expression) pop()))));
	}

	public Rule LimitClause() {
		return sequence("LIMIT", INTEGER(), //
				push(new LimitModifier(((IntegerLiteral) pop()).getValue())) //
		);
	}

	public Rule OffsetClause() {
		return sequence("OFFSET", INTEGER(), //
				push(new OffsetModifier(((IntegerLiteral) pop()).getValue())) //
		);
	}

	@SuppressWarnings("unchecked")
	public Rule GroupGraphPattern() {
		Var<List<Graph>> patterns = new Var<>();
		Var<List<Expression>> filters = new Var<>();
		return sequence(
				'{',
				patterns.set(new ArrayList<Graph>()),
				filters.set(new ArrayList<Expression>()),
				optional(
						TriplesBlock(), //
						patterns.get().add(
								new BasicGraphPattern((List<GraphNode>) pop()))),
				zeroOrMore(
						firstOf(sequence(GraphPatternNotTriples(), patterns
								.get().add((Graph) pop())),
								sequence(Filter(),
										filters.get().add((Expression) pop()))),
						optional('.'),
						optional(
								TriplesBlock(), //
								patterns.get().add(
										new BasicGraphPattern(
												(List<GraphNode>) pop())))),
				'}', //
				push(new GraphPattern(patterns.get(), filters.get())));
	}

	@SuppressWarnings("unchecked")
	public Rule TriplesBlock() {
		Var<List<GraphNode>> nodes = new Var<>();
		return sequence(
				TriplesSameSubject(), //
				nodes.set(new ArrayList<GraphNode>()),
				nodes.get().add((GraphNode) pop()), //
				optional(
						'.',
						optional(TriplesBlock(),
								nodes.get().addAll((List<GraphNode>) pop()))), //
				push(nodes.get()));
	}

	public Rule GraphPatternNotTriples() {
		return firstOf(OptionalGraphPattern(), GroupOrUnionGraphPattern(),
				GraphGraphPattern());
	}

	public Rule OptionalGraphPattern() {
		return sequence("OPTIONAL", GroupGraphPattern(),
				push(new OptionalGraph((Graph) pop())));
	}

	public Rule GraphGraphPattern() {
		return sequence("GRAPH", VarOrIRIref(), GroupGraphPattern(),
				push(new NamedGraph((GraphNode) pop(1), (Graph) pop())));
	}

	public Rule GroupOrUnionGraphPattern() {
		return sequence(
				GroupGraphPattern(),
				sequence(push(LIST_BEGIN),
						zeroOrMore("UNION", GroupGraphPattern()),
						push(new UnionGraph(popList(Graph.class, 1)))));
	}

	public Rule Filter() {
		return sequence("FILTER", Constraint());
	}

	public Rule Constraint() {
		return firstOf(BrackettedExpression(), BuiltInCall(), FunctionCall());
	}

	@SuppressWarnings("unchecked")
	public Rule FunctionCall() {
		return sequence(IriRef(), ArgList(), //
				push(new FunctionCall((Expression) pop(1),
						(List<Expression>) pop())));
	}

	public Rule ArgList() {
		Var<List<Expression>> args = new Var<>();
		return firstOf(
				//
				sequence('(', ')', push(Collections.emptyList())), //
				sequence(
						'(',
						args.set(new ArrayList<Expression>()),
						Expression(),
						args.get().add((Expression) pop()),
						zeroOrMore(',', Expression(),
								args.get().add((Expression) pop())), ')', //
						push(args.get())) //
		);
	}

	public Rule ConstructTemplate() {
		return sequence(push(null), '{', optional(ConstructTriples(), drop(1)),
				'}');
	}

	@SuppressWarnings("unchecked")
	public Rule ConstructTriples() {
		Var<List<GraphNode>> nodes = new Var<>();
		return sequence(
				TriplesSameSubject(),
				nodes.set(new ArrayList<GraphNode>()), //
				nodes.get().add((GraphNode) pop()),
				optional(
						'.',
						optional(ConstructTriples(),
								nodes.get().addAll((List<GraphNode>) pop())//
						)), //
				push(nodes.get()));
	}

	public Rule TriplesSameSubject() {
		Var<GraphNode> subject = new Var<>();
		return firstOf(
				sequence(VarOrTerm(), subject.set((GraphNode) peek()),
						PropertyListNotEmpty(subject)), //
				sequence(TriplesNode(), subject.set((GraphNode) peek()),
						PropertyList(subject)) //
		);
	}

	public boolean addPropertyPatterns(PropertyList propertyList,
			GraphNode predicate, List<GraphNode> objects) {
		for (GraphNode object : objects) {
			propertyList.add(new PropertyPattern(predicate, object));
		}
		return true;
	}

	public PropertyList createPropertyList(GraphNode subject) {
		PropertyList propertyList = subject.getPropertyList();
		if ((propertyList == null || PropertyList.EMPTY_LIST
				.equals(propertyList)) && subject instanceof AbstractGraphNode) {
			propertyList = new PropertyList();
			((AbstractGraphNode) subject).setPropertyList(propertyList);
		}
		return propertyList;
	}

	@SuppressWarnings("unchecked")
	public Rule PropertyListNotEmpty(Var<GraphNode> subject) {
		Var<PropertyList> propertyList = new Var<>();
		return sequence(
				Verb(),
				ObjectList(), //
				propertyList.set(createPropertyList(subject.get())),
				addPropertyPatterns(propertyList.get(), (GraphNode) pop(1),
						(List<GraphNode>) pop()), //
				zeroOrMore(
						';',
						optional(Verb(),
								ObjectList(), //
								addPropertyPatterns(propertyList.get(),
										(GraphNode) pop(1),
										(List<GraphNode>) pop()))) //
		);
	}

	public Rule PropertyList(Var<GraphNode> subject) {
		return optional(PropertyListNotEmpty(subject));
	}

	public Rule ObjectList() {
		return sequence(
				Object(),
				sequence(push(LIST_BEGIN), zeroOrMore(',', Object()),
						push(popList(GraphNode.class, 1))));
	}

	public Rule Object() {
		return GraphNode();
	}

	public Rule Verb() {
		return firstOf(VarOrIRIref(), sequence('a', push(new IriRef(RDF_TYPE))));
	}

	public Rule TriplesNode() {
		return firstOf(Collection(), BlankNodePropertyList());
	}

	public Rule BlankNodePropertyList() {
		Var<GraphNode> subject = new Var<>();
		return sequence('[', push(new BNodePropertyList()),
				subject.set((GraphNode) peek()), PropertyListNotEmpty(subject),
				']');
	}

	public Rule Collection() {
		return sequence('(', push(LIST_BEGIN), oneOrMore(GraphNode()), //
				push(new Collection(popList(GraphNode.class))), ')');
	}

	public Rule GraphNode() {
		return firstOf(VarOrTerm(), TriplesNode());
	}

	public Rule VarOrTerm() {
		return firstOf(Var(), GraphTerm());
	}

	public Rule VarOrIRIref() {
		return firstOf(Var(), IriRef());
	}

	public Rule Var() {
		return firstOf(VAR1(), VAR2());
	}

	public Rule GraphTerm() {
		return firstOf(IriRef(), RdfLiteral(), NumericLiteral(),
				BooleanLiteral(), BlankNode(),
				sequence('(', ')', push(new IriRef(RDF_NIL))));
	}

	public Rule Expression() {
		return ConditionalOrExpression();
	}

	public Rule ConditionalOrExpression() {
		return sequence(
				ConditionalAndExpression(),
				optional(
						push(LIST_BEGIN),
						oneOrMore("||", ConditionalAndExpression()), //
						push(new LogicalExpr(LogicalOperator.OR, popList(
								Expression.class, 1)))));
	}

	public Rule ConditionalAndExpression() {
		return sequence(
				ValueLogical(),
				optional(push(LIST_BEGIN),
						oneOrMore("&&", ValueLogical()), //
						push(new LogicalExpr(LogicalOperator.AND, popList(
								Expression.class, 1)))));
	}

	public Rule ValueLogical() {
		return RelationalExpression();
	}

	public Rule RelationalExpression() {
		return sequence(
				NumericExpression(), //
				optional(RelationalOperator(), NumericExpression(), //
						push(new RelationalExpr((RelationalOperator) pop(1),
								(Expression) pop(1), (Expression) pop()))));
	}

	public Rule RelationalOperator() {
		return sequence(firstOf('=', "!=", "<=", ">=", '<', '>'),
				push(RelationalOperator.fromSymbol(match().trim())));
	}

	public Rule NumericExpression() {
		return AdditiveExpression();
	}

	public Rule AdditiveExpression() {
		Var<Expression> expr = new Var<>();
		return sequence(
				MultiplicativeExpression(),
				expr.set((Expression) pop()), //
				zeroOrMore(firstOf(
						//
						sequence('+', MultiplicativeExpression(), expr
								.set(new NumericExpr(NumericOperator.ADD, expr
										.get(), (Expression) pop()))), //
						sequence('-', MultiplicativeExpression(), expr
								.set(new NumericExpr(NumericOperator.SUB, expr
										.get(), (Expression) pop()))), //
						sequence(NumericLiteralPositive(), expr
								.set(new NumericExpr(NumericOperator.ADD, expr
										.get(), (Expression) pop()))), //
						sequence(NumericLiteralNegative(), expr
								.set(new NumericExpr(NumericOperator.SUB, expr
										.get(), ((NumericLiteral) pop())
										.negate()))))), push(expr.get()));
	}

	public Rule MultiplicativeExpression() {
		Var<Expression> expr = new Var<>();
		return sequence(UnaryExpression(),
				expr.set((Expression) pop()), //
				zeroOrMore(firstOf(
						//
						sequence('*', UnaryExpression(), expr
								.set(new NumericExpr(NumericOperator.MUL, expr
										.get(), (Expression) pop()))), //
						sequence('/', UnaryExpression(), expr
								.set(new NumericExpr(NumericOperator.DIV, expr
										.get(), (Expression) pop()))) //
				)), push(expr.get()));
	}

	public Rule UnaryExpression() {
		return firstOf(
				sequence('!',
						PrimaryExpression(), //
						push(new LogicalExpr(LogicalOperator.NOT, Collections
								.singletonList((Expression) pop())))), //
				sequence('+', PrimaryExpression()), //
				sequence('-', PrimaryExpression(), //
						push(new NegateExpr((Expression) pop()))), //
				PrimaryExpression());
	}

	public Rule PrimaryExpression() {
		return firstOf(BrackettedExpression(), BuiltInCall(),
				IriRefOrFunction(), RdfLiteral(), NumericLiteral(),
				BooleanLiteral(), Var());
	}

	public Rule BrackettedExpression() {
		return sequence('(', Expression(), ')');
	}

	public boolean beginExprList() {
		push(match());
		push(LIST_BEGIN);
		return true;
	}

	public Rule BuiltInCall() {
		Var<List<Expression>> args = new Var<>();
		return sequence(
				firstOf(sequence("STR", beginExprList(), '(', Expression(), ')'), //
						sequence("LANG", beginExprList(), '(', Expression(),
								')'), //
						sequence("LANGMATCHES", beginExprList(), '(',
								Expression(), ',', Expression(), ')'), //
						sequence("DATATYPE", beginExprList(), '(',
								Expression(), ')'), //
						sequence("BOUND", beginExprList(), '(', Var(), ')'), //
						sequence("SAMETERM", beginExprList(), '(',
								Expression(), ',', Expression(), ')'), //
						sequence("ISIRI", beginExprList(), '(', Expression(),
								')'), //
						sequence("ISURI", beginExprList(), '(', Expression(),
								')'), //
						sequence("ISBLANK", beginExprList(), '(', Expression(),
								')'), //
						sequence("ISLITERAL", beginExprList(), '(',
								Expression(), ')'), //
						sequence("REGEX", beginExprList(), '(', Expression(),
								',', Expression(), optional(',', Expression()),
								')') //
				), //
				args.set(popList(Expression.class)), //
				push(new BuiltInCall((String) pop(), args.get())) //
		);
	}

	@SuppressWarnings("unchecked")
	public Rule IriRefOrFunction() {
		return sequence(
				IriRef(), //
				optional(ArgList(), push(new FunctionCall((Expression) pop(1),
						(List<Expression>) pop()))));
	}

	public Rule VAR1() {
		return sequence(ch('?'), VARNAME());
	}

	public Rule VAR2() {
		return sequence(ch('$'), VARNAME());
	}

	public Rule VARNAME() {
		return sequence(
				sequence(
						firstOf(PN_CHARS_U(), DIGIT()),
						zeroOrMore(firstOf(PN_CHARS_U(), DIGIT(), ch('\u00B7'),
								charRange('\u0300', '\u036F'),
								charRange('\u203F', '\u2040')))),
				push(new Variable(match())), WS());
	}

	@Override
	protected Rule fromStringLiteral(String string) {
		return sequence(ignoreCase(string), WS());
	}
}