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

package net.enilink.komma.owl.editor.internal.ontology;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import net.enilink.commons.models.AbstractTableModel;
import net.enilink.commons.ui.jface.viewers.TableCellModifier;
import net.enilink.commons.ui.jface.viewers.TableContentProvider;
import net.enilink.komma.common.command.CommandResult;
import net.enilink.komma.common.command.SimpleCommand;
import net.enilink.komma.common.notify.INotification;
import net.enilink.komma.common.notify.INotificationListener;
import net.enilink.komma.common.notify.NotificationFilter;
import net.enilink.komma.edit.ui.properties.IEditUIPropertiesImages;
import net.enilink.komma.edit.ui.properties.KommaEditUIPropertiesPlugin;
import net.enilink.komma.edit.ui.provider.ExtendedImageRegistry;
import net.enilink.komma.edit.ui.views.AbstractEditingDomainPart;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.event.INamespaceNotification;
import net.enilink.komma.owl.editor.OWLEditorPlugin;
import net.enilink.komma.core.INamespace;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.core.Namespace;
import net.enilink.komma.core.URIImpl;

public class NamespacesPart extends AbstractEditingDomainPart {
	static final String[] TABLE_HEAD = { "Prefix", "Namespace" };
	private IModel model;
	private TableViewer namespaceViewer;
	private NamespaceList namespaceList;
	Action deleteItemAction, addItemAction;

	private class NamespaceList extends AbstractTableModel<INamespace>
			implements INotificationListener<INotification> {
		abstract class NamespaceCommand extends SimpleCommand {
			@Override
			protected CommandResult doExecuteWithResult(
					IProgressMonitor progressMonitor, IAdaptable info)
					throws ExecutionException {
				try {
					modifyNamespace();
					// force model to be modified
					model.setModified(true);
				} catch (KommaException e) {
					return CommandResult.newErrorCommandResult(e);
				}
				return CommandResult.newOKCommandResult();
			}

			protected abstract void modifyNamespace();
		}

		IModel model;
		Set<INamespace> namespaces = Collections.emptySet();

		public void add(final INamespace namespace) {
			for (INamespace existing : namespaces) {
				if (namespace.getPrefix().equals(existing.getPrefix())) {
					return;
				}
			}
			execute(new NamespaceCommand() {
				@Override
				protected void modifyNamespace() {
					model.getManager().setNamespace(namespace.getPrefix(),
							namespace.getURI());
				}
			});
		}

		public boolean remove(final INamespace element) {
			return execute(new NamespaceCommand() {
				@Override
				protected void modifyNamespace() {
					model.getManager().removeNamespace(element.getPrefix());
				}
			});
		}

		@Override
		public boolean isValueEditable(INamespace element, int columnIndex) {
			return true;
		}

		@Override
		public void setValue(final INamespace element, final Object value,
				int columnIndex) {
			if (value == null) {
				return;
			}

			switch (columnIndex) {
			case 0:
				for (INamespace existing : namespaces) {
					if (value.equals(existing.getPrefix())) {
						return;
					}
				}
				execute(new NamespaceCommand() {
					@Override
					protected void modifyNamespace() {
						model.getManager().removeNamespace(element.getPrefix());
						model.getManager().setNamespace((String) value,
								element.getURI());
					}
				});
				break;
			case 1:
				execute(new NamespaceCommand() {
					@Override
					protected void modifyNamespace() {
						model.getManager().removeNamespace(element.getPrefix());
						model.getManager().setNamespace(element.getPrefix(),
								URIImpl.createURI((String) value));
					}
				});
			}
		}

		void setModel(IModel model) {
			unregisterListener();
			this.model = model;
			namespaces.clear();
			if (this.model != null) {
				this.model.getModelSet().addListener(this);
			}
			fireContentsChanged();
		}

		void unregisterListener() {
			if (this.model != null) {
				this.model.getModelSet().removeListener(this);
			}
		}

		@Override
		public Collection<? extends INamespace> getElements() {
			if (model != null) {
				namespaces = new LinkedHashSet<INamespace>(model.getManager()
						.getNamespaces().toList());
			}
			return namespaces;
		}

		@Override
		public int getColumnCount() {
			return TABLE_HEAD.length;
		}

		@Override
		public String getColumnName(int column) {
			return TABLE_HEAD[column];
		}

		@Override
		public Object getValue(INamespace element, int columnIndex) {
			switch (columnIndex) {
			case 0:
				return element.getPrefix();
			case 1:
				return element.getURI().toString();
			}
			return "";
		}

		void dispose() {
			unregisterListener();
		}

		@Override
		public NotificationFilter<INotification> getFilter() {
			return NotificationFilter.instanceOf(INamespaceNotification.class);
		}

		@Override
		public void notifyChanged(
				Collection<? extends INotification> notifications) {
			fireContentsChanged();
		}

		boolean execute(NamespaceCommand command) {
			try {
				getEditingDomain().getCommandStack().execute(command, null,
						null);
				return true;
			} catch (ExecutionException e) {
				OWLEditorPlugin.INSTANCE.log(e);
			}
			return false;
		}
	};

	private class NamespaceLabelProvider extends LabelProvider implements
			ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return getImage(element);
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (element instanceof INamespace) {
				switch (columnIndex) {
				case 0:
					return ((INamespace) element).getPrefix();
				case 1:
					return ((INamespace) element).getURI().toString();
				}
			}
			return "";
		}
	}

	public void createContents(Composite parent) {
		parent.setLayout(new FillLayout());

		createActions();

		Table table = getWidgetFactory().createTable(parent,
				SWT.V_SCROLL | SWT.MULTI | SWT.FULL_SELECTION);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);

		namespaceViewer = new TableViewer(table);
		CellEditor[] editors = new CellEditor[TABLE_HEAD.length];
		editors[0] = new TextCellEditor(namespaceViewer.getTable());
		editors[1] = new TextCellEditor(namespaceViewer.getTable());
		namespaceViewer.setCellEditors(editors);
		namespaceViewer.setCellModifier(new TableCellModifier<INamespace>(
				namespaceViewer));
		namespaceViewer.setColumnProperties(TABLE_HEAD);
		namespaceViewer.setContentProvider(new TableContentProvider());
		namespaceViewer.setLabelProvider(new NamespaceLabelProvider());

		namespaceList = new NamespaceList();
		namespaceViewer.setInput(namespaceList);

		namespaceViewer
				.addSelectionChangedListener(new ISelectionChangedListener() {
					public void selectionChanged(SelectionChangedEvent event) {
						if (deleteItemAction != null)
							deleteItemAction.setEnabled(!event.getSelection()
									.isEmpty());
					}
				});
	}

	public void createActions() {
		IToolBarManager toolBarManager = (IToolBarManager) getForm()
				.getAdapter(IToolBarManager.class);
		if (toolBarManager == null) {
			return;
		}

		addItemAction = new Action("Add") {
			public void run() {
				addItem();
			}
		};
		addItemAction.setImageDescriptor(ExtendedImageRegistry.getInstance()
				.getImageDescriptor(
						KommaEditUIPropertiesPlugin.INSTANCE
								.getImage(IEditUIPropertiesImages.ADD)));
		toolBarManager.add(addItemAction);

		deleteItemAction = new Action("Remove") {
			public void run() {
				deleteItem();
			}
		};
		deleteItemAction.setImageDescriptor(ExtendedImageRegistry.getInstance()
				.getImageDescriptor(
						KommaEditUIPropertiesPlugin.INSTANCE
								.getImage(IEditUIPropertiesImages.REMOVE)));
		deleteItemAction.setEnabled(false);
		toolBarManager.add(deleteItemAction);
	}

	void addItem() {
		namespaceList.add(new Namespace("", "http://"));
	}

	void deleteItem() {
		IStructuredSelection selection = (IStructuredSelection) namespaceViewer
				.getSelection();
		for (Iterator<?> it = selection.iterator(); it.hasNext();) {
			INamespace namespace = (INamespace) it.next();
			namespaceList.remove(namespace);
		}
	}

	@Override
	public boolean setFocus() {
		if (namespaceViewer != null && namespaceViewer.getTable().setFocus()) {
			return true;
		}
		return super.setFocus();
	}

	@Override
	public void dispose() {
		namespaceList.dispose();
		super.dispose();
	}

	public void setInput(Object input) {
		this.model = (IModel) input;
		setStale(true);
	}

	@Override
	public void refresh() {
		namespaceList.setModel(model);
		for (TableColumn column : namespaceViewer.getTable().getColumns()) {
			column.pack();
		}

		super.refresh();
	}
}
