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

public class StatementPattern implements IStatementPattern {
	private IReference context;
	private Object obj;
	private IReference pred;
	private IReference subj;

	public StatementPattern(IReference subj, IReference pred, Object obj) {
		this(subj, pred, obj, null);
	}

	public StatementPattern(IReference subj, IReference pred, Object obj,
			IReference context) {
		this.subj = subj;
		this.pred = pred;
		this.obj = obj;
		this.context = context;
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
	public boolean equalsIgnoreContext(IStatementPattern other) {
		return Statements.equalsIgnoreContext(this, other);
	}

	@Override
	public boolean equals(Object obj) {
		return Statements.equals(this, obj);
	}

	@Override
	public int hashCode() {
		return Statements.hashCode(this);
	}

	@Override
	public String toString() {
		return Statements.toString(this);
	}
}
