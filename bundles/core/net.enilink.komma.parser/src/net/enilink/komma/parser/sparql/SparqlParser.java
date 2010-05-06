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
import org.parboiled.support.In;

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

	public Rule query() {
		return sequence(WS(), prologue(), firstOf(selectQuery(),
				constructQuery(), describeQuery(), askQuery()), setPrologue(
				(Prologue) VALUE("p"), (Query) LAST_VALUE()), eoi());
	}

	protected boolean setPrologue(Prologue prologue, Query query) {
		if (query != null) {
			query.setPrologue(prologue);
		}
		return true;
	}

	public Rule prologue() {
		return sequence(optional(baseDecl()), zeroOrMore(prefixDecl()),
				SET(new Prologue((IriRef) VALUE("o/b"), toList(
						PrefixDecl.class, VALUES("z/p")))));
	}

	public Rule baseDecl() {
		return sequence("BASE", IRI_REF());
	}

	public Rule prefixDecl() {
		return sequence("PREFIX", PNAME_NS(), IRI_REF(),
				SET(new PrefixDecl(stripColon(TEXT("PNAME_NS").trim()),
						(IriRef) VALUE("IRI_REF"))));
	}

	@SuppressWarnings("unchecked")
	public Rule selectQuery() {
		SelectQuery.Modifier modifier;
		Dataset dataset;
		return sequence(
				"SELECT",
				optional(firstOf(
						//
						sequence("DISTINCT",
								DO(modifier = SelectQuery.Modifier.DISTINCT)),
						sequence("REDUCED",
								DO(modifier = SelectQuery.Modifier.REDUCED)))
				//
				), firstOf(oneOrMore(var()), '*'), DO(dataset = new Dataset()),
				zeroOrMore(datasetClause(dataset)), whereClause(),
				solutionModifier(), //
				SET(new SelectQuery(modifier, toList(Variable.class,
						VALUES("f/o/v")), dataset,
						(Graph) VALUE("whereClause"),
						(List<SolutionModifier>) LAST_VALUE())) //
		);
	}

	@SuppressWarnings("unchecked")
	public Rule constructQuery() {
		Dataset dataset;
		return sequence("CONSTRUCT", constructTemplate(),
				DO(dataset = new Dataset()),
				zeroOrMore(datasetClause(dataset)), whereClause(),
				solutionModifier(), //
				SET(new ConstructQuery((List<GraphNode>) VALUE("c"), dataset,
						(Graph) VALUE("whereClause"),
						(List<SolutionModifier>) LAST_VALUE())) //
		);
	}

	@SuppressWarnings("unchecked")
	public Rule describeQuery() {
		Dataset dataset;
		return sequence("DESCRIBE", firstOf(oneOrMore(varOrIRIref()), '*'),
				DO(dataset = new Dataset()),
				zeroOrMore(datasetClause(dataset)), optional(whereClause()),
				solutionModifier(), //
				SET(new DescribeQuery(toList(GraphNode.class, VALUES("f/o/v")),
						dataset, (Graph) VALUE("o/w"),
						(List<SolutionModifier>) LAST_VALUE())) //
		);
	}

	public Rule askQuery() {
		Dataset dataset;
		return sequence("ASK", DO(dataset = new Dataset()),
				zeroOrMore(datasetClause(dataset)), whereClause(), //
				SET(new AskQuery(dataset, (Graph) VALUE("whereClause"))) //
		);
	}

	public Rule datasetClause(@In Dataset dataset) {
		return sequence("FROM", firstOf(//
				sequence(defaultGraphClause(), dataset
						.addDefaultGraph((Expression) LAST_VALUE())), //
				sequence(namedGraphClause(), dataset
						.addNamedGraph((Expression) LAST_VALUE()))));
	}

	public Rule defaultGraphClause() {
		return sourceSelector();
	}

	public Rule namedGraphClause() {
		return sequence("NAMED", sourceSelector());
	}

	public Rule sourceSelector() {
		return iriRef();
	}

	public Rule whereClause() {
		return sequence(optional("WHERE"), groupGraphPattern());
	}

	@SuppressWarnings("unchecked")
	public Rule solutionModifier() {
		return sequence(optional(orderClause()),
				optional(limitOffsetClauses()), SET(toList(
						SolutionModifier.class, VALUES("o/o"),
						(List<SolutionModifier>) VALUE("last:o/l"))));
	}

	public Rule limitOffsetClauses() {
		return firstOf(//
				sequence(limitClause(), optional(offsetClause()), SET(toList(
						SolutionModifier.class, VALUE("l"), VALUES("o/o")))), //
				sequence(offsetClause(), optional(limitClause()), SET(toList(
						SolutionModifier.class, VALUE("o"), VALUES("o/l")))));
	}

	public Rule orderClause() {
		return sequence(
				"ORDER",
				"BY",
				oneOrMore(orderCondition()),
				SET(new OrderModifier(toList(OrderCondition.class, VALUES("")))));
	}

	public Rule orderCondition() {
		return firstOf(
				sequence(firstOf("ASC", "DESC"), brackettedExpression(),
						SET(new OrderCondition("asc".equals(TEXT("f")
								.toLowerCase()) ? OrderCondition.Direction.ASC
								: OrderCondition.Direction.DESC,
								(Expression) LAST_VALUE()))), //
				sequence(firstOf(constraint(), var()),
						SET(new OrderCondition(OrderCondition.Direction.ASC,
								(Expression) LAST_VALUE()))));
	}

	public Rule limitClause() {
		return sequence("LIMIT", INTEGER(), //
				SET(new LimitModifier(((IntegerLiteral) VALUE()).getValue())) //
		);
	}

	public Rule offsetClause() {
		return sequence("OFFSET", INTEGER(), //
				SET(new OffsetModifier(((IntegerLiteral) VALUE()).getValue())) //
		);
	}

	@SuppressWarnings("unchecked")
	public Rule groupGraphPattern() {
		List<Graph> patterns;
		return sequence('{', DO(patterns = new ArrayList<Graph>()),
				optional(sequence(triplesBlock(), //
						patterns.add(new BasicGraphPattern(
								(List<GraphNode>) LAST_VALUE())))),
				zeroOrMore(sequence(firstOf(sequence(graphPatternNotTriples(),
						patterns.add((Graph) LAST_VALUE())), filter()),
						optional('.'), optional(sequence(triplesBlock(), //
								patterns.add(new BasicGraphPattern(
										(List<GraphNode>) LAST_VALUE())))))),
				'}', //
				SET(new GraphPattern(patterns, toList(Expression.class,
						VALUES("z/s/f/f")))));
	}

	@SuppressWarnings("unchecked")
	public Rule triplesBlock() {
		List<GraphNode> nodes;
		return sequence(triplesSameSubject(), // 
				DO(nodes = new ArrayList<GraphNode>()), nodes
						.add((GraphNode) LAST_VALUE()), // 
				optional(sequence('.', optional(sequence(triplesBlock(), nodes
						.addAll((List<GraphNode>) LAST_VALUE()))))), //
				SET(nodes));
	}

	public Rule graphPatternNotTriples() {
		return firstOf(optionalGraphPattern(), groupOrUnionGraphPattern(),
				graphGraphPattern());
	}

	public Rule optionalGraphPattern() {
		return sequence("OPTIONAL", groupGraphPattern(), SET(new OptionalGraph(
				(Graph) LAST_VALUE())));
	}

	public Rule graphGraphPattern() {
		return sequence(
				"GRAPH",
				varOrIRIref(),
				groupGraphPattern(),
				SET(new NamedGraph((GraphNode) VALUE("v"), (Graph) LAST_VALUE())));
	}

	public Rule groupOrUnionGraphPattern() {
		return sequence(groupGraphPattern(), sequence(zeroOrMore(sequence(
				"UNION", groupGraphPattern())), SET(new UnionGraph(toList(
				Graph.class, UP(VALUE("g")), VALUES("z/s"))))));
	}

	public Rule filter() {
		return sequence("FILTER", constraint());
	}

	public Rule constraint() {
		return firstOf(brackettedExpression(), builtInCall(), functionCall());
	}

	@SuppressWarnings("unchecked")
	public Rule functionCall() {
		return sequence(iriRef(), argList(), //
				SET(new FunctionCall((Expression) VALUE("i"),
						(List<Expression>) VALUE("a"))));
	}

	public Rule argList() {
		return firstOf(
				//
				sequence('(', ')', SET(Collections.emptyList())), //
				sequence('(',
						expression(),
						zeroOrMore(sequence(',', expression())),
						')', //
						SET(toList(Expression.class, VALUE("e"), VALUES("z/s")))) //
		);
	}

	public Rule constructTemplate() {
		return sequence('{', optional(constructTriples()), '}');
	}

	@SuppressWarnings("unchecked")
	public Rule constructTriples() {
		List<GraphNode> nodes;
		return sequence(triplesSameSubject(),
				DO(nodes = new ArrayList<GraphNode>()), //
				nodes.add((GraphNode) LAST_VALUE()), optional(sequence('.',
						optional(//
						sequence(constructTriples(), nodes
								.addAll((List<GraphNode>) LAST_VALUE()))//
						))), //
				SET(nodes));
	}

	public Rule triplesSameSubject() {
		return firstOf(//
				sequence(varOrTerm(), SET(),
						propertyListNotEmpty((AbstractGraphNode) LAST_VALUE())), // 
				sequence(triplesNode(), SET(),
						propertyList((AbstractGraphNode) LAST_VALUE())) //
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
				.equals(propertyList))
				&& subject instanceof AbstractGraphNode) {
			propertyList = new PropertyList();
			((AbstractGraphNode) subject).setPropertyList(propertyList);
		}
		return propertyList;
	}

	@SuppressWarnings("unchecked")
	public Rule propertyListNotEmpty(@In GraphNode subject) {
		PropertyList propertyList;
		return sequence(verb(),
				objectList(), //
				DO(propertyList = createPropertyList(subject)),
				addPropertyPatterns(propertyList, (GraphNode) VALUE("v"),
						(List<GraphNode>) VALUE("o")), //
				zeroOrMore(sequence(';', optional(sequence(verb(),
						objectList(), //
						addPropertyPatterns(propertyList,
								(GraphNode) VALUE("v"),
								(List<GraphNode>) VALUE("o")))))));
	}

	public Rule propertyList(@In GraphNode subject) {
		return optional(propertyListNotEmpty(subject));
	}

	public Rule objectList() {
		return sequence(object(), zeroOrMore(sequence(',', object())),
				SET(toList(GraphNode.class, VALUE("o"), VALUES("z/s/o"))));
	}

	public Rule object() {
		return graphNode();
	}

	public Rule verb() {
		return firstOf(varOrIRIref(), sequence('a', SET(new IriRef(RDF_TYPE))));
	}

	public Rule triplesNode() {
		return firstOf(collection(), blankNodePropertyList());
	}

	public Rule blankNodePropertyList() {
		return sequence('[', SET(new BNodePropertyList()),
				propertyListNotEmpty(UP((BNode) VALUE())), ']');
	}

	public Rule collection() {
		return sequence('(',
				oneOrMore(graphNode()), //
				SET(new Collection(toList(GraphNode.class, VALUES("o/g")))),
				')');
	}

	public Rule graphNode() {
		return firstOf(varOrTerm(), triplesNode());
	}

	public Rule varOrTerm() {
		return firstOf(var(), graphTerm());
	}

	public Rule varOrIRIref() {
		return firstOf(var(), iriRef());
	}

	public Rule var() {
		return firstOf(VAR1(), VAR2());
	}

	public Rule graphTerm() {
		return firstOf(iriRef(), rdfLiteral(), numericLiteral(),
				booleanLiteral(), blankNode(), sequence(sequence('(', ')'),
						SET(new IriRef(RDF_NIL))));
	}

	public Rule expression() {
		return conditionalOrExpression();
	}

	public Rule conditionalOrExpression() {
		return sequence(conditionalAndExpression(), zeroOrMore(sequence("||",
				conditionalAndExpression())), //
				SET(NODE("z/s") != null ? new LogicalExpr(LogicalOperator.OR,
						toList(Expression.class, VALUE("c"), VALUES("z/s")))
						: VALUE()));
	}

	public Rule conditionalAndExpression() {
		return sequence(valueLogical(), zeroOrMore(sequence("&&",
				valueLogical())), //
				SET(NODE("z/s") != null ? new LogicalExpr(LogicalOperator.AND,
						toList(Expression.class, VALUE("v"), VALUES("z/s")))
						: VALUE()));
	}

	public Rule valueLogical() {
		return relationalExpression();
	}

	public Rule relationalExpression() {
		return sequence(numericExpression(), //
				optional(sequence(firstOf(//
						sequence('=', numericExpression()), //
						sequence("!=", numericExpression()), //
						sequence('<', numericExpression()), //
						sequence('>', numericExpression()), //
						sequence("<=", numericExpression()), //
						sequence(">=", numericExpression()) //
						), //
						SET(new RelationalExpr(RelationalOperator
								.fromSymbol(TEXT("f/s/").trim()),
								UP(UP((Expression) VALUE("n"))),
								(Expression) VALUE("f/s/n"))))));
	}

	public Rule numericExpression() {
		return additiveExpression();
	}

	public Rule additiveExpression() {
		Expression expr;
		return sequence(multiplicativeExpression(),
				DO(expr = (Expression) LAST_VALUE()), //
				zeroOrMore(firstOf(//
						sequence('+', multiplicativeExpression(),
								DO(expr = new NumericExpr(NumericOperator.ADD,
										expr, (Expression) LAST_VALUE()))), //
						sequence('-', multiplicativeExpression(),
								DO(expr = new NumericExpr(NumericOperator.SUB,
										expr, (Expression) LAST_VALUE()))), //
						sequence(numericLiteralPositive(),
								DO(expr = new NumericExpr(NumericOperator.ADD,
										expr, (Expression) LAST_VALUE()))), //
						sequence(numericLiteralNegative(),
								DO(expr = new NumericExpr(NumericOperator.SUB,
										expr, ((NumericLiteral) LAST_VALUE())
												.negate()))))), SET(expr));
	}

	public Rule multiplicativeExpression() {
		Expression expr;
		return sequence(unaryExpression(),
				DO(expr = (Expression) LAST_VALUE()), //
				zeroOrMore(firstOf(//
						sequence('*', unaryExpression(),
								DO(expr = new NumericExpr(NumericOperator.MUL,
										expr, (Expression) LAST_VALUE()))), //
						sequence('/', unaryExpression(),
								DO(expr = new NumericExpr(NumericOperator.DIV,
										expr, (Expression) LAST_VALUE()))) //
				)), SET(expr));
	}

	public Rule unaryExpression() {
		return firstOf(sequence('!', primaryExpression(), //
				SET(new LogicalExpr(LogicalOperator.NOT, Collections
						.singletonList((Expression) LAST_VALUE())))), // 
				sequence('+', primaryExpression()), // 
				sequence('-', primaryExpression(), //
						SET(new NegateExpr((Expression) LAST_VALUE()))), // 
				primaryExpression());
	}

	public Rule primaryExpression() {
		return firstOf(brackettedExpression(), builtInCall(),
				iriRefOrFunction(), rdfLiteral(), numericLiteral(),
				booleanLiteral(), var());
	}

	public Rule brackettedExpression() {
		return sequence('(', expression(), ')');
	}

	public Rule builtInCall() {
		return sequence(
				firstOf(sequence("STR", '(', expression(), ')'), //
						sequence("LANG", '(', expression(), ')'), //
						sequence("LANGMATCHES", '(', expression(), ',',
								expression(), ')'), //
						sequence("DATATYPE", '(', expression(), ')'), //
						sequence("BOUND", '(', var(), ')'), //
						sequence("SAMETERM", '(', expression(), ',',
								expression(), ')'), //
						sequence("ISIRI", '(', expression(), ')'), //
						sequence("ISURI", '(', expression(), ')'), //
						sequence("ISBLANK", '(', expression(), ')'), //
						sequence("ISLITERAL", '(', expression(), ')'), //
						sequence("REGEX", '(', expression(), ',', expression(),
								optional(sequence(',', expression())), ')') //
				), //
				SET(new BuiltInCall(TEXT("f/s/"), toList(Expression.class,
						VALUES("f/s/e"), VALUES("f/s/v")))) //
		);
	}

	@SuppressWarnings("unchecked")
	public Rule iriRefOrFunction() {
		return sequence(iriRef(), //
				optional(sequence(argList(), SET(new FunctionCall(
						(Expression) UP(UP(VALUE())),
						(List<Expression>) VALUE("a"))))));
	}

	public Rule VAR1() {
		return sequence(ch('?'), VARNAME());
	}

	public Rule VAR2() {
		return sequence(ch('$'), VARNAME());
	}

	public Rule VARNAME() {
		return sequence(firstOf(PN_CHARS_U(), DIGIT()), zeroOrMore(firstOf(
				PN_CHARS_U(), DIGIT(), ch('\u00B7'), charRange('\u0300',
						'\u036F'), charRange('\u203F', '\u2040'))),
				SET(new Variable(TEXT("f") + TEXT("z"))), WS());
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
	protected Rule fromStringLiteral(String string) {
		return sequence(stringIgnoreCase(string), WS());
	}
}