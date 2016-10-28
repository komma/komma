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
import net.enilink.komma.parser.sparql.tree.GraphPattern;
import net.enilink.komma.parser.sparql.tree.IntegerLiteral;
import net.enilink.komma.parser.sparql.tree.IriRef;
import net.enilink.komma.parser.sparql.tree.LimitModifier;
import net.enilink.komma.parser.sparql.tree.MinusGraph;
import net.enilink.komma.parser.sparql.tree.NamedGraph;
import net.enilink.komma.parser.sparql.tree.OffsetModifier;
import net.enilink.komma.parser.sparql.tree.OptionalGraph;
import net.enilink.komma.parser.sparql.tree.OrderModifier;
import net.enilink.komma.parser.sparql.tree.Prologue;
import net.enilink.komma.parser.sparql.tree.PropertyList;
import net.enilink.komma.parser.sparql.tree.QName;
import net.enilink.komma.parser.sparql.tree.Query;
import net.enilink.komma.parser.sparql.tree.SelectQuery;
import net.enilink.komma.parser.sparql.tree.SimplePropertyPath;
import net.enilink.komma.parser.sparql.tree.UnionGraph;
import net.enilink.komma.parser.sparql.tree.Variable;
import net.enilink.komma.parser.sparql.tree.expr.BuiltInCall;
import net.enilink.komma.parser.sparql.tree.expr.FunctionCall;
import net.enilink.komma.parser.sparql.tree.expr.GraphPatternExpr;
import net.enilink.komma.parser.sparql.tree.expr.LogicalExpr;
import net.enilink.komma.parser.sparql.tree.expr.NegateExpr;
import net.enilink.komma.parser.sparql.tree.expr.NumericExpr;
import net.enilink.komma.parser.sparql.tree.expr.RelationalExpr;

public interface Visitor<R, T> {
	R askQuery(Query askQuery, T data);

	R basicGraphPattern(BasicGraphPattern nodePattern, T data);

	R bNode(BNode bNode, T data);

	R bNodePropertyList(BNodePropertyList bNode, T data);

	R booleanLiteral(BooleanLiteral booleanLiteral, T data);

	R builtInCall(BuiltInCall builtinCall, T data);

	R collection(Collection collection, T data);

	R constructQuery(ConstructQuery constructQuery, T data);

	R dataset(Dataset dataset, T data);

	R describeQuery(DescribeQuery describeQuery, T data);

	R doubleLiteral(DoubleLiteral doubleLiteral, T data);

	R functionCall(FunctionCall functionCall, T data);

	R genericLiteral(GenericLiteral genericLiteral, T data);

	R graphPattern(GraphPattern graphPattern, T data);

	R integerLiteral(IntegerLiteral numericLiteral, T data);

	R iriRef(IriRef iriRef, T data);

	R limitModifier(LimitModifier limitModifier, T data);

	R logicalExpr(LogicalExpr logicalExpr, T data);
	
	R minusGraph(MinusGraph minusGraph, T data);

	R namedGraph(NamedGraph namedGraph, T data);

	R negateExpr(NegateExpr negateExpr, T data);

	R numericExpr(NumericExpr numericExpr, T data);

	R offsetModifier(OffsetModifier offsetModifier, T data);

	R optionalGraph(OptionalGraph optionalGraph, T data);

	R orderModifier(OrderModifier orderModifier, T data);

	R prologue(Prologue prologue, T data);

	R propertyList(PropertyList propertyList, T data);

	R qName(QName qName, T data);

	R relationalExpr(RelationalExpr relationalExpr, T data);

	R selectQuery(SelectQuery selectQuery, T data);

	R unionGraph(UnionGraph unionGraph, T data);

	R variable(Variable variable, T data);

	R graphPatternExpr(GraphPatternExpr graphPatternExpr, T data);
	
	R simplePropertyPath(SimplePropertyPath propertyPath, T data);
}
