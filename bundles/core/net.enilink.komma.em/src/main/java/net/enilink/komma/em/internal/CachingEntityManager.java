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

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.infinispan.tree.Fqn;
import org.infinispan.tree.TreeCache;
import net.enilink.composition.cache.IPropertyCache;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import net.enilink.komma.dm.change.IDataChange;
import net.enilink.komma.dm.change.IDataChangeListener;
import net.enilink.komma.dm.change.IDataChangeTracker;
import net.enilink.komma.dm.change.IStatementChange;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IEntityDecorator;
import net.enilink.komma.core.IGraph;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.URI;

public class CachingEntityManager extends DecoratingEntityManager implements
		IDataChangeListener {
	@Inject
	TreeCache<Object, Object> cache;

	IDataChangeTracker changeTracker;

	@Inject
	IPropertyCache propertyCache;

	@Inject
	public CachingEntityManager(@Named("injectManager") boolean injectManager,
			Set<IEntityDecorator> decorators) {
		super(injectManager, decorators);
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

	public IEntity createBean(IReference resource, Collection<URI> types,
			boolean restrictTypes, IGraph graph) {
		Object element = cache.get(Fqn.fromElements(resource), "");
		if (element != null) {
			if (graph != null) {
				initializeBean((IEntity) element, graph);
			}
			return (IEntity) element;
		}
		IEntity entity = super
				.createBean(resource, types, restrictTypes, graph);
		if (!restrictTypes) {
			cache.put(Fqn.fromElements(resource), "", entity);
		}
		return entity;
	}

	@Override
	protected void initializeCache(IEntity entity, Object property, Object value) {
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
	public void dataChanged(List<IDataChange> changes) {
		for (IDataChange change : changes) {
			if (change instanceof IStatementChange) {
				IStatementChange stmtChange = (IStatementChange) change;
				cache.removeNode(Fqn.fromElements(stmtChange.getSubject()));
				cache.removeNode(Fqn.fromElements(stmtChange.getObject()));
			}
		}
	}

	@Inject
	protected void setChangeTracker(IDataChangeTracker changeTracker) {
		this.changeTracker = changeTracker;
		changeTracker.addChangeListener(this);
	}
}
