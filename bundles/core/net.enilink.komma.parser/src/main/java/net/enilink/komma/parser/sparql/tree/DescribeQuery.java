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

public class DescribeQuery extends QueryWithSolutionModifier {
	protected List<GraphNode> resources;

	public DescribeQuery(List<GraphNode> resources, Dataset dataset,
			Graph graph,
			java.util.Collection<? extends SolutionModifier> modifiers) {
		super(dataset, graph, modifiers.toArray(new SolutionModifier[modifiers
				.size()]));
		this.resources = resources;
	}

	public List<GraphNode> getResources() {
		return resources;
	}

	@Override
	public <R, T> R accept(Visitor<R, T> visitor, T data) {
		return visitor.describeQuery(this, data);
	}
}
