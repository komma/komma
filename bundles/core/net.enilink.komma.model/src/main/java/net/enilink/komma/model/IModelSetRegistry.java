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
package net.enilink.komma.model;

import net.enilink.komma.core.URI;

/**
 * Registry interface for {@link IModelSet}s.
 */
public interface IModelSetRegistry {
	/**
	 * Adds a model set to this registry.
	 *
	 * @param modelSet The model set to add.
	 */
	void registerModelSet(IModelSet modelSet);

	/**
	 * Returns a previously created model set by uri.
	 *
	 * @param uri The uri of the model set.
	 * @return the model set with the given uri or <code>null</code> if no such model set exists
	 */
	IModelSet getModelSet(URI uri);

	/**
	 * Removes a model set from this registry.
	 *
	 * @param uri The uri of the model set.
	 */
	void unregisterModelSet(URI uri);
}
