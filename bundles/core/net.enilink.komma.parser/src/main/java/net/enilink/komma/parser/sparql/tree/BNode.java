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
import net.enilink.komma.parser.sparql.tree.visitor.Visitor;

public class BNode extends AbstractGraphNode implements Expression {
	protected String label;

	public BNode() {
		this(null);
	}

	public BNode(String label) {
		this.label = label != null && label.trim().length() == 0 ? null : label;
	}

	public String getLabel() {
		return label;
	}

	@Override
	public <R, T> R accept(Visitor<R, T> visitor, T data) {
		return visitor.bNode(this, data);
	}

}
