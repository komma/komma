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
package net.enilink.komma.common.adapter;

import net.enilink.komma.common.util.ExtensibleHashSet;

public class AdapterSet extends ExtensibleHashSet<IAdapter> implements
		IAdapterSet {
	private static final long serialVersionUID = 1L;
	private Object target;

	public AdapterSet(Object target) {
		this.target = target;
	}

	@Override
	protected void addedValue(IAdapter value) {
		value.addTarget(target);
	}

	@Override
	protected void removedValue(IAdapter value) {
		value.removeTarget(target);
	}

	public IAdapter getAdapter(Object type) {
		for (IAdapter adapter : this) {
			if (adapter.isAdapterForType(type)) {
				return adapter;
			}
		}
		return null;
	}
}
