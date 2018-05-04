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
package net.enilink.komma.parser.sparql.tree;

import net.enilink.komma.parser.sparql.tree.expr.Expression;

public class OrderCondition {
	public static enum Direction {
		ASC, DESC
	};

	protected Direction direction;
	protected Expression expression;

	public OrderCondition(Direction direction, Expression expression) {
		this.direction = direction;
		this.expression = expression;
	}

	public Direction getDirection() {
		return direction;
	}

	public Expression getExpression() {
		return expression;
	}
}
