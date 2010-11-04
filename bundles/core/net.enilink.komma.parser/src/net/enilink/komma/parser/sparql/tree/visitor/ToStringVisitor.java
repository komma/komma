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
package net.enilink.komma.parser.sparql.tree.visitor;

import java.util.Iterator;
import java.util.List;

import net.enilink.vocab.rdf.RDF;
import net.enilink.komma.parser.sparql.SparqlParser;
import net.enilink.komma.parser.sparql.tree.BNode;
import net.enilink.komma.parser.sparql.tree.BNodePropertyList;
import net.enilink.komma.parser.sparql.tree.BasicGraphPattern;
import net.enilink.komma.parser.sparql.tree.BooleanLiteral;
import net.enilink.komma.parser.sparql.tree.Collection;
import net.enilink.komma.parser.sparql.tree.ConstructQuery;
import net.enilink.komma.parser.sparql.tree.Dataset;
import net.enilink.komma.parser.sparql.tree.DescribeQuery;
import net.enilink.komma.parser.sparql.tree.DoubleLiteral;
import net.enilink.komma.parser.sparql.tree.GenericLiteral;
import net.enilink.komma.parser.sparql.tree.Graph;
import net.enilink.komma.parser.sparql.tree.GraphNode;
import net.enilink.komma.parser.sparql.tree.GraphPattern;
import net.enilink.komma.parser.sparql.tree.IntegerLiteral;
import net.enilink.komma.parser.sparql.tree.IriRef;
import net.enilink.komma.parser.sparql.tree.LimitModifier;
import net.enilink.komma.parser.sparql.tree.NamedGraph;
import net.enilink.komma.parser.sparql.tree.OffsetModifier;
import net.enilink.komma.parser.sparql.tree.OptionalGraph;
import net.enilink.komma.parser.sparql.tree.OrderCondition;
import net.enilink.komma.parser.sparql.tree.OrderModifier;
import net.enilink.komma.parser.sparql.tree.PrefixDecl;
import net.enilink.komma.parser.sparql.tree.Prologue;
import net.enilink.komma.parser.sparql.tree.PropertyList;
import net.enilink.komma.parser.sparql.tree.PropertyPattern;
import net.enilink.komma.parser.sparql.tree.QName;
import net.enilink.komma.parser.sparql.tree.Query;
import net.enilink.komma.parser.sparql.tree.QueryWithSolutionModifier;
import net.enilink.komma.parser.sparql.tree.SelectQuery;
import net.enilink.komma.parser.sparql.tree.UnionGraph;
import net.enilink.komma.parser.sparql.tree.Variable;
import net.enilink.komma.parser.sparql.tree.expr.BuiltInCall;
import net.enilink.komma.parser.sparql.tree.expr.Expression;
import net.enilink.komma.parser.sparql.tree.expr.FunctionCall;
import net.enilink.komma.parser.sparql.tree.expr.LogicalExpr;
import net.enilink.komma.parser.sparql.tree.expr.LogicalOperator;
import net.enilink.komma.parser.sparql.tree.expr.NegateExpr;
import net.enilink.komma.parser.sparql.tree.expr.NumericExpr;
import net.enilink.komma.parser.sparql.tree.expr.RelationalExpr;

public class ToStringVisitor implements Visitor<StringBuilder, StringBuilder> {
	StringBuilder indent = new StringBuilder();

	protected String newLine() {
		return "\n" + indent.toString();
	}

	protected void indent() {
		indent.append("    ");
	}

	protected void dedent() {
		int start = Math.max(0, indent.length() - 4);
		int end = Math.min(indent.length(), start + 4);
		indent.replace(start, end, "");
	}

	protected void solutionModifiers(QueryWithSolutionModifier query,
			StringBuilder sb) {
		if (query.getLimitModifier() != null) {
			query.getLimitModifier().accept(this, sb);
		}
		if (query.getOffsetModifier() != null) {
			query.getOffsetModifier().accept(this, sb);
		}
		if (query.getOrderModifier() != null) {
			query.getOrderModifier().accept(this, sb);
		}
	}

	@Override
	public StringBuilder askQuery(Query askQuery, StringBuilder data) {
		askQuery.getPrologue().accept(this, data);
		data.append(newLine());
		data.append("ASK ");
		askQuery.getDataset().accept(this, data);
		askQuery.getGraph().accept(this, data);
		return data;
	}

	@Override
	public StringBuilder bNode(BNode bNode, StringBuilder data) {
		if (bNode.getLabel() != null) {
			data.append(bNode.getLabel());
		} else {
			data.append("[]");
		}
		return bNode.getPropertyList().accept(this, data);
	}

	@Override
	public StringBuilder bNodePropertyList(BNodePropertyList bNode,
			StringBuilder data) {
		data.append("[");
		bNode.getBNodePropertyList().accept(this, data);
		data.append("]");
		return data;
	}

	@Override
	public StringBuilder booleanLiteral(BooleanLiteral booleanLiteral,
			StringBuilder data) {
		return data.append(booleanLiteral.getValue() ? "true" : "true");
	}

	@Override
	public StringBuilder builtInCall(BuiltInCall builtinCall, StringBuilder data) {
		data.append(builtinCall.getName());
		data.append("(");
		printArgs(builtinCall.getArgs(), data);
		data.append(")");
		return data;
	}

	@Override
	public StringBuilder collection(Collection collection, StringBuilder data) {
		data.append("(");
		for (Iterator<GraphNode> it = collection.getElements().iterator(); it
				.hasNext();) {
			it.next().accept(this, data);

			if (it.hasNext()) {
				data.append(" ");
			}
		}
		data.append(")");

		collection.getPropertyList().accept(this, data);

		return data;
	}

	@Override
	public StringBuilder constructQuery(ConstructQuery constructQuery,
			StringBuilder data) {
		constructQuery.getPrologue().accept(this, data);
		data.append(newLine()).append("CONSTRUCT {");
		indent();
		for (Iterator<GraphNode> it = constructQuery.getTemplate().iterator(); it
				.hasNext();) {
			data.append(newLine());
			it.next().accept(this, data);

			if (it.hasNext()) {
				data.append(" . ");
			}

		}
		constructQuery.getDataset().accept(this, data);
		dedent();

		data.append(newLine()).append("} WHERE ");
		constructQuery.getGraph().accept(this, data);

		solutionModifiers(constructQuery, data);

		return data;
	}

	@Override
	public StringBuilder dataset(Dataset dataset, StringBuilder data) {
		for (Expression graph : dataset.getDefaultGraphs()) {
			data.append(newLine()).append("FROM ");
			graph.accept(this, data);
		}

		for (Expression graph : dataset.getNamedGraphs()) {
			data.append(newLine()).append("FROM NAMED ");
			graph.accept(this, data);
		}
		return data;
	}

	@Override
	public StringBuilder describeQuery(DescribeQuery describeQuery,
			StringBuilder data) {
		describeQuery.getPrologue().accept(this, data);
		data.append(newLine()).append("DESCRIBE ");
		if (describeQuery.getResources().isEmpty()) {
			data.append("* ");
		} else {
			for (GraphNode node : describeQuery.getResources()) {
				node.accept(this, data);

				data.append(" ");
			}
		}
		describeQuery.getDataset().accept(this, data);
		if (describeQuery.getGraph() != null) {
			data.append(newLine()).append("WHERE ");
			describeQuery.getGraph().accept(this, data);
		}

		solutionModifiers(describeQuery, data);

		return data;
	}

	@Override
	public StringBuilder doubleLiteral(DoubleLiteral doubleLiteral,
			StringBuilder data) {
		return data.append(doubleLiteral.getValue());
	}

	@Override
	public StringBuilder functionCall(FunctionCall functionCall,
			StringBuilder data) {
		functionCall.getName().accept(this, data);
		data.append("(");
		printArgs(functionCall.getArgs(), data);
		data.append(")");
		return data;
	}

	@Override
	public StringBuilder genericLiteral(GenericLiteral genericLiteral,
			StringBuilder data) {
		data.append("\"")
				.append(genericLiteral.getLabel().replaceAll("\"", "\\\""))
				.append("\"");
		if (genericLiteral.getLanguage() != null) {
			data.append("@").append(genericLiteral.getLanguage());
		} else if (genericLiteral.getDatatype() != null) {
			data.append("^^");
			genericLiteral.getDatatype().accept(this, data);
		}
		return data;
	}

	@Override
	public StringBuilder basicGraphPattern(BasicGraphPattern pattern,
			StringBuilder data) {
		for (Iterator<GraphNode> nodeIt = pattern.getNodes().iterator(); nodeIt
				.hasNext();) {
			data.append(newLine());
			nodeIt.next().accept(this, data);
			data.append(" . ");
		}

		return data;
	}

	protected char getLastChar(StringBuilder sb) {
		char[] chars = new char[1];
		if (sb.length() > 0) {
			sb.getChars(sb.length() - 1, sb.length(), chars, 0);
		}
		return chars[0];
	}

	@Override
	public StringBuilder graphPattern(GraphPattern graphPattern,
			StringBuilder data) {
		if (getLastChar(data) == '{') {
			data.append(newLine());
		}
		data.append("{");
		indent();

		for (Graph graph : graphPattern.getPatterns()) {
			graph.accept(this, data);
		}

		for (Expression filter : graphPattern.getFilters()) {
			data.append(newLine()).append("FILTER (");
			filter.accept(this, data);
			data.append(")");
		}

		dedent();

		data.append(newLine()).append("}");

		return data;
	}

	@Override
	public StringBuilder integerLiteral(IntegerLiteral numericLiteral,
			StringBuilder data) {
		return data.append(numericLiteral.getValue());
	}

	@Override
	public StringBuilder iriRef(IriRef iriRef, StringBuilder data) {
		if (SparqlParser.RDF_NIL.equals(iriRef.getIri())) {
			data.append("()");
		} else {
			data.append("<").append(iriRef.getIri()).append(">");
		}
		return iriRef.getPropertyList().accept(this, data);
	}

	@Override
	public StringBuilder limitModifier(LimitModifier limitModifier,
			StringBuilder data) {
		return data.append(newLine()).append("LIMIT ")
				.append(limitModifier.getLimit());
	}

	@Override
	public StringBuilder logicalExpr(LogicalExpr logicalExpr, StringBuilder data) {
		if (logicalExpr.getOperator() == LogicalOperator.NOT) {
			data.append(logicalExpr.getOperator().getSymbol()).append(" ");
		}
		for (Iterator<Expression> it = logicalExpr.getExprs().iterator(); it
				.hasNext();) {
			Expression expr = it.next();

			if (expr instanceof LogicalExpr
					&& logicalExpr.getOperator().ordinal() > ((LogicalExpr) expr)
							.getOperator().ordinal()) {
				data.append("(");
				expr.accept(this, data);
				data.append(")");
			} else {
				expr.accept(this, data);
			}

			if (it.hasNext()) {
				data.append(" ").append(logicalExpr.getOperator().getSymbol())
						.append(" ");
			}
		}
		return data;
	}

	@Override
	public StringBuilder namedGraph(NamedGraph namedGraph, StringBuilder data) {
		data.append("GRAPH ");
		namedGraph.getName().accept(this, data);
		return namedGraph.getGraph().accept(this, data);
	}

	@Override
	public StringBuilder negateExpr(NegateExpr negateExpr, StringBuilder data) {
		data.append("! ");
		return negateExpr.getExpr().accept(this, data);
	}

	@Override
	public StringBuilder numericExpr(NumericExpr numericExpr, StringBuilder data) {
		Expression left = numericExpr.getLeft();
		Expression right = numericExpr.getRight();

		if (left instanceof NumericExpr
				&& numericExpr.getOperator().hasPriorityOver(
						((NumericExpr) left).getOperator())) {
			data.append("(");
			left.accept(this, data);
			data.append(")");
		} else {
			left.accept(this, data);
		}

		data.append(numericExpr.getOperator().getSymbol());

		if (right instanceof NumericExpr
				&& numericExpr.getOperator().hasPriorityOver(
						((NumericExpr) right).getOperator())) {
			data.append("(");
			right.accept(this, data);
			data.append(")");
		} else {
			right.accept(this, data);
		}

		return data;
	}

	@Override
	public StringBuilder offsetModifier(OffsetModifier offsetModifier,
			StringBuilder data) {
		return data.append(newLine()).append("OFFSET ")
				.append(offsetModifier.getOffset());
	}

	@Override
	public StringBuilder optionalGraph(OptionalGraph optionalGraph,
			StringBuilder data) {
		data.append(newLine()).append("OPTIONAL ");
		optionalGraph.getGraph().accept(this, data);
		return data;
	}

	@Override
	public StringBuilder orderModifier(OrderModifier orderModifier,
			StringBuilder data) {
		data.append(newLine()).append("ORDER BY ");
		for (Iterator<OrderCondition> it = orderModifier.getOrderConditions()
				.iterator(); it.hasNext();) {
			OrderCondition condition = it.next();
			condition.getExpression().accept(this, data);

			switch (condition.getDirection()) {
			case DESC:
				data.append(" DESC");
			}

			if (it.hasNext()) {
				data.append(", ");
			}
		}
		return data;
	}

	protected void printArgs(List<Expression> args, StringBuilder sb) {
		for (Iterator<Expression> it = args.iterator(); it.hasNext();) {
			Expression arg = it.next();
			arg.accept(this, sb);

			if (it.hasNext()) {
				sb.append(", ");
			}
		}
	}

	@Override
	public StringBuilder prologue(Prologue prologue, StringBuilder data) {
		if (prologue.getBase() != null) {
			data.append(newLine()).append("BASE ");
			prologue.getBase().accept(this, data);
		}
		for (PrefixDecl prefixDecl : prologue.getPrefixDecls()) {
			data.append(newLine()).append("PREFIX ")
					.append(prefixDecl.getPrefix()).append(":");
			prefixDecl.getIri().accept(this, data);
		}
		return data;
	}

	@Override
	public StringBuilder propertyList(PropertyList propertyList,
			StringBuilder data) {
		GraphNode lastPredicate = null;
		for (Iterator<PropertyPattern> it = propertyList.iterator(); it
				.hasNext();) {
			PropertyPattern pattern = it.next();

			if (pattern.getPredicate() == lastPredicate) {
				data.append(", ");
			} else {
				data.append(" ");
				if (pattern.getPredicate() instanceof IriRef
						&& ((IriRef) pattern.getPredicate()).getIri().equals(
								RDF.PROPERTY_TYPE.toString())) {
					data.append("a");
				} else {
					pattern.getPredicate().accept(this, data);
				}
				data.append(" ");
			}
			pattern.getObject().accept(this, data);

			lastPredicate = pattern.getPredicate();
			if (it.hasNext()) {
				data.append("; ");
			}
		}
		return data;
	}

	@Override
	public StringBuilder qName(QName qName, StringBuilder data) {
		if (qName.getPrefix() != null) {
			data.append(qName.getPrefix());
		}
		data.append(":").append(
				qName.getLocalPart() != null ? qName.getLocalPart() : "");

		return qName.getPropertyList().accept(this, data);
	}

	@Override
	public StringBuilder relationalExpr(RelationalExpr relationalExpr,
			StringBuilder data) {
		Expression left = relationalExpr.getLeft();
		Expression right = relationalExpr.getRight();

		left.accept(this, data);
		data.append(relationalExpr.getOperator().getSymbol());
		right.accept(this, data);

		return data;
	}

	@Override
	public StringBuilder selectQuery(SelectQuery selectQuery, StringBuilder data) {
		selectQuery.getPrologue().accept(this, data);
		data.append(newLine());
		data.append("SELECT ");
		if (selectQuery.getModifier() != null) {
			switch (selectQuery.getModifier()) {
			case DISTINCT:
				data.append("DISTINCT ");
				break;
			case REDUCED:
				data.append("REDUCED ");
				break;
			}
		}
		if (selectQuery.getProjection().isEmpty()) {
			data.append("* ");
		} else {
			for (Variable var : selectQuery.getProjection()) {
				var.accept(this, data);

				data.append(" ");
			}
		}

		selectQuery.getDataset().accept(this, data);
		data.append(newLine());
		data.append("WHERE ");
		selectQuery.getGraph().accept(this, data);

		solutionModifiers(selectQuery, data);

		return data;
	}

	@Override
	public StringBuilder unionGraph(UnionGraph unionGraph, StringBuilder data) {
		for (Iterator<Graph> it = unionGraph.getGraphs().iterator(); it
				.hasNext();) {
			Graph graph = it.next();
			graph.accept(this, data);

			if (it.hasNext()) {
				data.append(" UNION ");
			}
		}
		return data;
	}

	@Override
	public StringBuilder variable(Variable variable, StringBuilder data) {
		data.append("?").append(variable.getName());
		return variable.getPropertyList().accept(this, data);
	}

}
