/*******************************************************************************
 * Copyright (c) 2009 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.edit.provider;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import net.enilink.komma.common.adapter.IAdapter;
import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.core.IReferenceable;

public abstract class AdapterFactory implements IAdapterFactory, IDisposable {
	private Map<Object, List<Object>> object2adapters = Collections
			.synchronizedMap(new WeakHashMap<Object, List<Object>>());

	@Override
	public Object adapt(Object object, Object type) {
		if (type instanceof Class<?>
				&& ((Class<?>) type).isAssignableFrom(object.getClass())) {
			return object;
		}
		Object objectReference = object instanceof IReferenceable ? ((IReferenceable) object)
				.getReference() : object;
		List<Object> adapters = object2adapters.get(objectReference);

		Object adapter = null;
		if (adapters != null) {
			for (Object a : adapters) {
				if (a instanceof IAdapter) {
					if (((IAdapter) a).isAdapterForType(type)) {
						adapter = a;
						break;
					}
				} else if (type instanceof Class<?>
						&& ((Class<?>) type).isAssignableFrom(a.getClass())) {
					adapter = a;
					break;
				}
			}
		}
		if (adapter == null) {
			adapter = createAdapter(object, type);
			if (adapter == null
					|| (adapter instanceof IAdapter && !((IAdapter) adapter)
							.isAdapterForType(type))) {
				return null;
			}

			if (adapters == null) {
				adapters = new CopyOnWriteArrayList<Object>();
				object2adapters.put(objectReference, adapters);
			}
			adapters.add(adapter);

			if (adapter instanceof IAdapter) {
				((IAdapter) adapter).addTarget(object);
			}
		}
		return adapter;
	}

	abstract protected Object createAdapter(Object object, Object type);

	public void unlinkAdapter(Object object) {
		Object adapter = object2adapters.remove(object);
		if (adapter instanceof IAdapter) {
			((IAdapter) adapter).removeTarget(object);
		}
	}

	@Override
	public boolean isFactoryForType(Object type) {
		return false;
	}

	public void dispose() {
	}
}