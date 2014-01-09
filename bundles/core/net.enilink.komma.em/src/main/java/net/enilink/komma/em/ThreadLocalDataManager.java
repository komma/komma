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
package net.enilink.komma.em;

import com.google.inject.Inject;

import net.enilink.komma.dm.IDataManager;
import net.enilink.komma.dm.IDataManagerFactory;
import net.enilink.komma.em.util.UnitOfWork;
import net.enilink.komma.core.KommaException;

public class ThreadLocalDataManager extends DelegatingDataManager {
	@Inject
	protected IDataManagerFactory dmFactory;

	@Inject
	protected UnitOfWork uow;

	private ThreadLocal<IDataManager> delegate = new ThreadLocal<IDataManager>();

	@Override
	public void close() {
		IDataManager manager = delegate.get();
		if (manager != null) {
			manager.close();
			delegate.remove();
		}
	}

	@Override
	public IDataManager getDelegate() {
		IDataManager manager = delegate.get();
		if (manager == null || !manager.isOpen()) {
			if (!uow.isActive()) {
				throw new KommaException("No active unit of work found.");
			}
			manager = dmFactory.get();
			uow.addManager(manager);
			delegate.set(manager);
		}
		return manager;
	}

	@Override
	public boolean isOpen() {
		IDataManager manager = delegate.get();
		return (manager != null) ? manager.isOpen() : false;
	}
}
