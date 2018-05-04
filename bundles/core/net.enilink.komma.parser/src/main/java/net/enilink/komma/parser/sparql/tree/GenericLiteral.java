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

import net.enilink.komma.parser.sparql.tree.visitor.Visitor;

public class GenericLiteral extends Literal {
	protected String label;
	protected GraphNode datatype;
	protected String language;

	public GenericLiteral(String label, GraphNode datatype, String language) {
		this.label = label;
		this.datatype = datatype;
		this.language = language;
	}

	public String getLabel() {
		return label;
	}

	public GraphNode getDatatype() {
		return datatype;
	}

	public String getLanguage() {
		return language;
	}

	@Override
	public <R, T> R accept(Visitor<R, T> visitor, T data) {
		return visitor.genericLiteral(this, data);
	}
}
