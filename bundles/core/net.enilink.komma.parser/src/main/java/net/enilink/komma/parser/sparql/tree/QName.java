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

import net.enilink.komma.parser.sparql.tree.expr.Expression;
import net.enilink.komma.parser.sparql.tree.visitor.Visitor;

public class QName extends AbstractGraphNode implements Expression {
	/**
	 * <p>
	 * local part of this <code>QName</code>.
	 * </p>
	 */
	protected final String localPart;

	/**
	 * <p>
	 * prefix of this <code>QName</code>.
	 * </p>
	 */
	protected final String prefix;

	public QName(String prefix, String localPart) {
		this.prefix = prefix;
		this.localPart = localPart;
	}
	
	public String getPrefix() {
		return prefix;
	}
	
	public String getLocalPart() {
		return localPart;
	}

	@Override
	public <R, T> R accept(Visitor<R, T> visitor, T data) {
		return visitor.qName(this, data);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((localPart == null) ? 0 : localPart.hashCode());
		result = prime * result + ((prefix == null) ? 0 : prefix.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof QName))
			return false;
		QName other = (QName) obj;
		if (localPart == null) {
			if (other.localPart != null)
				return false;
		} else if (!localPart.equals(other.localPart))
			return false;
		if (prefix == null) {
			if (other.prefix != null)
				return false;
		} else if (!prefix.equals(other.prefix))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return "{" + prefix + "}" + localPart;
	}
}
