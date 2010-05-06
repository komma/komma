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

import java.util.List;

import net.enilink.komma.parser.sparql.tree.expr.Expression;
import net.enilink.komma.parser.sparql.tree.visitor.Visitor;

public class GraphPattern extends Graph {
	protected List<Graph> patterns;
	protected List<Expression> filters;

	public GraphPattern(List<Graph> patterns,
			List<Expression> filters) {
		this.patterns = patterns;
		this.filters = filters;
	}

	public List<Graph> getPatterns() {
		return patterns;
	}

	public List<Expression> getFilters() {
		return filters;
	}

	@Override
	public <R, T> R accept(Visitor<R, T> visitor, T data) {
		return visitor.graphPattern(this, data);
	}
}
