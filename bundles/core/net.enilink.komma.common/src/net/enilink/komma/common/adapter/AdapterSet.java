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

import java.util.AbstractSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class AdapterSet extends AbstractSet<IAdapter>implements IAdapterSet {
	protected final Set<IAdapter> adapters = new HashSet<>();
	protected final Object target;

	public AdapterSet(Object target) {
		this.target = target;
	}

	public IAdapter getAdapter(Object type) {
		for (IAdapter adapter : this) {
			if (adapter.isAdapterForType(type)) {
				return adapter;
			}
		}
		return null;
	}

	@Override
	public boolean add(IAdapter e) {
		if (adapters.add(e)) {
			e.addTarget(target);
			return true;
		}
		return false;
	}

	@Override
	public boolean remove(Object o) {
		if (adapters.remove(o)) {
			if (o instanceof IAdapter) {
				((IAdapter) o).removeTarget(target);
			}
			return true;
		}
		return false;
	}

	@Override
	public Iterator<IAdapter> iterator() {
		return new Iterator<IAdapter>() {
			IAdapter current;
			final Iterator<IAdapter> base = adapters.iterator();

			@Override
			public boolean hasNext() {
				return base.hasNext();
			}

			@Override
			public IAdapter next() {
				return current = base.next();
			}

			@Override
			public void remove() {
				if (current != null) {
					base.remove();
					current.removeTarget(target);
				}
			}
		};
	}

	@Override
	public int size() {
		return adapters.size();
	}
}
