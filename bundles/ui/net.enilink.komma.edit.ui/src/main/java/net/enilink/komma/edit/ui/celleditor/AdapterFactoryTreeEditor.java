/**
 * <copyright> 
 *
 * Copyright (c) 2002, 2009 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   IBM - Initial API and implementation
 *
 * </copyright>
 *
 * $Id: AdapterFactoryTreeEditor.java,v 1.3 2006/12/28 06:50:05 marcelop Exp $
 */
package net.enilink.komma.edit.ui.celleditor;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.common.ui.celleditor.ExtendedTreeEditor;
import net.enilink.komma.edit.provider.IUpdateableItemText;

/**
 * This base class for implementing {@link org.eclipse.swt.custom.TreeEditor}s
 * that delegate to adapters produced by an {@link IAdapterFactory}.
 */
public class AdapterFactoryTreeEditor extends ExtendedTreeEditor {
	protected IAdapterFactory adapterFactory;
	protected TreeItem currentTreeItem;

	public AdapterFactoryTreeEditor(Tree tree, IAdapterFactory adapterFactory) {
		super(tree);
		this.adapterFactory = adapterFactory;
	}

	public IAdapterFactory getAdapterFactory() {
		return adapterFactory;
	}

	public void setAdapterFactory(IAdapterFactory adapterFactory) {
		this.adapterFactory = adapterFactory;
	}

	@Override
	protected void editItem(final TreeItem treeItem) {
		final Object object = treeItem.getData();
		final IUpdateableItemText updateableItemText = (IUpdateableItemText) adapterFactory
				.adapt(object, IUpdateableItemText.class);
		if (updateableItemText != null) {
			String string = updateableItemText.getUpdateableText(object);

			if (string != null) {
				horizontalAlignment = SWT.LEFT;
				// grabHorizontal = true;
				minimumWidth = Math.max(50, treeItem.getBounds().width);

				final Text text = new Text(tree, SWT.BORDER);
				setEditor(text, treeItem);
				text.setFocus();
				text.setText(string);
				text.setSelection(0, string.length());

				text.addFocusListener(new FocusAdapter() {
					@Override
					public void focusLost(FocusEvent event) {
						updateableItemText.setText(object, text.getText());
						text.setVisible(false);
					}
				});
				text.addKeyListener(new KeyAdapter() {
					@Override
					public void keyPressed(KeyEvent event) {
						if (event.character == '\r' || event.character == '\n') {
							updateableItemText.setText(object, text.getText());
							setEditor(null);
							text.dispose();
						} else if (event.character == '\033') {
							setEditor(null);
							text.dispose();
						}
					}
				});
			}
		}
	}
}
