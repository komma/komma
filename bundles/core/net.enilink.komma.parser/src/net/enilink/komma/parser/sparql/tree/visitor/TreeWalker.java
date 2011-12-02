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

import java.util.List;

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
import net.enilink.komma.parser.sparql.tree.expr.GraphPatternExpr;
import net.enilink.komma.parser.sparql.tree.expr.LogicalExpr;
import net.enilink.komma.parser.sparql.tree.expr.NegateExpr;
import net.enilink.komma.parser.sparql.tree.expr.NumericExpr;
import net.enilink.komma.parser.sparql.tree.expr.RelationalExpr;

public class TreeWalker<T> implements Visitor<Boolean, T> {
	protected boolean solutionModifiers(QueryWithSolutionModifier query, T sb) {
		if (query.getLimitModifier() != null) {
			if (!query.getLimitModifier().accept(this, sb)) {
				return false;
			}
		}
		if (query.getOffsetModifier() != null) {
			if (!query.getOffsetModifier().accept(this, sb)) {
				return false;
			}
		}
		if (query.getOrderModifier() != null) {
			if (!query.getOrderModifier().accept(this, sb)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public Boolean askQuery(Query askQuery, T data) {
		return askQuery.getPrologue().accept(this, data)
				&& askQuery.getDataset().accept(this, data)
				&& askQuery.getGraph().accept(this, data);
	}

	@Override
	public Boolean bNode(BNode bNode, T data) {
		return bNode.getPropertyList().accept(this, data);
	}

	@Override
	public Boolean bNodePropertyList(BNodePropertyList bNode, T data) {
		bNode.getBNodePropertyList().accept(this, data);
		return bNode.getPropertyList().accept(this, data);
	}

	@Override
	public Boolean booleanLiteral(BooleanLiteral booleanLiteral, T data) {
		return true;
	}

	@Override
	public Boolean builtInCall(BuiltInCall builtinCall, T data) {
		return args(builtinCall.getArgs(), data);
	}

	@Override
	public Boolean collection(Collection collection, T data) {
		for (GraphNode node : collection.getElements()) {
			if (!node.accept(this, data)) {
				return false;
			}
		}
		return collection.getPropertyList().accept(this, data);
	}

	@Override
	public Boolean constructQuery(ConstructQuery constructQuery, T data) {
		if (!constructQuery.getPrologue().accept(this, data)) {
			return false;
		}
		for (GraphNode node : constructQuery.getTemplate()) {
			if (!node.accept(this, data)) {
				return false;
			}
		}
		return constructQuery.getDataset().accept(this, data)
				&& constructQuery.getGraph().accept(this, data)
				&& solutionModifiers(constructQuery, data);
	}

	@Override
	public Boolean dataset(Dataset dataset, T data) {
		for (Expression graph : dataset.getDefaultGraphs()) {
			if (!graph.accept(this, data)) {
				return false;
			}
		}

		for (Expression graph : dataset.getNamedGraphs()) {
			if (!graph.accept(this, data)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public Boolean describeQuery(DescribeQuery describeQuery, T data) {
		if (!describeQuery.getPrologue().accept(this, data)) {
			return false;
		}
		for (GraphNode node : describeQuery.getResources()) {
			if (!node.accept(this, data)) {
				return false;
			}
		}
		if (!describeQuery.getDataset().accept(this, data)) {
			return false;
		}
		if (describeQuery.getGraph() != null) {
			if (!describeQuery.getGraph().accept(this, data)) {
				return false;
			}
		}

		return solutionModifiers(describeQuery, data);
	}

	@Override
	public Boolean doubleLiteral(DoubleLiteral doubleLiteral, T data) {
		return true;
	}

	@Override
	public Boolean functionCall(FunctionCall functionCall, T data) {
		return functionCall.getName().accept(this, data)
				&& args(functionCall.getArgs(), data);
	}

	@Override
	public Boolean genericLiteral(GenericLiteral genericLiteral, T data) {
		return true;
	}

	@Override
	public Boolean basicGraphPattern(BasicGraphPattern pattern, T data) {
		for (GraphNode node : pattern.getNodes()) {
			if (!node.accept(this, data)) {
				return false;
			}
		}

		return true;
	}

	@Override
	public Boolean graphPattern(GraphPattern graphPattern, T data) {
		for (Graph graph : graphPattern.getPatterns()) {
			if (!graph.accept(this, data)) {
				return false;
			}
		}

		for (Expression filter : graphPattern.getFilters()) {
			if (!filter.accept(this, data)) {
				return false;
			}
		}

		return true;
	}

	@Override
	public Boolean integerLiteral(IntegerLiteral numericLiteral, T data) {
		return true;
	}

	@Override
	public Boolean iriRef(IriRef iriRef, T data) {
		return iriRef.getPropertyList().accept(this, data);
	}

	@Override
	public Boolean limitModifier(LimitModifier limitModifier, T data) {
		return true;
	}

	@Override
	public Boolean logicalExpr(LogicalExpr logicalExpr, T data) {
		for (Expression expr : logicalExpr.getExprs()) {
			if (!expr.accept(this, data)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public Boolean namedGraph(NamedGraph namedGraph, T data) {
		return namedGraph.getName().accept(this, data)
				&& namedGraph.getGraph().accept(this, data);
	}

	@Override
	public Boolean negateExpr(NegateExpr negateExpr, T data) {
		return negateExpr.getExpr().accept(this, data);
	}

	@Override
	public Boolean numericExpr(NumericExpr numericExpr, T data) {
		return numericExpr.getLeft().accept(this, data)
				&& numericExpr.getRight().accept(this, data);
	}

	@Override
	public Boolean offsetModifier(OffsetModifier offsetModifier, T data) {
		return true;
	}

	@Override
	public Boolean optionalGraph(OptionalGraph optionalGraph, T data) {
		return optionalGraph.getGraph().accept(this, data);
	}

	@Override
	public Boolean orderModifier(OrderModifier orderModifier, T data) {
		for (OrderCondition condition : orderModifier.getOrderConditions()) {
			if (!condition.getExpression().accept(this, data)) {
				return false;
			}
		}
		return true;
	}

	protected boolean args(List<Expression> args, T sb) {
		for (Expression arg : args) {
			if (!arg.accept(this, sb)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public Boolean prologue(Prologue prologue, T data) {
		if (prologue.getBase() != null) {
			if (!prologue.getBase().accept(this, data)) {
				return false;
			}
		}
		for (PrefixDecl prefixDecl : prologue.getPrefixDecls()) {
			if (!prefixDecl.getIri().accept(this, data)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public Boolean propertyList(PropertyList propertyList, T data) {
		for (PropertyPattern pattern : propertyList) {
			if (!(pattern.getPredicate().accept(this, data) && pattern
					.getObject().accept(this, data))) {
				return false;
			}
		}
		return true;
	}

	@Override
	public Boolean qName(QName qName, T data) {
		return qName.getPropertyList().accept(this, data);
	}

	@Override
	public Boolean relationalExpr(RelationalExpr relationalExpr, T data) {
		return relationalExpr.getLeft().accept(this, data)
				&& relationalExpr.getRight().accept(this, data);
	}

	@Override
	public Boolean selectQuery(SelectQuery selectQuery, T data) {
		if (!selectQuery.getPrologue().accept(this, data)) {
			return false;
		}
		for (Variable var : selectQuery.getProjection()) {
			if (!var.accept(this, data)) {
				return false;
			}
		}

		return selectQuery.getDataset().accept(this, data)
				&& selectQuery.getGraph().accept(this, data)
				&& solutionModifiers(selectQuery, data);
	}

	@Override
	public Boolean unionGraph(UnionGraph unionGraph, T data) {
		for (Graph graph : unionGraph.getGraphs()) {
			if (!graph.accept(this, data)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public Boolean variable(Variable variable, T data) {
		return variable.getPropertyList().accept(this, data);
	}

	public Boolean graphPatternExpr(GraphPatternExpr graphPatternExpr, T data) {
		return graphPatternExpr.getPattern().accept(this, data);
	}
}
