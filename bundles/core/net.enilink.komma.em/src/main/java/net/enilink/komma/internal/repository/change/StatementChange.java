/*******************************************************************************
 * Copyright (c) 2009, 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.internal.repository.change;

import java.util.Arrays;

import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;

import net.enilink.komma.repository.change.IStatementChange;

public abstract class StatementChange implements IStatementChange {
	private Resource[] ctx;

	private Value obj;

	private URI pred;

	private Resource subj;

	public StatementChange(Resource subj, URI pred, Value obj, Resource... ctx) {
		this.subj = subj;
		this.pred = pred;
		this.obj = obj;
		this.ctx = ctx;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StatementChange other = (StatementChange) obj;
		if (!Arrays.equals(ctx, other.ctx))
			return false;
		if (this.obj == null) {
			if (other.obj != null)
				return false;
		} else if (!this.obj.equals(other.obj))
			return false;
		if (pred == null) {
			if (other.pred != null)
				return false;
		} else if (!pred.equals(other.pred))
			return false;
		if (subj == null) {
			if (other.subj != null)
				return false;
		} else if (!subj.equals(other.subj))
			return false;
		return true;
	}

	public Resource[] getContexts() {
		return ctx;
	}

	public Value getObject() {
		return obj;
	}

	public URI getPredicate() {
		return pred;
	}

	public Resource getSubject() {
		return subj;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(ctx);
		result = prime * result + ((obj == null) ? 0 : obj.hashCode());
		result = prime * result + ((pred == null) ? 0 : pred.hashCode());
		result = prime * result + ((subj == null) ? 0 : subj.hashCode());
		return result;
	}

	@Override
	public String toString() {
		return new StringBuilder("[").append(getSubject()).append(", ").append(
				getPredicate()).append(", ").append(getObject()).append("]")
				.toString();
	}
}
