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
import net.enilink.komma.parser.sparql.tree.visitor.Visitable;

public abstract class Literal implements GraphNode, Expression, Visitable {
	@Override
	public PropertyList getPropertyList() {
		return null;
	}

	public GraphNode copy(boolean copyProperties) {
		try {
			return (GraphNode) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException();
		}
	}
}