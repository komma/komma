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
