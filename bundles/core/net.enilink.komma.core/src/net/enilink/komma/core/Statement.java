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

public class Statement extends StatementBase implements IStatement,
		Iterable<IStatement> {
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
	public boolean isInferred() {
		return inferred;
	}

	@Override
	public Iterator<IStatement> iterator() {
		return Collections.<IStatement> singleton(this).iterator();
	}
}
