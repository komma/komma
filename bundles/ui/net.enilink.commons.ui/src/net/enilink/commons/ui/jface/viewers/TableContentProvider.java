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
package net.enilink.commons.ui.jface.viewers;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import net.enilink.commons.models.IListModel;
import net.enilink.commons.models.ITableModel;
import net.enilink.commons.models.ITableModelListener;
import net.enilink.commons.models.ListModelEvent;
import net.enilink.commons.models.TableModelEvent;

public class TableContentProvider implements IStructuredContentProvider {
	public static final int NONE = 0, MANAGE_COLUMNS = 1,
			MANAGE_PROPERTIES = MANAGE_COLUMNS << 1;

	ITableModelListener listener = new ITableModelListener() {
		public void contentsChanged(ListModelEvent e) {
			tableContentsChanged(e);
		}

		public void elementsAdded(ListModelEvent e) {
			tableElementsAdded(e);
		}

		public void elementsRemoved(ListModelEvent e) {
			tableElementsRemoved(e);
		}

		public void columnsAdded(TableModelEvent e) {
			tableColumnsAdded(e);
		}

		public void columnsChanged(TableModelEvent e) {
			tableColumnsChanged(e);
		}

		public void columnsRemoved(TableModelEvent e) {
			tableColumnsRemoved(e);
		}
	};

	boolean manageColumns;
	boolean manageProperties;
	TableViewer viewer;
	IListModel<?> model;

	public TableContentProvider() {
		this(MANAGE_COLUMNS | MANAGE_PROPERTIES);
	}

	public TableContentProvider(int flags) {
		this.manageColumns = (flags & MANAGE_COLUMNS) != 0;
		this.manageProperties = (flags & MANAGE_PROPERTIES) != 0;
	}

	protected IListModel<?> getModel() {
		return model;
	}

	public Object[] getElements(Object inputElement) {
		if (model != null) {
			return model.getElements().toArray();
		}
		return new Object[0];
	}

	public void dispose() {
		if (model instanceof ITableModel<?>
				&& (manageColumns || manageProperties)) {
			((ITableModel<?>) model).removeTableModelListener(listener);
		} else if (model instanceof IListModel<?>) {
			model.removeListModelListener(listener);
		}
		listener = null;
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (viewer instanceof TableViewer) {
			this.viewer = (TableViewer) viewer;
		} else {
			this.viewer = null;
		}

		if (oldInput instanceof ITableModel<?>
				&& (manageColumns || manageProperties)) {
			((ITableModel<?>) oldInput).removeTableModelListener(listener);
		} else if (oldInput instanceof IListModel<?>) {
			((IListModel<?>) oldInput).removeListModelListener(listener);
		}
		if (newInput instanceof ITableModel<?>
				&& (manageColumns || manageProperties)) {
			model = (IListModel<?>) newInput;
			if (this.viewer != null) {
				((ITableModel<?>) model).addTableModelListener(listener);
				intializeColumns();
			}
		} else if (newInput instanceof IListModel<?>) {
			model = (IListModel<?>) newInput;
			if (this.viewer != null) {
				model.addListModelListener(listener);
			}
		} else {
			model = null;
		}
	}

	private void intializeColumns() {
		ITableModel<?> tableModel = (ITableModel<?>) model;
		Table table = viewer.getTable();

		for (TableColumn column : table.getColumns()) {
			column.dispose();
		}

		for (int index = 0, count = tableModel.getColumnCount(); index < count; index++) {
			TableColumn column = new TableColumn(table, SWT.NONE, index);
			column.setText(tableModel.getColumnName(index));
			column.pack();
		}
		if (manageProperties) {
			updateColumnProperties(tableModel);
		}
	}

	private void updateColumnProperties(ITableModel<?> tableModel) {
		String[] properties = new String[tableModel.getColumnCount()];
		for (int i = 0, length = properties.length; i < length; i++) {
			properties[i] = String.valueOf(i);
		}
		viewer.setColumnProperties(properties);
	}

	protected void tableContentsChanged(ListModelEvent e) {
		Object[] elements = e.elements;
		if (elements == null) {
			viewer.refresh();
		} else {
			for (Object element : elements) {
				viewer.refresh(element);
			}
		}
	}

	protected void tableContentsChanged(TableModelEvent e) {
		Object[] elements = e.elements;
		if (elements == null) {
			viewer.refresh();
		} else {
			String property = manageProperties
					&& e.index != TableModelEvent.NO_INDEX ? Integer
					.toString(e.index) : null;
			for (Object element : elements) {
				if (property != null) {
					viewer.update(element, new String[] { Integer
							.toString(e.index) });
				} else {
					viewer.refresh(element);
				}
			}
		}
	}

	protected void tableElementsAdded(ListModelEvent e) {
		Object[] elements = e.elements;
		if (elements == null) {
			viewer.refresh();
		} else {
			if (e.index == ListModelEvent.NO_INDEX) {
				viewer.add(elements);
			} else {
				int index = e.index;
				for (Object element : elements) {
					viewer.insert(element, index++);
				}
			}
		}
	}

	protected void tableElementsRemoved(ListModelEvent e) {
		Object[] elements = e.elements;
		if (elements == null) {
			viewer.refresh();
		} else {
			viewer.remove(elements);
		}
	}

	protected void tableColumnsAdded(TableModelEvent e) {
		ITableModel<?> tableModel = (ITableModel<?>) model;
		Table table = viewer.getTable();

		if (manageColumns) {
			int index = e.index == TableModelEvent.NO_INDEX ? table
					.getColumnCount() : e.index;
			int count = e.count;

			while (count > 0) {
				TableColumn column = new TableColumn(table, SWT.NONE, index);
				column.setText(tableModel.getColumnName(index));
				column.pack();
				count--;
				index++;
			}
		}
		if (manageProperties) {
			updateColumnProperties(tableModel);
		}
	}

	protected void tableColumnsChanged(TableModelEvent e) {
		if (!manageColumns) {
			return;
		}

		ITableModel<?> tableModel = (ITableModel<?>) model;
		Table table = viewer.getTable();

		int newColumnCount = tableModel.getColumnCount();
		TableColumn[] oldColumns = table.getColumns();

		// delete some columns if new column count is less
		for (int i = newColumnCount; i < oldColumns.length; i++) {
			oldColumns[i].dispose();
		}

		// add some columns if new column count is greater
		for (int i = oldColumns.length; i < newColumnCount; i++) {
			TableColumn column = new TableColumn(table, SWT.NONE);
			column.setText(tableModel.getColumnName(i));
			column.pack();
		}

		TableColumn[] currentColumns = table.getColumns();
		int updateLength = Math.min(oldColumns.length, newColumnCount);

		// update captions and pack columns
		for (int i = 0; i < updateLength; i++) {
			currentColumns[i].setText(tableModel.getColumnName(i));
			currentColumns[i].pack();
		}

		if (manageProperties) {
			updateColumnProperties((ITableModel<?>) model);
		}

		viewer.refresh();
	}

	protected void tableColumnsRemoved(TableModelEvent e) {
		if (manageColumns) {
			Table table = viewer.getTable();

			int count = e.count;
			int index = e.index == TableModelEvent.NO_INDEX ? table
					.getColumnCount()
					- count : e.index;

			while (count > 0) {
				TableColumn column = table.getColumn(index++);
				column.dispose();
				count--;
			}
		}
		if (manageProperties) {
			updateColumnProperties((ITableModel<?>) model);
		}
	}
}
