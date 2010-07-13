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

	public static String RDF_FIRST = RDF_NAMESPACE + "first";
	public static String RDF_NEXT = RDF_NAMESPACE + "next";
	public static String RDF_NIL = RDF_NAMESPACE + "nil";

	public Rule Query() {
		return Sequence(
				WS(),
				Prologue(),
				FirstOf(SelectQuery(), ConstructQuery(), DescribeQuery(),
						AskQuery()),
				setPrologue((Prologue) value("P"), (Query) lastValue()), Eoi());
	}

	protected boolean setPrologue(Prologue prologue, Query query) {
		if (query != null) {
			query.setPrologue(prologue);
		}
		return true;
	}

	public Rule Prologue() {
		return Sequence(
				Optional(BaseDecl()),
				ZeroOrMore(PrefixDecl()),
				set(new Prologue((IriRef) value("O/B"), toList(
						PrefixDecl.class, values("Z/P")))));
	}

	public Rule BaseDecl() {
		return Sequence("BASE", IRI_REF());
	}

	public Rule PrefixDecl() {
		return Sequence("PREFIX", PNAME_NS(), IRI_REF(),
				set(new PrefixDecl(stripColon(text("PNAME_NS").trim()),
						(IriRef) value("IRI_REF"))));
	}

	@SuppressWarnings("unchecked")
	public Rule SelectQuery() {
		SelectQuery.Modifier modifier;
		Dataset dataset;
		return Sequence(
				"SELECT",
				Optional(FirstOf(
						//
						Sequence("DISTINCT",
								DO(modifier = SelectQuery.Modifier.DISTINCT)),
						Sequence("REDUCED",
								DO(modifier = SelectQuery.Modifier.REDUCED)))
				//
				),
				FirstOf(OneOrMore(Var()), '*'),
				DO(dataset = new Dataset()),
				ZeroOrMore(DatasetClause(dataset)),
				WhereClause(),
				SolutionModifier(), //
				set(new SelectQuery(modifier, toList(Variable.class,
						values("F/O/V")), dataset,
						(Graph) value("WhereClause"),
						(List<SolutionModifier>) lastValue())) //
		);
	}

	@SuppressWarnings("unchecked")
	public Rule ConstructQuery() {
		Dataset dataset;
		return Sequence("CONSTRUCT", ConstructTemplate(),
				DO(dataset = new Dataset()),
				ZeroOrMore(DatasetClause(dataset)), WhereClause(),
				SolutionModifier(), //
				set(new ConstructQuery((List<GraphNode>) value("C"), dataset,
						(Graph) value("WhereClause"),
						(List<SolutionModifier>) lastValue())) //
		);
	}

	@SuppressWarnings("unchecked")
	public Rule DescribeQuery() {
		Dataset dataset;
		return Sequence("DESCRIBE", FirstOf(OneOrMore(VarOrIRIref()), '*'),
				DO(dataset = new Dataset()),
				ZeroOrMore(DatasetClause(dataset)), Optional(WhereClause()),
				SolutionModifier(), //
				set(new DescribeQuery(toList(GraphNode.class, values("F/O/V")),
						dataset, (Graph) value("O/W"),
						(List<SolutionModifier>) lastValue())) //
		);
	}

	public Rule AskQuery() {
		Dataset dataset;
		return Sequence("ASK", DO(dataset = new Dataset()),
				ZeroOrMore(DatasetClause(dataset)), WhereClause(), //
				set(new AskQuery(dataset, (Graph) value("WhereClause"))) //
		);
	}

	public Rule DatasetClause(@Var Dataset dataset) {
		return Sequence(
				"FROM",
				FirstOf(//
				Sequence(DefaultGraphClause(),
						dataset.addDefaultGraph((Expression) lastValue())), //
						Sequence(NamedGraphClause(),
								dataset.addNamedGraph((Expression) lastValue()))));
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
		return Sequence(
				Optional(OrderClause()),
				Optional(LimitOffsetClauses()),
				set(toList(SolutionModifier.class, values("O/O"),
						(List<SolutionModifier>) value("last:O/L"))));
	}

	public Rule LimitOffsetClauses() {
		return FirstOf(
				//
				Sequence(
						LimitClause(),
						Optional(OffsetClause()),
						set(toList(SolutionModifier.class, value("L"),
								values("O/O")))), //
				Sequence(
						OffsetClause(),
						Optional(LimitClause()),
						set(toList(SolutionModifier.class, value("O"),
								values("O/L")))));
	}

	public Rule OrderClause() {
		return Sequence(
				"ORDER",
				"BY",
				OneOrMore(OrderCondition()),
				set(new OrderModifier(toList(OrderCondition.class, values("")))));
	}

	public Rule OrderCondition() {
		return FirstOf(
				Sequence(
						FirstOf("ASC", "DESC"),
						BrackettedExpression(),
						set(new OrderCondition("asc".equals(text("F")
								.toLowerCase()) ? OrderCondition.Direction.ASC
								: OrderCondition.Direction.DESC,
								(Expression) lastValue()))), //
				Sequence(FirstOf(Constraint(), Var()),
						set(new OrderCondition(OrderCondition.Direction.ASC,
								(Expression) lastValue()))));
	}

	public Rule LimitClause() {
		return Sequence("LIMIT", INTEGER(), //
				set(new LimitModifier(((IntegerLiteral) value()).getValue())) //
		);
	}

	public Rule OffsetClause() {
		return Sequence("OFFSET", INTEGER(), //
				set(new OffsetModifier(((IntegerLiteral) value()).getValue())) //
		);
	}

	@SuppressWarnings("unchecked")
	public Rule GroupGraphPattern() {
		List<Graph> patterns;
		return Sequence(
				'{',
				DO(patterns = new ArrayList<Graph>()),
				Optional(Sequence(TriplesBlock(), //
						patterns.add(new BasicGraphPattern(
								(List<GraphNode>) lastValue())))),
				ZeroOrMore(Sequence(
						FirstOf(Sequence(GraphPatternNotTriples(),
								patterns.add((Graph) lastValue())), Filter()),
						Optional('.'),
						Optional(Sequence(TriplesBlock(), //
								patterns.add(new BasicGraphPattern(
										(List<GraphNode>) lastValue())))))),
				'}', //
				set(new GraphPattern(patterns, toList(Expression.class,
						values("Z/S/F/F")))));
	}

	@SuppressWarnings("unchecked")
	public Rule TriplesBlock() {
		List<GraphNode> nodes;
		return Sequence(
				TriplesSameSubject(), //
				DO(nodes = new ArrayList<GraphNode>()),
				nodes.add((GraphNode) lastValue()), //
				Optional(Sequence(
						'.',
						Optional(Sequence(TriplesBlock(),
								nodes.addAll((List<GraphNode>) lastValue()))))), //
				set(nodes));
	}

	public Rule GraphPatternNotTriples() {
		return FirstOf(OptionalGraphPattern(), GroupOrUnionGraphPattern(),
				GraphGraphPattern());
	}

	public Rule OptionalGraphPattern() {
		return Sequence("OPTIONAL", GroupGraphPattern(), set(new OptionalGraph(
				(Graph) lastValue())));
	}

	public Rule GraphGraphPattern() {
		return Sequence(
				"GRAPH",
				VarOrIRIref(),
				GroupGraphPattern(),
				set(new NamedGraph((GraphNode) value("V"), (Graph) lastValue())));
	}

	public Rule GroupOrUnionGraphPattern() {
		return Sequence(
				GroupGraphPattern(),
				Sequence(
						ZeroOrMore(Sequence("UNION", GroupGraphPattern())),
						set(new UnionGraph(toList(Graph.class, UP(value("G")),
								values("Z/S"))))));
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
				set(new FunctionCall((Expression) value("I"),
						(List<Expression>) value("A"))));
	}

	public Rule ArgList() {
		return FirstOf(
				//
				Sequence('(', ')', set(Collections.emptyList())), //
				Sequence(
						'(',
						Expression(),
						ZeroOrMore(Sequence(',', Expression())),
						')', //
						set(toList(Expression.class, value("E"), values("Z/S")))) //
		);
	}

	public Rule ConstructTemplate() {
		return Sequence('{', Optional(ConstructTriples()), '}');
	}

	@SuppressWarnings("unchecked")
	public Rule ConstructTriples() {
		List<GraphNode> nodes;
		return Sequence(
				TriplesSameSubject(),
				DO(nodes = new ArrayList<GraphNode>()), //
				nodes.add((GraphNode) lastValue()),
				Optional(Sequence(
						'.',
						Optional(//
						Sequence(ConstructTriples(),
								nodes.addAll((List<GraphNode>) lastValue()))//
						))), //
				set(nodes));
	}

	public Rule TriplesSameSubject() {
		return FirstOf(
				//
				Sequence(VarOrTerm(), set(),
						PropertyListNotEmpty((AbstractGraphNode) lastValue())), //
				Sequence(TriplesNode(), set(),
						PropertyList((AbstractGraphNode) lastValue())) //
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
				DO(propertyList = createPropertyList(subject)),
				addPropertyPatterns(propertyList, (GraphNode) value("V"),
						(List<GraphNode>) value("O")), //
				ZeroOrMore(Sequence(
						';',
						Optional(Sequence(Verb(),
								ObjectList(), //
								addPropertyPatterns(propertyList,
										(GraphNode) value("V"),
										(List<GraphNode>) value("O")))))));
	}

	public Rule PropertyList(@Var GraphNode subject) {
		return Optional(PropertyListNotEmpty(subject));
	}

	public Rule ObjectList() {
		return Sequence(Object(), ZeroOrMore(Sequence(',', Object())),
				set(toList(GraphNode.class, value("O"), values("Z/S/O"))));
	}

	public Rule Object() {
		return GraphNode();
	}

	public Rule Verb() {
		return FirstOf(VarOrIRIref(), Sequence('a', set(new IriRef(RDF_TYPE))));
	}

	public Rule TriplesNode() {
		return FirstOf(Collection(), BlankNodePropertyList());
	}

	public Rule BlankNodePropertyList() {
		return Sequence('[', set(new BNodePropertyList()),
				PropertyListNotEmpty(UP((BNode) value())), ']');
	}

	public Rule Collection() {
		return Sequence('(',
				OneOrMore(GraphNode()), //
				set(new Collection(toList(GraphNode.class, values("O/G")))),
				')');
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
				Sequence(Sequence('(', ')'), set(new IriRef(RDF_NIL))));
	}

	public Rule Expression() {
		return ConditionalOrExpression();
	}

	public Rule ConditionalOrExpression() {
		return Sequence(ConditionalAndExpression(),
				ZeroOrMore(Sequence("||", ConditionalAndExpression())), //
				set(node("Z/S") != null ? new LogicalExpr(LogicalOperator.OR,
						toList(Expression.class, value("C"), values("Z/S")))
						: value()));
	}

	public Rule ConditionalAndExpression() {
		return Sequence(ValueLogical(),
				ZeroOrMore(Sequence("&&", ValueLogical())), //
				set(node("Z/S") != null ? new LogicalExpr(LogicalOperator.AND,
						toList(Expression.class, value("V"), values("Z/S")))
						: value()));
	}

	public Rule ValueLogical() {
		return RelationalExpression();
	}

	public Rule RelationalExpression() {
		return Sequence(
				NumericExpression(), //
				Optional(Sequence(
						FirstOf(//
						Sequence('=', NumericExpression()), //
						Sequence("!=", NumericExpression()), //
								Sequence('<', NumericExpression()), //
								Sequence('>', NumericExpression()), //
								Sequence("<=", NumericExpression()), //
								Sequence(">=", NumericExpression()) //
						), //
						set(new RelationalExpr(RelationalOperator
								.fromSymbol(text("F/S/").trim()),
								UP(UP((Expression) value("N"))),
								(Expression) value("F/S/N"))))));
	}

	public Rule NumericExpression() {
		return AdditiveExpression();
	}

	public Rule AdditiveExpression() {
		Expression expr;
		return Sequence(
				MultiplicativeExpression(),
				DO(expr = (Expression) lastValue()), //
				ZeroOrMore(FirstOf(
						//
						Sequence('+', MultiplicativeExpression(),
								DO(expr = new NumericExpr(NumericOperator.ADD,
										expr, (Expression) lastValue()))), //
						Sequence('-', MultiplicativeExpression(),
								DO(expr = new NumericExpr(NumericOperator.SUB,
										expr, (Expression) lastValue()))), //
						Sequence(NumericLiteralPositive(),
								DO(expr = new NumericExpr(NumericOperator.ADD,
										expr, (Expression) lastValue()))), //
						Sequence(
								NumericLiteralNegative(),
								DO(expr = new NumericExpr(NumericOperator.SUB,
										expr, ((NumericLiteral) lastValue())
												.negate()))))), set(expr));
	}

	public Rule MultiplicativeExpression() {
		Expression expr;
		return Sequence(UnaryExpression(),
				DO(expr = (Expression) lastValue()), //
				ZeroOrMore(FirstOf(
						//
						Sequence('*', UnaryExpression(),
								DO(expr = new NumericExpr(NumericOperator.MUL,
										expr, (Expression) lastValue()))), //
						Sequence('/', UnaryExpression(),
								DO(expr = new NumericExpr(NumericOperator.DIV,
										expr, (Expression) lastValue()))) //
				)), set(expr));
	}

	public Rule UnaryExpression() {
		return FirstOf(
				Sequence('!',
						PrimaryExpression(), //
						set(new LogicalExpr(LogicalOperator.NOT, Collections
								.singletonList((Expression) lastValue())))), //
				Sequence('+', PrimaryExpression()), //
				Sequence('-', PrimaryExpression(), //
						set(new NegateExpr((Expression) lastValue()))), //
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

	public Rule BuiltInCall() {
		return Sequence(
				FirstOf(Sequence("STR", '(', Expression(), ')'), //
						Sequence("LANG", '(', Expression(), ')'), //
						Sequence("LANGMATCHES", '(', Expression(), ',',
								Expression(), ')'), //
						Sequence("DATATYPE", '(', Expression(), ')'), //
						Sequence("BOUND", '(', Var(), ')'), //
						Sequence("SAMETERM", '(', Expression(), ',',
								Expression(), ')'), //
						Sequence("ISIRI", '(', Expression(), ')'), //
						Sequence("ISURI", '(', Expression(), ')'), //
						Sequence("ISBLANK", '(', Expression(), ')'), //
						Sequence("ISLITERAL", '(', Expression(), ')'), //
						Sequence("REGEX", '(', Expression(), ',', Expression(),
								Optional(Sequence(',', Expression())), ')') //
				), //
				set(new BuiltInCall(text("F/S/"), toList(Expression.class,
						values("F/S/E"), values("F/S/V")))) //
		);
	}

	@SuppressWarnings("unchecked")
	public Rule IriRefOrFunction() {
		return Sequence(
				IriRef(), //
				Optional(Sequence(ArgList(), set(new FunctionCall(
						(Expression) UP(UP(value())),
						(List<Expression>) value("A"))))));
	}

	public Rule VAR1() {
		return Sequence(Ch('?'), VARNAME());
	}

	public Rule VAR2() {
		return Sequence(Ch('$'), VARNAME());
	}

	public Rule VARNAME() {
		return Sequence(
				FirstOf(PN_CHARS_U(), DIGIT()),
				ZeroOrMore(FirstOf(PN_CHARS_U(), DIGIT(), Ch('\u00B7'),
						CharRange('\u0300', '\u036F'),
						CharRange('\u203F', '\u2040'))), set(new Variable(
						text("F") + text("Z"))), WS());
	}

	@SuppressWarnings("unchecked")
	protected <T> List<T> toList(Class<T> elementType, List<?>... values) {
		List<T> list = new ArrayList<T>();
		for (List<?> valueList : values) {
			if (valueList != null) {
				for (Object value : valueList) {
					if (value != null) {
						if (!elementType.isAssignableFrom(value.getClass())) {
							throw new IllegalArgumentException(value
									+ " is not compatible with " + elementType);
						}
						list.add((T) value);
					}
				}
			}
		}
		return list;
	}

	@SuppressWarnings("unchecked")
	protected <T> List<T> toList(Class<T> elementType, Object firstValue,
			List<?>... values) {
		List<T> list = new ArrayList<T>();
		if (!elementType.isAssignableFrom(firstValue.getClass())) {
			throw new IllegalArgumentException(firstValue
					+ " is not compatible with " + elementType);
		}
		list.add((T) firstValue);
		for (List<?> valueList : values) {
			if (valueList != null) {
				for (Object value : valueList) {
					if (value != null) {
						if (!elementType.isAssignableFrom(value.getClass())) {
							throw new IllegalArgumentException(value
									+ " is not compatible with " + elementType);
						}
						list.add((T) value);
					}
				}
			}
		}
		return list;
	}

	@Override
	protected Rule FromStringLiteral(String string) {
		return Sequence(StringIgnoreCase(string), WS());
	}
}