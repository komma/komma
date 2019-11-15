/*******************************************************************************
 * Copyright (c) 2009, 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.model.mem;

import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.model.MODELS;
import net.enilink.komma.core.URI;

public class SerializableModelFactory implements IModel.Factory {
	@Override
	public IModel createModel(IModelSet modelSet, URI uri) {
		IModel model = (IModel) modelSet.getMetaDataManager().createNamed(uri,
				MODELS.NAMESPACE_URI.appendFragment("SerializableModel"),
				MODELS.TYPE_MODEL);
		return model;
	}
}
