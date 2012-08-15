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
package net.enilink.komma.em.internal;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import net.enilink.komma.em.internal.behaviours.IEntityManagerAware;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IEntityDecorator;
import net.enilink.komma.core.IReference;

public class DecoratingEntityManager extends AbstractEntityManager {
	private CopyOnWriteArraySet<IEntityDecorator> decorators;

	@Inject(optional = true)
	@Named("injectManager")
	private boolean injectManager = true;

	@Inject
	public DecoratingEntityManager(Set<IEntityDecorator> decorators) {
		this.decorators = new CopyOnWriteArraySet<IEntityDecorator>(decorators);
	}

	@Override
	public void addDecorator(IEntityDecorator decorator) {
		decorators.add(decorator);
	}

	@Override
	protected IEntity createBeanForClass(IReference resource, Class<?> type) {
		IEntity bean = super.createBeanForClass(resource, type);
		return decorate(bean);
	}

	public <T> T decorate(T entity) {
		if (entity instanceof IEntity) {
			if (injectManager) {
				((IEntityManagerAware) entity)
						.initEntityManager(DecoratingEntityManager.this);
			}
			for (IEntityDecorator decorator : decorators) {
				decorator.decorate((IEntity) entity);
			}
		}
		return entity;
	}

	@Override
	public boolean hasDecorator(IEntityDecorator decorator) {
		return decorators.contains(decorator);
	}

	@Override
	public void removeDecorator(IEntityDecorator decorator) {
		decorators.remove(decorator);
	}
}