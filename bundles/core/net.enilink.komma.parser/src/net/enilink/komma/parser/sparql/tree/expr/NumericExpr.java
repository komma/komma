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
package net.enilink.komma.parser.sparql.tree.expr;

import net.enilink.komma.parser.sparql.tree.visitor.Visitor;

public class NumericExpr extends BinaryExpr {
	protected NumericOperator operator;

	public NumericExpr(NumericOperator operator, Expression left,
			Expression right) {
		super(left, right);

		this.operator = operator;
	}

	public NumericOperator getOperator() {
		return operator;
	}
	
	@Override
	public <R, T> R accept(Visitor<R, T> visitor, T data) {
		return visitor.numericExpr(this, data);
	}
}
