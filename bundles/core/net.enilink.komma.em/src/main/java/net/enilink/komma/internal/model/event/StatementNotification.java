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

import java.util.Arrays;

import org.openrdf.model.Resource;

import net.enilink.komma.common.notify.INotification;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.model.event.IStatementNotification;
import net.enilink.komma.core.IReference;

public class StatementNotification implements IStatementNotification {
	private IModelSet modelSet;

	private boolean add;

	private Resource[] ctx;

	private Object obj;

	private IReference pred;

	private IReference subj;

	public StatementNotification(IModelSet modelSet, boolean add,
			IReference subj, IReference pred, Object obj, Resource... ctx) {
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
		if (!(obj instanceof IStatementNotification))
			return false;
		IStatementNotification other = (IStatementNotification) obj;
		if (!Arrays.equals(ctx, other.getContexts()))
			return false;
		if (this.obj == null) {
			if (other.getObject() != null)
				return false;
		} else if (!this.obj.equals(other.getObject()))
			return false;
		if (pred == null) {
			if (other.getPredicate() != null)
				return false;
		} else if (!pred.equals(other.getPredicate()))
			return false;
		if (subj == null) {
			if (other.getSubject() != null)
				return false;
		} else if (!subj.equals(other.getSubject()))
			return false;
		return true;
	}

	public Resource[] getContexts() {
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
		result = prime * result + Arrays.hashCode(ctx);
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
