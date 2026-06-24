/*******************************************************************************
 * Copyright (c) 2026 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.em;

import net.enilink.komma.core.ITransaction;

public abstract class DelegatingTransaction implements ITransaction {
	@Override
	public void begin() {
		getDelegate().begin();
	}

	@Override
	public void commit() {
		getDelegate().commit();
	}

	@Override
	public boolean getRollbackOnly() {
		return getDelegate().getRollbackOnly();
	}

	abstract public ITransaction getDelegate();

	@Override
	public boolean isActive() {
		return getDelegate().isActive();
	}

	@Override
	public void rollback() {
		getDelegate().rollback();
	}

	@Override
	public void setRollbackOnly() {
		getDelegate().setRollbackOnly();
	}

	@Override
	public String toString() {
		return getDelegate().toString();
	}
}