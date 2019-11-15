/*******************************************************************************
 * Copyright (c) 2009 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.sparql.ui.views;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.enilink.commons.ui.editor.EditorWidgetFactory;
import net.enilink.komma.core.INamespace;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.URI;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
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

public class ResultTableViewer implements IResultViewer {
	static class DataRow {
		Object[] data;
		int rowNr;
	}

	class TableLabelProvider extends CellLabelProvider implements
			ITableLabelProvider {
		Map<URI, String> prefixMap = new HashMap<>();

		public TableLabelProvider(Set<INamespace> namespaces) {
			for (INamespace ns : namespaces) {
				prefixMap.put(ns.getURI(), ns.getPrefix());
			}
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

		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		public String getColumnText(Object element, int columnIndex) {
			if (element instanceof DataRow) {
				Object value = ((DataRow) element).data[columnIndex];
				if (value instanceof IReference) {
					URI uri = ((IReference) value).getURI();
					if (uri != null) {
						String prefix = prefixMap.get(uri.namespace());
						if (prefix != null) {
							return prefix.isEmpty() ? uri.localPart() : prefix
									+ ":" + uri.localPart();
						}
					}
				}
				return String.valueOf(value);
			}
			return null;
		}

		public Color getForeground(Object element, int columnIndex) {
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
	}

	private final class TableViewerComparator extends ViewerComparator {
		int sortDirection;
		int sortColumn;

		private TableViewerComparator(Comparator<? super String> comparator,
				int sortDirection, int sortColumn) {
			super(comparator);
			this.sortDirection = sortDirection;
			this.sortColumn = sortColumn;
		}

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

	TableViewer tableViewer;

	@Override
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		tableViewer.addSelectionChangedListener(listener);
	}

	@Override
	public void createContents(EditorWidgetFactory widgetFactory,
			Composite parent) {
		parent.setLayout(new GridLayout(1, false));
		Table table = widgetFactory.createTable(parent, SWT.V_SCROLL
				| SWT.H_SCROLL);
		table.setHeaderVisible(true);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tableViewer = new TableViewer(table);
		tableViewer.setContentProvider(new IStructuredContentProvider() {
			@Override
			public void inputChanged(Viewer viewer, Object oldInput,
					Object newInput) {
				// TODO Auto-generated method stub

			}

			@Override
			public void dispose() {
			}

			@Override
			public Object[] getElements(Object inputElement) {
				if (inputElement instanceof Object[]) {
					return (Object[]) inputElement;
				}
				return new Object[0];
			}
		});
	}

	@Override
	public String getName() {
		return "List";
	}

	@Override
	public ISelection getSelection() {
		return tableViewer.getSelection();
	}

	@Override
	public void removeSelectionChangedListener(
			ISelectionChangedListener listener) {
		tableViewer.removeSelectionChangedListener(listener);
	}

	@Override
	public void setData(Set<INamespace> namespaces, String[] columnNames,
			Collection<Object[]> data) {
		Collection<DataRow> dataRows = new ArrayList<DataRow>(data.size());
		int rowNr = 0;
		for (Object[] result : data) {
			DataRow row = new DataRow();
			row.data = result;
			row.rowNr = rowNr++;
			dataRows.add(row);
		}
		tableViewer.setInput(null);
		for (TableColumn column : tableViewer.getTable().getColumns()) {
			column.dispose();
		}
		for (String columnName : columnNames) {
			TableColumn column = new TableColumn(tableViewer.getTable(),
					SWT.LEFT);
			column.setText(columnName);
			column.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					TableColumn oldColumn = tableViewer.getTable()
							.getSortColumn();
					if (e.widget instanceof TableColumn) {
						TableColumn newColumn = (TableColumn) e.widget;
						int sortDirection = oldColumn != newColumn ? SWT.UP
								: (tableViewer.getTable().getSortDirection() == SWT.UP ? SWT.DOWN
										: SWT.UP);
						tableViewer.getTable().setSortDirection(sortDirection);
						tableViewer.getTable().setSortColumn(newColumn);
						int index = 0;
						for (TableColumn column : tableViewer.getTable()
								.getColumns()) {
							if (column == newColumn)
								break;
							index++;
						}
						tableViewer.setComparator(new TableViewerComparator(
								Collator.getInstance(), sortDirection, index));
					} else {
						return;
					}
				}
			});
		}
		tableViewer.setLabelProvider(new TableLabelProvider(namespaces));
		tableViewer.setInput(dataRows.toArray(new Object[dataRows.size()]));
		for (TableColumn column : tableViewer.getTable().getColumns()) {
			column.pack();
		}
	}

	@Override
	public void setSelection(ISelection selection) {
		tableViewer.setSelection(selection);
	}
}
