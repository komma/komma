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
package net.enilink.komma.sparql.ui.views;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import net.enilink.commons.models.DefaultTableModel;
import net.enilink.commons.ui.editor.EditorWidgetFactory;
import net.enilink.commons.ui.jface.viewers.TableContentProvider;

public class ResultTableViewer implements IResultViewer {
	private final class TableViewerComparator extends ViewerComparator {
		int sortDirection;
		int sortColumn;

		private TableViewerComparator(Comparator<?> comparator,
				int sortDirection, int sortColumn) {
			super(comparator);
			this.sortDirection = sortDirection;
			this.sortColumn = sortColumn;
		}

		@SuppressWarnings("unchecked")
		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			int cat1 = category(e1);
			int cat2 = category(e2);

			if (cat1 != cat2) {
				return cat1 - cat2;
			}

			String name1;
			String name2;

			int direction = sortDirection == SWT.DOWN ? -1 : 1;
			if (viewer == null || !(viewer instanceof ContentViewer)) {
				name1 = e1.toString();
				name2 = e2.toString();
			} else {
				IBaseLabelProvider prov = ((ContentViewer) viewer)
						.getLabelProvider();

				if (prov instanceof ITableLabelProvider) {
					name1 = ((ITableLabelProvider) prov).getColumnText(e1,
							sortColumn);
					name2 = ((ITableLabelProvider) prov).getColumnText(e2,
							sortColumn);
				} else if (prov instanceof ILabelProvider) {
					ILabelProvider lprov = (ILabelProvider) prov;
					name1 = lprov.getText(e1);
					name2 = lprov.getText(e2);
				} else {
					name1 = e1.toString();
					name2 = e2.toString();
				}
			}
			if (name1 == null) {
				name1 = "";//$NON-NLS-1$
			}
			if (name2 == null) {
				name2 = "";//$NON-NLS-1$
			}

			// use the comparator to compare the strings
			return direction * getComparator().compare(name1, name2);
		}
	}

	class TableLabelProvider extends CellLabelProvider implements
			ITableLabelProvider {
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		public String getColumnText(Object element, int columnIndex) {
			if (element instanceof DataRow) {
				Object value = tableModel.getValue((DataRow) element,
						columnIndex);
				return String.valueOf(value);
			}
			return null;
		}

		public void update(ViewerCell cell) {
			Object element = cell.getElement();
			int columnIndex = cell.getColumnIndex();

			cell.setText(getColumnText(element, columnIndex));
			cell.setImage(getColumnImage(element, columnIndex));
			cell.setForeground(getForeground(element, columnIndex));
			cell.setBackground(getBackground(element, columnIndex));
		}

		public Color getBackground(Object element, int columnIndex) {
			if (element instanceof DataRow) {
				if (((DataRow) element).rowNr % 2 == 0) {
					return tableViewer.getControl().getDisplay()
							.getSystemColor(SWT.COLOR_GRAY);
				}
			}
			return null;
		}

		public Color getForeground(Object element, int columnIndex) {
			return null;
		}
	}

	class DataRow {
		Object[] data;
		int rowNr;
	}

	DefaultTableModel<DataRow> tableModel = new DefaultTableModel<DataRow>() {
		@Override
		public Object getValue(DataRow element, int columnIndex) {
			return element.data[columnIndex];
		}
	};
	TableViewer tableViewer;

	@Override
	public void createContents(EditorWidgetFactory widgetFactory,
			Composite parent) {
		parent.setLayout(new GridLayout(1, false));

		Table table = widgetFactory.createTable(parent, SWT.V_SCROLL
				| SWT.H_SCROLL | SWT.FULL_SELECTION);
		table.setHeaderVisible(true);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		tableViewer = new TableViewer(table);
		tableViewer.setContentProvider(new TableContentProvider());
		tableViewer.setLabelProvider(new TableLabelProvider());
		tableViewer.setInput(tableModel);
	}

	@Override
	public String getName() {
		return "Tabelle";
	}

	@Override
	public void setData(String[] columnNames, Collection<Object[]> data) {
		Collection<DataRow> dataRows = new ArrayList<DataRow>(data.size());
		int rowNr = 0;
		for (Object[] result : data) {
			DataRow row = new DataRow();
			row.data = result;
			row.rowNr = rowNr++;

			dataRows.add(row);
		}

		if (tableViewer != null) {
			// prevents firing of events
			tableViewer.setInput(null);
		}
		tableModel.clear();
		tableModel.setColumnNames(columnNames);

		tableModel.addAll(dataRows);
		if (tableViewer != null) {
			tableViewer.setInput(tableModel);
		}

		packColumns();

		if (tableViewer != null) {
			for (TableColumn column : tableViewer.getTable().getColumns()) {
				column.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						TableColumn oldColumn = tableViewer.getTable()
								.getSortColumn();
						if (e.widget instanceof TableColumn) {
							TableColumn newColumn = (TableColumn) e.widget;

							int sortDirection = oldColumn != newColumn ? SWT.UP
									: (tableViewer.getTable()
											.getSortDirection() == SWT.UP ? SWT.DOWN
											: SWT.UP);

							tableViewer.getTable().setSortDirection(
									sortDirection);

							tableViewer.getTable().setSortColumn(newColumn);

							int index = 0;
							for (TableColumn column : tableViewer.getTable()
									.getColumns()) {
								if (column == newColumn)
									break;
								index++;
							}

							tableViewer
									.setComparator(new TableViewerComparator(
											Collator.getInstance(),
											sortDirection, index));
						} else {
							return;
						}
					}
				});
			}
		}
	}

	private void packColumns() {
		if (tableViewer != null) {
			for (TableColumn column : tableViewer.getTable().getColumns()) {
				column.pack();
			}
		}
	}

	@Override
	public Collection<?> getSelection() {
		return ((IStructuredSelection) tableViewer.getSelection()).toList();
	}
}
