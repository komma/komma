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
package net.enilink.komma.internal.sesame;

import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArraySet;

import org.openrdf.model.Resource;

import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IEntityDecorator;
import net.enilink.komma.sesame.ISesameEntity;

public class DecoratingSesameManager extends AbstractSesameManager {
	private CopyOnWriteArraySet<IEntityDecorator> decorators;

	public DecoratingSesameManager(boolean injectManager,
			IEntityDecorator... decorators) {
		this.decorators = new CopyOnWriteArraySet<IEntityDecorator>(Arrays
				.asList(decorators));

		if (injectManager) {
			this.decorators.add(new IEntityDecorator() {
				@Override
				public void decorate(IEntity entity) {
					((ISesameManagerAware) entity)
							.initSesameManager(DecoratingSesameManager.this);
				}
			});
		}
	}

	public DecoratingSesameManager(IEntityDecorator... decorators) {
		this(true, decorators);
	}

	@Override
	public void addDecorator(IEntityDecorator decorator) {
		decorators.add(decorator);
	}

	public <T> T decorate(T entity) {
		if (entity instanceof IEntity) {
			for (IEntityDecorator decorator : decorators) {
				decorator.decorate((IEntity) entity);
			}
		}
		return entity;
	}

	@Override
	protected ISesameEntity createBeanForClass(Resource resource, Class<?> type) {
		ISesameEntity bean = super.createBeanForClass(resource, type);
		return decorate(bean);
	}

	@Override
	public void removeDecorator(IEntityDecorator decorator) {
		decorators.remove(decorator);
	}

	@Override
	public boolean hasDecorator(IEntityDecorator decorator) {
		return decorators.contains(decorator);
	}
}