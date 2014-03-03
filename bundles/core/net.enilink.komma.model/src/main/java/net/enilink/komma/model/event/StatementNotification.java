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
package net.enilink.komma.model.event;

import net.enilink.komma.common.notify.INotification;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.Statement;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IModelAware;
import net.enilink.komma.model.IModelSet;

public class StatementNotification implements IStatementNotification {
	private IStatement stmt;

	private boolean add;

	private IModelSet modelSet;

	public StatementNotification(boolean add, IStatement stmt) {
		this(null, add, stmt);
	}

	public StatementNotification(IModelSet modelSet, boolean add,
			IStatement stmt) {
		IReference subj = stmt.getSubject();
		Object obj = stmt.getObject();
		IReference ctx = stmt.getContext();
		if (modelSet == null) {
			IModel model;
			if (subj instanceof IModelAware) {
				model = ((IModelAware) subj).getModel();
			} else if (obj instanceof IModelAware) {
				model = ((IModelAware) obj).getModel();
			} else {
				throw new IllegalArgumentException(
						"The argument modelSet may not be null.");
			}
			modelSet = model.getModelSet();
			if (ctx == null) {
				ctx = model.getURI();
				stmt = new Statement(subj, stmt.getPredicate(), obj, ctx,
						stmt.isInferred());
			}
		}
		this.stmt = stmt;
		this.modelSet = modelSet;
		this.add = add;
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
		return stmt.equals(other.stmt);
	}

	public IReference getContext() {
		return stmt.getContext();
	}

	public IModelSet getModelSet() {
		return modelSet;
	}

	public Object getObject() {
		return stmt.getObject();
	}

	public IReference getPredicate() {
		return stmt.getPredicate();
	}

	public IReference getSubject() {
		return stmt.getSubject();
	}

	@Override
	public IStatement getStatement() {
		return stmt;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (add ? 1231 : 1237);
		result = prime * result + stmt.hashCode();
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
		return new StringBuilder("[").append(getSubject()).append(", ")
				.append(getPredicate()).append(", ").append(getObject())
				.append("]").toString();
	}
}
