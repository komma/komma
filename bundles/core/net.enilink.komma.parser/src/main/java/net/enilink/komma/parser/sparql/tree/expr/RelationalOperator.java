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

public enum RelationalOperator {
	EQUAL("="), NOT_EQUAL("!="), LESS("<"), GREATER(">"), LESS_EQUAL("<="), GREATER_EQUAL(
			">=");
	private String symbol;

	private RelationalOperator(String symbol) {
		this.symbol = symbol;
	}
	
	public static RelationalOperator fromSymbol(String symbol) {
		for (RelationalOperator op : RelationalOperator.values()) {
			if (symbol.equals(op.getSymbol())) {
				return op;
			}
		}
		return null;
	}
	
	public String getSymbol() {
		return symbol;
	}
}
