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
package net.enilink.komma.internal.model.event;

import net.enilink.komma.common.notify.INotification;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.model.event.IStatementNotification;
import net.enilink.komma.core.IReference;

public class StatementNotification implements IStatementNotification {
	private boolean add;

	private IReference ctx;

	private IModelSet modelSet;

	private Object obj;

	private IReference pred;

	private IReference subj;

	public StatementNotification(IModelSet modelSet, boolean add,
			IReference subj, IReference pred, Object obj, IReference ctx) {
		this.modelSet = modelSet;
		this.add = add;
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
		StatementNotification other = (StatementNotification) obj;
		if (add != other.add)
			return false;
		if (ctx == null) {
			if (other.ctx != null)
				return false;
		} else if (!ctx.equals(other.ctx))
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

	public IReference getContext() {
		return ctx;
	}

	public IModelSet getModelSet() {
		return modelSet;
	}

	public Object getObject() {
		return obj;
	}

	public IReference getPredicate() {
		return pred;
	}

	public IReference getSubject() {
		return subj;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (add ? 1231 : 1237);
		result = prime * result + ((ctx == null) ? 0 : ctx.hashCode());
		result = prime * result + ((obj == null) ? 0 : obj.hashCode());
		result = prime * result + ((pred == null) ? 0 : pred.hashCode());
		result = prime * result + ((subj == null) ? 0 : subj.hashCode());
		return result;
	}

	@Override
	public boolean isAdd() {
		return add;
	}

	@Override
	public boolean merge(INotification notification) {
		return false;
	}

	@Override
	public String toString() {
		return new StringBuilder("[").append(getSubject()).append(", ").append(
				getPredicate()).append(", ").append(getObject()).append("]")
				.toString();
	}
}
