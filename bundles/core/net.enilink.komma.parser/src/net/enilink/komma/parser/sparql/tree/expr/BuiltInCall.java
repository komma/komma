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

import java.util.List;

import net.enilink.komma.parser.sparql.tree.visitor.Visitor;

public class BuiltInCall extends CallExpr {
	protected String name;
	protected List<Expression> args;

	public BuiltInCall(String name, List<Expression> args) {
		this.name = name;
		this.args = args;
	}

	public String getName() {
		return name;
	}

	public List<Expression> getArgs() {
		return args;
	}

	@Override
	public <R, T> R accept(Visitor<R, T> visitor, T data) {
		return visitor.builtInCall(this, data);
	}
}
