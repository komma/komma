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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.google.inject.Inject;

import net.enilink.komma.em.internal.behaviours.IEntityManagerAware;
import net.enilink.komma.em.util.UnitOfWork;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IEntityDecorator;
import net.enilink.komma.core.IEntityManager;

public abstract class ThreadLocalEntityManager extends DelegatingEntityManager {
	private ThreadLocal<IEntityManager> delegate = new ThreadLocal<IEntityManager>();
	private IEntityDecorator managerInjector = new IEntityDecorator() {
		@Override
		public void decorate(IEntity entity) {
			((IEntityManagerAware) entity)
					.initEntityManager(ThreadLocalEntityManager.this);
		}
	};

	private List<IEntityDecorator> decorators = new CopyOnWriteArrayList<IEntityDecorator>();

	@Inject
	protected UnitOfWork uow;

	public void addDecorator(IEntityDecorator decorator) {
		decorators.add(decorator);
		if (delegate.get() != null) {
			delegate.get().addDecorator(decorator);
		}
	}

	@Override
	public void close() {
		IEntityManager manager = delegate.get();
		if (manager != null) {
			manager.close();
			delegate.remove();
		}
	}

	abstract protected IEntityManager initialValue();

	public IEntityManager getDelegate() {
		IEntityManager manager = delegate.get();
		if (manager == null || !manager.isOpen()) {
			manager = initialValue();
			for (IEntityDecorator decorator : decorators) {
				manager.addDecorator(decorator);
			}
			manager.addDecorator(managerInjector);
			uow.addCloseable(this);
			delegate.set(manager);
		}
		return manager;
	}

	@Override
	public boolean isOpen() {
		IEntityManager manager = delegate.get();
		return (manager != null) ? manager.isOpen() : false;
	}

	@Override
	public void removeDecorator(IEntityDecorator decorator) {
		decorators.remove(decorator);
		if (delegate.get() != null) {
			delegate.get().removeDecorator(decorator);
		}
	}
}