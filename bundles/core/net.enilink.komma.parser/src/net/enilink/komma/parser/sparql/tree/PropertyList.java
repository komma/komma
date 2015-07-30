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

import java.util.ArrayList;

import net.enilink.komma.parser.sparql.tree.visitor.Visitable;
import net.enilink.komma.parser.sparql.tree.visitor.Visitor;

public class PropertyList extends ArrayList<PropertyPattern> implements
		Visitable {
	public static final PropertyList EMPTY_LIST = new PropertyList();

	private static final long serialVersionUID = 1L;

	public PropertyList copy() {
		PropertyList copy = new PropertyList();
		for (PropertyPattern pattern : this) {
			copy.add(pattern.copy());
		}
		return copy;
	}

	@Override
	public <R, T> R accept(Visitor<R, T> visitor, T data) {
		return visitor.propertyList(this, data);
	}
}
