/**
 * <copyright> 
 *
 * Copyright (c) 2002, 2010 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 * 
 * Contributors: 
 *   IBM - Initial API and implementation
 *
 * </copyright>
 *
 * $Id: ExtendedTreeEditor.java,v 1.3 2006/12/28 06:42:02 marcelop Exp $
 */
package net.enilink.komma.common.ui.celleditor;

import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

/**
 * This base class for implementing a {@link TreeEditor} calls {@link #editItem}
 * when the cell editor potentially needs to be activated. Activation is
 * determined by checking for when a click happens on the single selection that
 * was previously in effect, or when the user hits the space key when a single
 * selection is in effect.
 */
public abstract class ExtendedTreeEditor extends TreeEditor implements
		SelectionListener, MouseListener, KeyListener {
	protected Tree tree;
	protected TreeItem selectedTreeItem;
	protected TreeItem editTreeItem;

	public ExtendedTreeEditor(Tree tree) {
		super(tree);
		this.tree = tree;
		tree.addKeyListener(this);
		tree.addMouseListener(this);
		tree.addSelectionListener(this);
	}

	public void mouseDoubleClick(MouseEvent event) {
		// Do nothing
	}

	public void mouseDown(MouseEvent event) {
		if (event.button == 1) {
			TreeItem treeItem = tree.getItem(new Point(event.x, event.y));
			editTreeItem = treeItem == selectedTreeItem ? treeItem : null;
		}
	}

	public void mouseUp(MouseEvent event) {
		if (event.button == 1) {
			TreeItem treeItem = tree.getItem(new Point(event.x, event.y));
			if (editTreeItem == treeItem && editTreeItem != null) {
				editTreeItem = null;
				editItem(treeItem);
			}
		}
	}

	public void widgetDefaultSelected(SelectionEvent event) {
		widgetSelected(event);
	}

	public void widgetSelected(SelectionEvent event) {
		Control control = getEditor();
		if (control != null && !control.isDisposed()) {
			setEditor(null);
			control.dispose();
		}

		TreeItem[] selection = tree.getSelection();
		selectedTreeItem = selection.length == 1 ? selection[0] : null;
	}

	public void keyPressed(KeyEvent event) {
		// Do nothing
	}

	public void keyReleased(KeyEvent event) {
		if (event.character == ' ' && selectedTreeItem != null) {
			editItem(selectedTreeItem);
		}
	}

	protected abstract void editItem(TreeItem treeItem);
}
