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
package net.enilink.komma.em.internal.behaviours;

import net.enilink.composition.traits.Behaviour;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IEntityManager;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.URI;

/**
 * Stores the reference and manager for a bean and implements equals, hashCode,
 * and toString.
 * 
 */
public abstract class EntitySupport implements IEntity, IEntityManagerAware,
		Behaviour<IEntity> {
	private IEntityManager manager;
	private IReference reference;

	@SuppressWarnings("unchecked")
	@Override
	public <T> T as(Class<T> concept) {
		if (concept.isAssignableFrom(getBehaviourDelegate().getClass())) {
			return (T) getBehaviourDelegate();
		}
		Object entity = getEntityManager().find(this, concept);
		if (concept.isAssignableFrom(entity.getClass())) {
			return (T) entity;
		}
		return null;
	}

	public IEntityManager getEntityManager() {
		return manager;
	}

	public URI getURI() {
		return reference.getURI();
	}

	@Override
	public void initEntityManager(IEntityManager manager) {
		this.manager = manager;
	}

	@Override
	public void initReference(IReference reference) {
		this.reference = reference;
	}

	public IReference getReference() {
		return reference;
	}

	public void refresh() {
		// do nothing
	}

	@Override
	public String toString() {
		return reference.toString();
	}

	@Override
	public int hashCode() {
		return reference.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return reference.equals(obj);
	}
}
