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

public class SelectQuery extends QueryWithSolutionModifier {
	public static enum Modifier {
		DISTINCT, REDUCED;
	}

	protected final Modifier modifier;
	protected final List<Variable> projection;

	public SelectQuery(Modifier modifier, List<Variable> projection,
			Dataset dataset, Graph graph,
			java.util.Collection<? extends SolutionModifier> modifiers) {
		super(dataset, graph, modifiers.toArray(new SolutionModifier[modifiers
				.size()]));
		this.modifier = modifier;
		this.projection = projection;
	}

	public List<Variable> getProjection() {
		return projection;
	}

	public Modifier getModifier() {
		return modifier;
	}

	@Override
	public <R, T> R accept(Visitor<R, T> visitor, T data) {
		return visitor.selectQuery(this, data);
	}
}
