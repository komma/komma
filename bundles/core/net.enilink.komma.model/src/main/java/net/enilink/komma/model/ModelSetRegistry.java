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

import net.enilink.komma.core.IReference;
import net.enilink.komma.core.URI;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ModelSetRegistry implements IModelSetRegistry {
	private final Map<URI, IModelSet> modelSets = new ConcurrentHashMap<>();

	@Override
	public void registerModelSet(IModelSet modelSet) {
		modelSets.put(((IReference) modelSet).getURI(), modelSet);
	}

	@Override
	public IModelSet getModelSet(URI uri) {
		return modelSets.get(uri);
	}

	@Override
	public void unregisterModelSet(URI uri) {
		modelSets.remove(uri);
	}
}
