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
