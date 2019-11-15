/*******************************************************************************
 * Copyright (c) 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.parser.sparql.tree.expr;

import java.util.List;

import net.enilink.komma.parser.sparql.tree.visitor.Visitor;

public class LogicalExpr implements Expression {
	protected LogicalOperator operator;
	protected List<Expression> exprs;

	public LogicalExpr(LogicalOperator operator, List<Expression> exprs) {
		this.operator = operator;
		this.exprs = exprs;
	}

	public LogicalOperator getOperator() {
		return operator;
	}

	public List<Expression> getExprs() {
		return exprs;
	}

	@Override
	public <R, T> R accept(Visitor<R, T> visitor, T data) {
		return visitor.logicalExpr(this, data);
	}
}
