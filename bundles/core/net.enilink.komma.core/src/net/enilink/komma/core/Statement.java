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
package net.enilink.komma.core;

import java.util.Collections;
import java.util.Iterator;

public class Statement implements IStatement, Iterable<IStatement> {
	private IReference context;
	private boolean inferred;
	private Object obj;
	private IReference pred;
	private IReference subj;

	public Statement(IReference subj, IReference pred, Object obj) {
		this(subj, pred, obj, null, false);
	}

	public Statement(IReference subj, IReference pred, Object obj,
			boolean inferred) {
		this(subj, pred, obj, null, inferred);
	}

	public Statement(IReference subj, IReference pred, Object obj,
			IReference context) {
		this(subj, pred, obj, context, false);
	}

	public Statement(IReference subj, IReference pred, Object obj,
			IReference context, boolean inferred) {
		this.subj = subj;
		this.pred = pred;
		this.obj = obj;
		this.context = context;
		this.inferred = inferred;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Statement))
			return false;
		Statement other = (Statement) obj;
		if (context == null) {
			if (other.context != null)
				return false;
		} else if (!context.equals(other.context))
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

	@Override
	public IReference getContext() {
		return context;
	}

	@Override
	public Object getObject() {
		return obj;
	}

	@Override
	public IReference getPredicate() {
		return pred;
	}

	@Override
	public IReference getSubject() {
		return subj;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((context == null) ? 0 : context.hashCode());
		result = prime * result + ((obj == null) ? 0 : obj.hashCode());
		result = prime * result + ((pred == null) ? 0 : pred.hashCode());
		result = prime * result + ((subj == null) ? 0 : subj.hashCode());
		return result;
	}

	@Override
	public boolean isInferred() {
		return inferred;
	}

	@Override
	public Iterator<IStatement> iterator() {
		return Collections.<IStatement> singleton(this).iterator();
	}

	@Override
	public String toString() {
		return new StringBuilder("[").append(getSubject()).append(", ")
				.append(getPredicate()).append(", ").append(getObject())
				.append("]").toString();
	}
}
