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

public class BasicGraphPattern extends Graph {
	protected List<GraphNode> nodes;
	
	public BasicGraphPattern(List<GraphNode> nodes) {
		this.nodes = nodes;
	}
	
	public List<GraphNode> getNodes() {
		return nodes;
	}
	
	@Override
	public <R, T> R accept(Visitor<R, T> visitor, T data) {
		return visitor.basicGraphPattern(this, data);
	}
	
}
