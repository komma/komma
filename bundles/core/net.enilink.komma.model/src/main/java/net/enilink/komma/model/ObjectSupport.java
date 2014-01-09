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
package net.enilink.komma.model;

import net.enilink.composition.traits.Behaviour;
import net.enilink.komma.em.concepts.BehaviorBase;

public abstract class ObjectSupport extends BehaviorBase implements IObject,
		IModelAware, Behaviour<IObject>,
		net.enilink.komma.internal.model.IModelAware {
	private IModel model;

	@Override
	public IModel getModel() {
		return model;
	}

	@Override
	public void initModel(IModel model) {
		this.model = model;
	}
}