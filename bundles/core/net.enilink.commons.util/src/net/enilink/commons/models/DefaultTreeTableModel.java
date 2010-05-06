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

public class DefaultTreeTableModel<E> extends AbstractTreeTableModel<E> {
	protected List<E> list;
	protected String[] columnNames;

	public DefaultTreeTableModel() {
		this(new ArrayList<E>());
	}

	public DefaultTreeTableModel(List<E> list) {
		this.list = list;
	}

	public void setColumnNames(String[] columnNames) {
		this.columnNames = columnNames;
		fireColumnsChanged();
	}

	public void setList(List<E> list) {
		this.list = list;
		fireTreeStructureChanged(null, null);
	}

	public boolean add(E element) {
		if (list.add(element)) {
			fireTreeNodesInserted(null, new Object[] { element },
					list.size() - 1);
			return true;
		}
		return false;
	}

	public void addAll(Collection<? extends E> c) {
		int index = list.size();
		list.addAll(c);
		fireTreeNodesInserted(null, c.toArray(), index);
	}

	public void addAll(int index, Collection<? extends E> c) {
		list.addAll(index, c);
		fireTreeNodesInserted(null, c.toArray(), index);
	}

	public void add(int index, E element) {
		list.add(index, element);
		fireTreeNodesInserted(null, new Object[] { element }, index);
	}

	public void clear() {
		list.clear();
		fireTreeStructureChanged(null, null);
	}

	public boolean remove(E element) {
		if (list.remove(element)) {
			fireTreeNodesRemoved(null, new Object[] { element });
			return true;
		}
		return false;
	}

	public E remove(int index) {
		E element = list.remove(index);
		if (element != null) {
			fireTreeNodesRemoved(null, new Object[] { element });
		}
		return element;
	}

	public int size() {
		return list.size();
	}

	public Collection<? extends E> getElements() {
		return new ArrayList<E>(list);
	}

	@Override
	public int getColumnCount() {
		return columnNames != null ? columnNames.length : 0;
	}

	@Override
	public String getColumnName(int column) {
		if (columnNames != null) {
			return columnNames[column];
		}
		return null;
	}

	@Override
	public Object getValue(E element, int columnIndex) {
		return null;
	}

	@Override
	public Collection<? extends E> getChildren(E parent) {
		return null;
	}

	@Override
	public E getParent(E node) {
		return null;
	}

	@Override
	public boolean hasChildren(E node) {
		return false;
	}
}
