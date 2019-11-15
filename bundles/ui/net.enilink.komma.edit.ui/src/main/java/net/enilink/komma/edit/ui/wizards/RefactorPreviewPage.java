/*******************************************************************************
 * Copyright (c) 2014 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.edit.ui.wizards;

import java.util.Collection;

import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.edit.refactor.Change;
import net.enilink.komma.edit.refactor.Change.StatementChange;
import net.enilink.komma.edit.refactor.Change.StatementChange.Type;
import net.enilink.komma.edit.ui.KommaEditUIPlugin;
import net.enilink.komma.edit.ui.provider.ExtendedImageRegistry;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;

public abstract class RefactorPreviewPage extends WizardPage {

	protected TreeViewer treeViewer;

	public RefactorPreviewPage(String pageName) {
		super(pageName);
	}

	/**
	 * This method is called upon making the page visible to determine the set
	 * of changes that are to be previewed. It needs to be implemented by
	 * subclasses.
	 * 
	 * @return The set of changes to preview. Should be generated using the
	 *         RefactoringProcessor.
	 */
	public abstract Collection<Change> collectChanges();

	static class TreeLabelProvider extends ColumnLabelProvider {
		public static enum ColumnType {
			RESOURCE, PROPERTY, VALUE
		};

		ColumnType column;
		Color addBg = new Color(Display.getDefault(), 240, 255, 240);
		Color removeBg = new Color(Display.getDefault(), 255, 240, 240);

		public TreeLabelProvider(ColumnType column) {
			this.column = column;
		}

		@Override
		public void dispose() {
			super.dispose();
			addBg.dispose();
			removeBg.dispose();
		}

		@Override
		public Color getBackground(Object element) {
			if (element instanceof StatementChange) {
				if (((StatementChange) element).getType() == Type.ADD) {
					return addBg;
				} else {
					return removeBg;
				}
			}
			return null;
		}

		@Override
		public Image getImage(Object element) {
			if (element instanceof Change) {
				switch (column) {
				case RESOURCE:
					return ExtendedImageRegistry.getInstance().getImage(
							KommaEditUIPlugin.INSTANCE
									.getImage("full/obj16/OWLFile.png"));
				default:
					return null;
				}
			}
			return null;
		}

		@Override
		public String getText(Object element) {
			if (element instanceof Change) {
				switch (column) {
				case RESOURCE:
					return ((Change) element).getModel().toString();
				default:
					return null;
				}
			}
			if (element instanceof StatementChange) {
				IStatement st = ((StatementChange) element).getStatement();
				switch (column) {
				case RESOURCE:
					return st.getSubject().toString();
				case PROPERTY:
					return st.getPredicate().toString();
				case VALUE:
					return st.getObject().toString();
				default:
					return null;
				}
			}
			return null;
		}
	};

	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());

		final Tree tree = new Tree(composite, SWT.BORDER | SWT.H_SCROLL
				| SWT.V_SCROLL | SWT.FULL_SELECTION);
		tree.setHeaderVisible(true);
		tree.setLinesVisible(true);

		GridData treeGridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		tree.setLayoutData(treeGridData);

		treeViewer = new TreeViewer(tree);
		treeViewer.setContentProvider(new ITreeContentProvider() {
			@Override
			public void inputChanged(Viewer viewer, Object oldInput,
					Object newInput) {
			}

			@SuppressWarnings("unchecked")
			@Override
			public Object[] getElements(Object inputElement) {
				if (inputElement instanceof Collection) {
					return ((Collection<Change>) inputElement).toArray();
				}
				return getChildren(inputElement);
			}

			@Override
			public Object[] getChildren(Object parentElement) {
				if (parentElement instanceof Change) {
					return ((Change) parentElement).getStatementChanges()
							.toArray();
				}
				return null;
			}

			@Override
			public Object getParent(Object element) {
				return null;
			}

			@Override
			public boolean hasChildren(Object element) {
				if (element instanceof Change) {
					return true;
				}
				return false;
			}

			@Override
			public void dispose() {
			}
		});
		treeViewer.addTreeListener(new ITreeViewerListener() {
			@Override
			public void treeCollapsed(final TreeExpansionEvent event) {
				getShell().getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {
						treeViewer.update(event.getElement(), null);
					}
				});
			}

			@Override
			public void treeExpanded(final TreeExpansionEvent event) {
				getShell().getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {
						treeViewer.update(event.getElement(), null);
					}
				});
			}
		});

		treeViewer.setComparator(new ViewerComparator() {
			@Override
			public int compare(Viewer viewer, Object o1, Object o2) {
				if (o1 instanceof Change && o2 instanceof Change) {
					return ((Change) o1).getModel().toString()
							.compareTo(((Change) o2).getModel().toString());
				}
				if (o1 instanceof StatementChange
						&& o2 instanceof StatementChange) {
					return compare(((StatementChange) o1).getStatement()
							.getSubject(), ((StatementChange) o2)
							.getStatement().getSubject());
				}
				return o1.hashCode() - o2.hashCode();
			}

			private int compare(IReference s1, IReference s2) {
				// blank nodes at bottom
				if (s1.getURI() == null) {
					return (s2.getURI() == null ? s1.toString().compareTo(
							s2.toString()) : 1);
				}
				if (s2.getURI() == null) {
					return -1;
				}
				// compare the URIs
				return s1.toString().compareTo(s2.toString());
			}
		});

		TreeViewerColumn column = new TreeViewerColumn(treeViewer, SWT.LEFT);
		column.getColumn().setText("Resource");
		column.getColumn().setWidth(350);
		column.setLabelProvider(new TreeLabelProvider(
				TreeLabelProvider.ColumnType.RESOURCE));

		column = new TreeViewerColumn(treeViewer, SWT.LEFT);
		column.getColumn().setText("Property");
		column.getColumn().setWidth(350);
		column.setLabelProvider(new TreeLabelProvider(
				TreeLabelProvider.ColumnType.PROPERTY));

		column = new TreeViewerColumn(treeViewer, SWT.LEFT);
		column.getColumn().setAlignment(SWT.LEFT);
		column.getColumn().setText("Value");
		column.getColumn().setWidth(350);
		column.setLabelProvider(new TreeLabelProvider(
				TreeLabelProvider.ColumnType.VALUE));

		setDescription("Preview and confirm the pending changes.");
		setControl(composite);
		setPageComplete(false);
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			treeViewer.setInput(collectChanges());
			setPageComplete(true);
		}
	}
}
