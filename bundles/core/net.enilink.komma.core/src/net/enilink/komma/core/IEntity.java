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

public interface IEntity extends IReference, IReferenceable {
	/**
	 * View this entity as an instance of the given concept.
	 * 
	 * @return This entity or a new instance that implements the given concept.
	 * 
	 * @see IEntityManager#find(IReference, Class, Class...)
	 */
	<T> T as(Class<T> concept);

	/**
	 * The associated entity manager of this entity.
	 * 
	 * @return The entity manager
	 */
	IEntityManager getEntityManager();

	/**
	 * Flushes the local state of the entity forcing it to refresh.
	 * 
	 */
	void refresh();
}
