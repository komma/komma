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

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;

import net.enilink.commons.models.ITreeModel;
import net.enilink.commons.models.ITreeTableModel;
import net.enilink.commons.models.ITreeTableModelListener;
import net.enilink.commons.models.TableModelEvent;
import net.enilink.commons.models.TreeModelEvent;

public class TreeContentProvider<E> implements ITreeContentProvider {
	public static final int NONE = 0, MANAGE_COLUMNS = 1,
			MANAGE_PROPERTIES = MANAGE_COLUMNS << 1;

	ITreeTableModelListener listener = new ITreeTableModelListener() {
		public void columnsAdded(TableModelEvent e) {
			tableColumnsAdded(e);
		}

		public void columnsChanged(TableModelEvent e) {
			tableColumnsChanged(e);
		}

		public void columnsRemoved(TableModelEvent e) {
			tableColumnsRemoved(e);
		}

		public void treeNodesChanged(TreeModelEvent e) {
			TreeContentProvider.this.treeNodesChanged(e);
		}

		public void treeNodesInserted(TreeModelEvent e) {
			TreeContentProvider.this.treeNodesInserted(e);
		}

		public void treeNodesRemoved(TreeModelEvent e) {
			TreeContentProvider.this.treeNodesRemoved(e);
		}

		public void treeStructureChanged(TreeModelEvent e) {
			TreeContentProvider.this.treeStructureChanged(e);
		}
	};

	TreeViewer viewer;
	ITreeModel<E> model;
	boolean manageColumns;
	boolean manageProperties;

	public TreeContentProvider() {
		this(MANAGE_COLUMNS | MANAGE_PROPERTIES);
	}

	public TreeContentProvider(int flags) {
		this.manageColumns = (flags & MANAGE_COLUMNS) != 0;
		this.manageProperties = (flags & MANAGE_PROPERTIES) != 0;
	}

	public void dispose() {
		if (model instanceof ITreeTableModel<?>
				&& (manageColumns || manageProperties)) {
			((ITreeTableModel<?>) model).removeTreeTableModelListener(listener);
		} else if (model instanceof ITreeModel<?>) {
			model.removeTreeModelListener(listener);
		}
		listener = null;
	}

	@SuppressWarnings({ "unchecked" })
	public Object[] getChildren(Object parentElement) {
		if (model != null) {
			return model.getChildren((E) parentElement).toArray();
		}
		return new Object[0];
	}

	@SuppressWarnings({ "unchecked" })
	public Object getParent(Object element) {
		return model.getParent((E) element);
	}

	@SuppressWarnings({ "unchecked" })
	public boolean hasChildren(Object element) {
		return model.hasChildren((E) element);
	}

	public Object[] getElements(Object inputElement) {
		return model.getElements().toArray();
	}

	@SuppressWarnings("unchecked")
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (viewer instanceof TreeViewer) {
			this.viewer = (TreeViewer) viewer;
		} else {
			this.viewer = null;
		}

		if (oldInput instanceof ITreeTableModel<?>
				&& (manageColumns || manageProperties)) {
			((ITreeTableModel<?>) oldInput)
					.removeTreeTableModelListener(listener);
		} else if (oldInput instanceof ITreeModel<?>) {
			((ITreeModel<?>) oldInput).removeTreeModelListener(listener);
		}
		if (newInput instanceof ITreeTableModel<?>
				&& (manageColumns || manageProperties)) {
			model = (ITreeTableModel<E>) newInput;
			if (this.viewer != null) {
				((ITreeTableModel<?>) model)
						.addTreeTableModelListener(listener);
				intializeColumns();
			}
		} else if (newInput instanceof ITreeModel<?>) {
			model = (ITreeModel<E>) newInput;
			if (this.viewer != null) {
				model.addTreeModelListener(listener);
			}
		} else {
			model = null;
		}
	}

	private void intializeColumns() {
		ITreeTableModel<?> treeTableModel = (ITreeTableModel<?>) model;
		Tree tree = viewer.getTree();

		for (TreeColumn column : tree.getColumns()) {
			column.dispose();
		}

		for (int index = 0, count = treeTableModel.getColumnCount(); index < count; index++) {
			TreeColumn column = new TreeColumn(tree, SWT.NONE, index);
			column.setText(treeTableModel.getColumnName(index));
			column.pack();
		}
		if (manageProperties) {
			updateColumnProperties(treeTableModel);
		}
	}

	private void updateColumnProperties(ITreeTableModel<?> treeTableModel) {
		String[] properties = new String[treeTableModel.getColumnCount()];
		for (int i = 0, length = properties.length; i < length; i++) {
			properties[i] = String.valueOf(i);
		}
		viewer.setColumnProperties(properties);
	}

	protected void tableColumnsAdded(TableModelEvent e) {
		ITreeTableModel<?> treeTableModel = (ITreeTableModel<?>) model;
		Tree tree = viewer.getTree();

		if (manageColumns) {
			int index = e.index == TableModelEvent.NO_INDEX ? tree
					.getColumnCount() : e.index;
			int count = e.count;

			while (count > 0) {
				TreeColumn column = new TreeColumn(tree, SWT.NONE, index);
				column.setText(treeTableModel.getColumnName(index));
				column.pack();
				count--;
				index++;
			}
		}
		if (manageProperties) {
			updateColumnProperties(treeTableModel);
		}
	}

	protected void tableColumnsChanged(TableModelEvent e) {
		if (!manageColumns) {
			return;
		}

		ITreeTableModel<?> treeTableModel = (ITreeTableModel<?>) model;
		Tree tree = viewer.getTree();

		for (TreeColumn column : tree.getColumns()) {
			column.dispose();
		}

		int columnCount = treeTableModel.getColumnCount();
		for (int i = 0; i < columnCount; i++) {
			TreeColumn column = new TreeColumn(tree, SWT.NONE);
			column.setText(treeTableModel.getColumnName(i));
			column.pack();
		}
	}

	protected void tableColumnsRemoved(TableModelEvent e) {
		if (manageColumns) {
			Tree tree = viewer.getTree();

			int count = e.count;
			int index = e.index == TableModelEvent.NO_INDEX ? tree
					.getColumnCount()
					- count : e.index;

			while (count > 0) {
				TreeColumn column = tree.getColumn(index++);
				column.dispose();
				count--;
			}
		}
		if (manageProperties) {
			updateColumnProperties((ITreeTableModel<?>) model);
		}
	}

	public void treeNodesChanged(TreeModelEvent e) {
		Object[] elements = e.getChildren();
		for (Object element : elements) {
			viewer.update(element, null);
		}
	}

	public void treeNodesInserted(TreeModelEvent e) {
		Object[] parentPath = e.getParentPath();
		if (parentPath != null) {
			viewer.refresh(parentPath[parentPath.length - 1]);
		} else {
			viewer.refresh();
		}
	}

	public void treeNodesRemoved(TreeModelEvent e) {
		Object[][] paths = e.getPaths();
		TreePath[] treePaths = new TreePath[paths.length];
		for (int i = 0; i < paths.length; i++) {
			treePaths[i] = new TreePath(paths[i]);
		}
		viewer.remove((Object[]) treePaths);
	}

	public void treeStructureChanged(TreeModelEvent e) {
		Object[] children = e.getChildren();
		if (children != null && children.length > 0) {
			for (Object child : children) {
				viewer.refresh(child);
			}
			return;
		}

		Object[] parentPath = e.getParentPath();
		if (parentPath != null && parentPath.length > 0) {
			viewer.refresh(parentPath[parentPath.length - 1]);
			return;
		}

		viewer.refresh();
	}
}
