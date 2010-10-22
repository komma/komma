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

import java.util.Collection;

import org.infinispan.tree.Fqn;
import org.infinispan.tree.TreeCache;
import net.enilink.composition.cache.IPropertyCache;
import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;

import com.google.inject.Inject;

import net.enilink.komma.repository.change.IRepositoryChange;
import net.enilink.komma.repository.change.IRepositoryChangeListener;
import net.enilink.komma.repository.change.IRepositoryChangeTracker;
import net.enilink.komma.repository.change.IStatementChange;
import net.enilink.komma.core.IEntityDecorator;
import net.enilink.komma.sesame.ISesameEntity;

public class CachingSesameManager extends DecoratingSesameManager implements
		IRepositoryChangeListener {
	@Inject
	TreeCache<Object, Object> cache;

	IRepositoryChangeTracker changeTracker;

	@Inject
	IPropertyCache propertyCache;

	public CachingSesameManager(boolean injectManager,
			IEntityDecorator... decorators) {
		super(injectManager, decorators);
	}

	public CachingSesameManager(IEntityDecorator... decorators) {
		this(true, decorators);
	}

	@Override
	public void close() {
		if (cache != null) {
			cache.stop();
			cache = null;
		}
		changeTracker.removeChangeListener(this);
		super.close();
	}

	public ISesameEntity createBean(Resource resource, Collection<URI> types,
			boolean restrictTypes, Model model) {
		Object element = cache.get(Fqn.fromElements(resource), "");
		if (element != null) {
			if (model != null) {
				initializeBean((ISesameEntity) element, model);
			}
			return (ISesameEntity) element;
		}
		ISesameEntity entity = super.createBean(resource, types, restrictTypes,
				model);
		if (! restrictTypes) {
			cache.put(Fqn.fromElements(resource), "", entity);
		}
		return entity;
	}

	@Override
	protected void initializeCache(ISesameEntity entity, Object property,
			Object value) {
		System.out.println("init cache for " + entity + "/" + property + ": "
				+ value);
		propertyCache.put(entity, property, new Object[0], value);
	}

	@Override
	public void refresh(Object entity) {
		super.refresh(entity);
		cache.removeNode(Fqn.fromElements(entity));
	}

	@Override
	public void repositoryChanged(IRepositoryChange... changes) {
		for (IRepositoryChange change : changes) {
			if (change instanceof IStatementChange) {
				IStatementChange stmtChange = (IStatementChange) change;
				cache.removeNode(Fqn.fromElements(stmtChange.getSubject()));
				cache.removeNode(Fqn.fromElements(stmtChange.getObject()));
			}
		}
	}

	@Inject
	protected void setChangeTracker(IRepositoryChangeTracker changeTracker) {
		this.changeTracker = changeTracker;
		changeTracker.addChangeListener(this);
	}
}
