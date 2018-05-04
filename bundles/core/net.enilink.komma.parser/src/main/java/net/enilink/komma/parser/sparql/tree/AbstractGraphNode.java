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

public abstract class AbstractGraphNode implements GraphNode, Cloneable {
	private PropertyList propertyList = PropertyList.EMPTY_LIST;

	public PropertyList getPropertyList() {
		return propertyList;
	}

	public void setPropertyList(PropertyList propertyList) {
		this.propertyList = propertyList;
	}

	public GraphNode copy(boolean copyProperties) {
		try {
			AbstractGraphNode copy = (AbstractGraphNode) super.clone();

			copy.setPropertyList(copyProperties ? propertyList.copy()
					: PropertyList.EMPTY_LIST);

			return copy;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException();
		}
	}
}
