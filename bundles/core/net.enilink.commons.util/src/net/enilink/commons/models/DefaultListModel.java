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
package net.enilink.commons.models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DefaultListModel<E> extends AbstractListModel<E> {
	protected List<E> list;

	public DefaultListModel() {
		this(new ArrayList<E>());
	}

	public DefaultListModel(List<E> list) {
		this.list = list;
	}

	public void setList(List<E> list) {
		this.list = list;
		fireContentsChanged();
	}

	public boolean add(E element) {
		if (list.add(element)) {
			fireElementsAdded(new Object[] { element });
			return true;
		}
		return false;
	}

	public void addAll(Collection<? extends E> c) {
		int index = list.size();
		list.addAll(c);
		fireElementsAdded(index, c.toArray());
	}

	public void addAll(int index, Collection<? extends E> c) {
		list.addAll(index, c);
		fireElementsAdded(index, c.toArray());
	}

	public void add(int index, E element) {
		list.add(index, element);
		fireElementsAdded(index, new Object[] { element });
	}

	public void clear() {
		list.clear();
		fireElementsRemoved(null);
	}

	public boolean remove(E element) {
		if (list.remove(element)) {
			fireElementsRemoved(new Object[] { element });
			return true;
		}
		return false;
	}

	public E remove(int index) {
		E element = list.remove(index);
		if (element != null) {
			fireElementsRemoved(index, new Object[] { element });
		}
		return element;
	}

	public int size() {
		return list.size();
	}

	public List<? extends E> getElements() {
		return new ArrayList<E>(list);
	}
}
