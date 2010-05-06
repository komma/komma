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

public enum NumericOperator {
	MUL("*"), DIV("/"), ADD("+"), SUB("-");
	String symbol;

	private NumericOperator(String symbol) {
		this.symbol = symbol;
	}
	
	public String getSymbol() {
		return symbol;
	}

	public boolean hasPriorityOver(NumericOperator other) {
		switch (this) {
		case MUL:
		case DIV:
			switch (other) {
			case ADD:
			case SUB:
				return true;
			}
		}
		return false;
	}
}
