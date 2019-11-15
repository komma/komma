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
package net.enilink.komma.parser.sparql.tree;

import java.util.List;

import net.enilink.komma.parser.sparql.tree.visitor.Visitor;

public class OrderModifier extends SolutionModifier {
	protected List<OrderCondition> orderConditions;

	public OrderModifier(List<OrderCondition> orderConditions) {
		this.orderConditions = orderConditions;
	}

	public List<OrderCondition> getOrderConditions() {
		return orderConditions;
	}

	@Override
	public <R, T> R accept(Visitor<R, T> visitor, T data) {
		return visitor.orderModifier(this, data);
	}
}
