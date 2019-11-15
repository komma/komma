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
package net.enilink.komma.edit.ui.wizards;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;

public class ObjectTypeSelectionPage extends WizardPage {
	protected Object treeInput;
	protected ILabelProvider treeLabelProvider;
	protected ITreeContentProvider treeContentProvider;

	protected TreeViewer treeViewer;

	protected Object[] types = new Object[0];

	public ObjectTypeSelectionPage(String pageName, Object treeInput,
			ILabelProvider labelProvider,
			ITreeContentProvider treeContentProvider) {
		super(pageName);
		this.treeInput = treeInput;
		this.treeLabelProvider = labelProvider;
		this.treeContentProvider = treeContentProvider;
		setPageComplete(false);

		setTitle("New Resource");
		setDescription("Select resource type.");
	}

	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);

		composite.setLayout(new GridLayout(1, false));
		Tree tree = new Tree(composite, SWT.BORDER);

		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		tree.setLayoutData(gridData);

		treeViewer = new TreeViewer(tree);
		treeViewer.setLabelProvider(treeLabelProvider);
		treeViewer.setContentProvider(treeContentProvider);
		treeViewer.setInput(treeInput);

		treeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent e) {
				IStructuredSelection selection = (IStructuredSelection) e
						.getSelection();
				types = selection.toArray();
				setPageComplete(!selection.isEmpty());
			}
		});

		selectTypes();

		setControl(composite);
	}

	protected Object[] getTypes() {
		return types;
	}

	protected void setTypes(Object[] types) {
		this.types = types;
		selectTypes();
	}

	private void selectTypes() {
		if (types != null && treeViewer != null) {
			treeViewer.setSelection(new StructuredSelection(types), true);
		}
	}
}
