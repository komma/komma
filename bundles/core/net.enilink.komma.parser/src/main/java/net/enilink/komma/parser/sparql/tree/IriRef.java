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

public class IriRef extends AbstractGraphNode implements Expression {
	protected String Iri;

	public IriRef(String Iri) {
		this.Iri = Iri;
	}

	public String getIri() {
		return Iri;
	}

	@Override
	public <R, T> R accept(Visitor<R, T> visitor, T data) {
		return visitor.iriRef(this, data);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((Iri == null) ? 0 : Iri.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof IriRef))
			return false;
		IriRef other = (IriRef) obj;
		if (Iri == null) {
			if (other.Iri != null)
				return false;
		} else if (!Iri.equals(other.Iri))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return Iri;
	}
}