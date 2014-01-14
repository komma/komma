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
package net.enilink.komma.edit.ui.properties.internal.parts;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.enilink.commons.ui.editor.IEditorForm;
import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.common.command.CommandResult;
import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.common.command.SimpleCommand;
import net.enilink.komma.core.ILiteral;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIImpl;
import net.enilink.komma.edit.properties.PropertyEditingHelper;
import net.enilink.komma.edit.ui.properties.IEditUIPropertiesImages;
import net.enilink.komma.edit.ui.properties.KommaEditUIPropertiesPlugin;
import net.enilink.komma.edit.ui.properties.internal.context.IPropertiesContext;
import net.enilink.komma.edit.ui.properties.internal.wizards.EditPropertyWizard;
import net.enilink.komma.edit.ui.provider.AdapterFactoryLabelProvider;
import net.enilink.komma.edit.ui.provider.ExtendedImageRegistry;
import net.enilink.komma.edit.ui.util.EditUIUtil;
import net.enilink.komma.edit.ui.views.AbstractEditingDomainPart;
import net.enilink.komma.edit.util.PropertyUtil;
import net.enilink.komma.em.concepts.IProperty;
import net.enilink.komma.em.concepts.IResource;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IObject;
import net.enilink.komma.model.ModelUtil;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;

public class PropertyTreePart extends AbstractEditingDomainPart implements
		IPropertyChangeListener {
	enum ColumnType {
		PROPERTY, VALUE, LITERAL_LANG_TYPE, INFERRED
	}

	class AddButtonListener extends SelectionAdapter {
		public void widgetSelected(SelectionEvent event) {
			Object selected = ((IStructuredSelection) treeViewer.getSelection())
					.getFirstElement();

			IResource selectedResource;
			if (selected instanceof StatementNode) {
				selectedResource = (IResource) ((StatementNode) selected)
						.getResource();
			} else {
				// simply use root object
				selectedResource = resource;
			}

			if (selectedResource != null) {
				// make sure to call the dialog for the selected resource
				// in case this statement is a reference to another resource,
				// return that one instead of the statement's subject
				EditPropertyWizard newPropertyWizard = new EditPropertyWizard(
						getAdapterFactory(), getEditingDomain(),
						selectedResource, null, null);
				WizardDialog dialog = new WizardDialog(getShell(),
						newPropertyWizard);
				dialog.open();
			}
		}
	}

	class AnonymousFilter extends ViewerFilter {
		private boolean excludeAnonymous = true;

		@Override
		public boolean select(Viewer viewer, Object parentElement,
				Object element) {
			if (!excludeAnonymous) {
				return true;

			} else {
				IStatement statement = null;
				if (element instanceof StatementNode) {
					statement = ((StatementNode) element).getStatement();
				}

				if (statement != null) {
					Object obj = statement.getObject();
					if (obj instanceof IObject) {
						if (((IObject) obj).getURI() == null) {
							return false;
						}
					}
				}
				return true;
			}
		}

		public void setExcludeAnonymous(boolean showAnonymous) {
			this.excludeAnonymous = showAnonymous;
		}
	}

	class ChangeUriButtonListener extends SelectionAdapter {
		@Override
		public void widgetSelected(SelectionEvent e) {
			if (resource != null) {
				final URI uri;
				if (itemShowFull.getSelection()) {
					uri = URIImpl.createURI(uriText.getText());
				} else {
					uri = resource.getURI().namespace()
							.appendFragment(uriText.getText());
				}

				try {
					getEditingDomain().getCommandStack().execute(
							new SimpleCommand() {

								@Override
								protected CommandResult doExecuteWithResult(
										IProgressMonitor progressMonitor,
										IAdaptable info)
										throws ExecutionException {
									IResource newResource = resource
											.getEntityManager().rename(
													resource, uri);
									setInput(newResource);
									return CommandResult.newOKCommandResult();
								}
							}, null, null);
				} catch (ExecutionException e1) {
					e1.printStackTrace();
				}
			}
		}
	}

	class EditButtonListener extends SelectionAdapter {
		@Override
		public void widgetSelected(SelectionEvent e) {
			Object selected = ((IStructuredSelection) treeViewer.getSelection())
					.getFirstElement();

			IStatement statement = null;
			if (selected instanceof StatementNode) {
				statement = ((StatementNode) selected).getStatement();
			}

			if (statement != null) {
				EditPropertyWizard editPropertyWizard = new EditPropertyWizard(
						getAdapterFactory(), getEditingDomain(),
						(IResource) statement.getSubject(),
						(IProperty) statement.getPredicate(),
						statement.getObject());
				WizardDialog dialog = new WizardDialog(getShell(),
						editPropertyWizard);
				dialog.open();
			}
		}
	}

	class RemoveButtonListener extends SelectionAdapter {
		String toString(IStatement stmt) {
			return new StringBuilder(labelProvider.getText(stmt.getSubject()))
					.append(" ")
					.append(labelProvider.getText(stmt.getPredicate()))
					.append(" ")
					.append(labelProvider.getText(stmt.getObject())).toString();
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			Object selected = ((IStructuredSelection) treeViewer.getSelection())
					.getFirstElement();

			IStatement statement = null;
			if (selected instanceof StatementNode) {
				statement = ((StatementNode) selected).getStatement();
			}

			if (statement != null) {
				MessageBox messageBox = new MessageBox(getShell(),
						SWT.ICON_QUESTION | SWT.YES | SWT.NO);
				messageBox.setMessage("Are you sure you want to delete '"
						+ toString(statement) + "'?");
				messageBox.setText("Remove property");
				int response = messageBox.open();
				if (response == SWT.YES) {
					ICommand removeCommand = PropertyUtil.getRemoveCommand(
							getEditingDomain(),
							(IResource) statement.getSubject(),
							(IProperty) statement.getPredicate(),
							statement.getObject());

					IStatus status = Status.CANCEL_STATUS;
					try {
						status = getEditingDomain().getCommandStack().execute(
								removeCommand, null, null);
					} catch (ExecutionException exc) {
						status = EditUIUtil.createErrorStatus(exc);
					}

					if (!status.isOK()) {
						MessageDialog.openError(getShell(), "Error",
								status.getMessage());
					}
				}
			}
		}
	}

	class ShowFullButtonListener extends SelectionAdapter {
		@Override
		public void widgetSelected(SelectionEvent e) {
			setUriText();
		}
	}

	class TreeLabelProvider extends ColumnLabelProvider {
		private Color background;
		private Color invalidBackground;
		private ColumnType column;

		public TreeLabelProvider(ColumnType column) {
			this.column = column;
			background = new Color(Display.getDefault(), 229, 229, 229);
			invalidBackground = Display.getDefault().getSystemColor(
					SWT.COLOR_YELLOW);
		}

		@Override
		public void dispose() {
			super.dispose();
			background.dispose();
		}

		@Override
		public Color getBackground(Object element) {
			if (column == ColumnType.VALUE && element instanceof StatementNode
					&& !((StatementNode) element).getStatus().isOK()) {
				return invalidBackground;
			}

			if (element instanceof PropertyNode) {
				return null;
			}

			return background;
		}

		@Override
		public Color getForeground(Object element) {
			return null;
		}

		@Override
		public Image getImage(Object element) {
			switch (column) {
			case PROPERTY:
				if (element instanceof PropertyNode) {
					if (((PropertyNode) element).getStatement().getPredicate() == null) {
						return ExtendedImageRegistry.getInstance().getImage(
								KommaEditUIPropertiesPlugin.INSTANCE
										.getImage(IEditUIPropertiesImages.ADD));
					}
					return toImage(((PropertyNode) element).getStatement()
							.getPredicate());
				} else if (element instanceof StatementNode) {
					return toImage(((StatementNode) element).getStatement()
							.getPredicate());
				}
				break;
			case VALUE:
				if ((element instanceof PropertyNode)) {
					if (((PropertyNode) element).isIncomplete()
							|| ((PropertyNode) element)
									.isCreateNewStatementOnEdit()) {
						return ExtendedImageRegistry.getInstance().getImage(
								KommaEditUIPropertiesPlugin.INSTANCE
										.getImage(IEditUIPropertiesImages.ADD));
					}
					if (treeViewer.getExpandedState(element)) {
						return null;
					}

					return toImage(((PropertyNode) element).getValue());
				} else if (element instanceof StatementNode) {
					return toImage(element);
				}
				break;
			case INFERRED:
				if (element instanceof PropertyNode) {
					if (treeViewer.getExpandedState(element)
							|| ((PropertyNode) element).isIncomplete()) {
						return null;
					}
					element = ((PropertyNode) element).getStatement();
				} else {
					element = ((StatementNode) element).getStatement();
				}

				if (((IStatement) element).isInferred()) {
					return ExtendedImageRegistry.getInstance().getImage(
							KommaEditUIPropertiesPlugin.INSTANCE
									.getImage(IEditUIPropertiesImages.CHECKED));
				}
			default:
			}
			return null;
		}

		@Override
		public String getText(Object element) {
			if (element instanceof StatementNode) {
				switch (column) {
				case PROPERTY:
					if (element instanceof PropertyNode) {
						PropertyNode propertyNode = (PropertyNode) element;
						return (propertyNode.isInverse() ? "<- " : "")
								+ toString(propertyNode.getStatement()
										.getPredicate());
					}
					return "";
				case VALUE:
					return toString(element);
				case LITERAL_LANG_TYPE:
					return toString(element);
				default:
				}
			}
			return null;
		}

		@Override
		public int getToolTipDisplayDelayTime(Object object) {
			return 300;
		}

		@Override
		public String getToolTipText(Object element) {
			if (column == ColumnType.VALUE && element instanceof StatementNode
					&& !((StatementNode) element).getStatus().isOK()) {
				return ((StatementNode) element).getStatus().getMessage();
			}

			Object target = null;
			if (element instanceof PropertyNode) {
				PropertyNode propertyNode = (PropertyNode) element;
				switch (column) {
				case PROPERTY:
					target = propertyNode.getStatement().getPredicate();
					break;
				case VALUE:
					if (treeViewer.getExpandedState(element)) {
						return null;
					}
					target = propertyNode.getStatement().getObject();
					break;
				default:
				}
			} else if (element instanceof StatementNode) {
				switch (column) {
				case PROPERTY:
					target = ((StatementNode) element).getStatement()
							.getPredicate();
					break;
				case VALUE:
					target = ((StatementNode) element).getStatement()
							.getObject();
					break;
				default:
				}
			}

			return target != null ? target.toString() : null;
		}

		public Image toImage(Object element) {
			if (labelProvider == null || element == null) {
				return null;
			}
			if (element instanceof PropertyNode) {
				element = ((PropertyNode) element).getStatement()
						.getPredicate();
			} else if (element instanceof StatementNode) {
				element = ((StatementNode) element).getValue();
			}
			return labelProvider.getImage(element);
		}

		public String toString(Object element) {
			if (labelProvider == null || element == null) {
				return null;
			}
			if (element instanceof PropertyNode
					&& treeViewer.getExpandedState(element)) {
				return null;
			}
			if (element instanceof StatementNode) {
				Object invalidEditorValue = ((StatementNode) element)
						.getEditorValue();
				if (invalidEditorValue != null) {
					element = invalidEditorValue;
				} else {
					element = ((StatementNode) element).getValue();
				}
			}
			if (column == ColumnType.LITERAL_LANG_TYPE) {
				if (element instanceof ILiteral) {
					String lang = ((ILiteral) element).getLanguage();
					if (lang != null) {
						return "@" + lang;
					}
					URI datatype = ((ILiteral) element).getDatatype();
					return datatype != null ? labelProvider.getText(resource
							.getEntityManager().find(datatype)) : null;
				}
				return null;
			}
			if (element instanceof ILiteral) {
				return ((ILiteral) element).getLabel();
			}
			return labelProvider.getText(element);
		}
	}

	private IAdapterFactory adapterFactory;
	private AnonymousFilter anonymousFilter;

	private Button changeUri;

	private PropertyTreeContentProvider contentProvider;

	private IPropertiesContext context;

	private List<ValueEditingSupport> editingSupports;

	private Button itemShowFull;

	private ILabelProvider labelProvider;

	private IResource resource;

	private TreeViewer treeViewer;

	private Text uriText;

	private List<Object> cachedExpandedElements = Collections.emptyList();

	private IAction addPropertyAction = new Action("Add property") {
		@Override
		public void run() {
			ITreeSelection selection = (ITreeSelection) treeViewer
					.getSelection();

			TreePath path = TreePath.EMPTY;
			IResource subject = resource;
			if (!selection.isEmpty()) {
				path = selection.getPaths()[0];
				Object selected = path.getLastSegment();

				if (selected instanceof PropertyNode) {
					subject = ((PropertyNode) selected).getResource();
					path = path.getParentPath();
				} else if (selected instanceof StatementNode) {
					Object object = ((StatementNode) selected).getStatement()
							.getObject();

					if (object instanceof IResource) {
						subject = (IResource) object;
					}

				}
			}

			PropertyNode node = new PropertyNode(subject, null, false, true);
			treeViewer.insert(
					path.getSegmentCount() > 0 ? path : treeViewer.getInput(),
					node, 0);
			treeViewer.setSelection(new StructuredSelection(node), true);
			treeViewer.editElement(node, 0);
		}
	};

	private IAction addPropertyValueAction = new Action("Add value") {
		@Override
		public void run() {
			ITreeSelection selection = (ITreeSelection) treeViewer
					.getSelection();
			Object selected = selection.getFirstElement();

			if (selected instanceof PropertyNode
					&& !((PropertyNode) selected).isInverse()) {
				((PropertyNode) selected).setCreateNewStatementOnEdit(true);
				treeViewer.editElement(selection.getPaths()[0], 1);
			}
		}
	};

	@Override
	public void activate() {
		contentProvider.registerListener();
		super.activate();
	}

	@Override
	public void createContents(Composite parent) {
		parent.setLayout(new GridLayout(1, false));
		getWidgetFactory().paintBordersFor(parent);

		context = (IPropertiesContext) getForm().getAdapter(
				IPropertiesContext.class);
		if (context != null) {
			context.addPropertyChangeListener(this);
		}

		createToolbar(parent);
		createTree(parent);
	}

	private void createToolbar(Composite parent) {
		Composite toolBar = getWidgetFactory().createComposite(parent);
		toolBar.setLayout(new GridLayout(6, false));
		toolBar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		itemShowFull = getWidgetFactory().createButton(toolBar, "Full",
				SWT.TOGGLE);
		itemShowFull.setToolTipText("Show the full URI");
		itemShowFull.addSelectionListener(new ShowFullButtonListener());
		itemShowFull.setEnabled(true);

		changeUri = getWidgetFactory().createButton(toolBar, "OK", SWT.NONE);
		changeUri.setToolTipText("Change the URI of the selected entity");
		changeUri.addSelectionListener(new ChangeUriButtonListener());
		changeUri.setEnabled(false);

		uriText = getWidgetFactory().createText(toolBar, "", SWT.SINGLE);
		uriText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		uriText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (resource.getURI() != null) {
					changeUri.setEnabled(true);
				}
			}
		});

		Button itemAdd = getWidgetFactory().createButton(toolBar, "Add",
				SWT.NONE);
		itemAdd.setImage(ExtendedImageRegistry.getInstance().getImage(
				KommaEditUIPropertiesPlugin.INSTANCE
						.getImage(IEditUIPropertiesImages.INDIVIDUAL_ADD)));
		itemAdd.setToolTipText("Add property value");
		itemAdd.addSelectionListener(new AddButtonListener());
		itemAdd.setLayoutData(new GridData());

		Button itemRemove = getWidgetFactory().createButton(toolBar, "Remove",
				SWT.NONE);
		itemRemove.setImage(ExtendedImageRegistry.getInstance().getImage(
				KommaEditUIPropertiesPlugin.INSTANCE
						.getImage(IEditUIPropertiesImages.INDIVIDUAL_REMOVE)));
		itemRemove.setToolTipText("Remove property balue");
		itemRemove.addSelectionListener(new RemoveButtonListener());
		itemRemove.setLayoutData(new GridData());

		Button itemEdit = getWidgetFactory().createButton(toolBar, "Edit",
				SWT.NONE);
		itemEdit.setImage(ExtendedImageRegistry.getInstance().getImage(
				KommaEditUIPropertiesPlugin.INSTANCE
						.getImage(IEditUIPropertiesImages.CHECKED)));
		itemEdit.setToolTipText("Edit property value");
		itemEdit.addSelectionListener(new EditButtonListener());
		itemEdit.setLayoutData(new GridData());
	}

	private void createTree(Composite parent) {
		final Tree tree = getWidgetFactory().createTree(parent,
				SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
		tree.setHeaderVisible(true);
		tree.setLinesVisible(true);

		GridData treeGridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		tree.setLayoutData(treeGridData);

		treeViewer = new TreeViewer(tree) {
			public boolean getExpandedState(Object elementOrTreePath) {
				if (cachedExpandedElements.contains(elementOrTreePath)) {
					return true;
				}
				return super.getExpandedState(elementOrTreePath);
			};
		};
		treeViewer.setUseHashlookup(true);
		enableToolTips(treeViewer);
		treeViewer.setContentProvider(contentProvider);

		TreeViewerColumn column = new TreeViewerColumn(treeViewer, SWT.LEFT);
		column.getColumn().setText("Property");
		column.getColumn().setWidth(300);
		column.setLabelProvider(new TreeLabelProvider(ColumnType.PROPERTY));

		editingSupports = new ArrayList<>();
		ValueEditingSupport editingSupport = new ValueEditingSupport(
				treeViewer, PropertyEditingHelper.Type.PROPERTY);
		editingSupports.add(editingSupport);
		column.setEditingSupport(editingSupport);

		column = new TreeViewerColumn(treeViewer, SWT.LEFT);
		column.getColumn().setAlignment(SWT.LEFT);
		column.getColumn().setText("Value");
		column.getColumn().setWidth(200);
		column.setLabelProvider(new TreeLabelProvider(ColumnType.VALUE));

		editingSupport = new ValueEditingSupport(treeViewer);
		editingSupports.add(editingSupport);
		column.setEditingSupport(editingSupport);

		column = new TreeViewerColumn(treeViewer, SWT.CENTER);
		column.getColumn().setText("lang/type");
		column.setLabelProvider(new TreeLabelProvider(
				ColumnType.LITERAL_LANG_TYPE));
		column.getColumn().setWidth(80);

		editingSupport = new ValueEditingSupport(treeViewer,
				PropertyEditingHelper.Type.LITERAL_LANG_TYPE);
		editingSupports.add(editingSupport);
		column.setEditingSupport(editingSupport);

		column = new TreeViewerColumn(treeViewer, SWT.CENTER);
		column.getColumn().setText("Inferred");
		column.setLabelProvider(new TreeLabelProvider(ColumnType.INFERRED));
		column.getColumn().setWidth(50);

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
		treeViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(final DoubleClickEvent event) {
				asyncExec(new Runnable() {
					public void run() {
						addPropertyValueAction.run();
					}
				});
			}
		});

		final MenuManager menuManager = new MenuManager();
		menuManager.setRemoveAllWhenShown(true);
		menuManager.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(IMenuManager manager) {
				menuManager.add(addPropertyAction);
				if (((IStructuredSelection) treeViewer.getSelection())
						.getFirstElement() instanceof PropertyNode) {
					menuManager.add(addPropertyValueAction);
				}
			}
		});

		Menu menu = menuManager.createContextMenu(treeViewer.getTree());
		treeViewer.getTree().setMenu(menu);

		if (context != null) {
			anonymousFilter = new AnonymousFilter();
			anonymousFilter.setExcludeAnonymous(context.excludeAnonymous());
			treeViewer.setFilters(new ViewerFilter[] { anonymousFilter });
		}
	}

	// support for RAP
	private void enableToolTips(Viewer viewer) {
		try {
			Method enableFor = ColumnViewerToolTipSupport.class.getMethod(
					"enableFor", ColumnViewer.class);
			enableFor.invoke(null, viewer);
		} catch (Exception e) {
			// ignore
		}
	}

	@Override
	public void initialize(IEditorForm form) {
		super.initialize(form);

		this.contentProvider = new PropertyTreeContentProvider();
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if ("excludeAnonymous".equals(event.getProperty())) {
			anonymousFilter.setExcludeAnonymous(context.excludeAnonymous());
			refresh();
		} else if ("excludeInference".equals(event.getProperty())) {
			contentProvider.setIncludeInferred(!context.excludeInferred());
			refresh();
		}
	}

	@Override
	public void refresh() {
		IAdapterFactory newAdapterFactory = getAdapterFactory();
		if (adapterFactory == null || !adapterFactory.equals(newAdapterFactory)) {
			adapterFactory = newAdapterFactory;

			labelProvider = new AdapterFactoryLabelProvider(getAdapterFactory());
			for (ValueEditingSupport editingSupport : editingSupports) {
				editingSupport.setEditingDomain(getEditingDomain());
			}
			// createContextMenuFor(treeViewer);
		}

		cachedExpandedElements = Arrays
				.asList(treeViewer.getExpandedElements());

		treeViewer.setInput(resource);
		setUriText();
		changeUri.setEnabled(false);
		super.refresh();

		treeViewer.setExpandedElements(cachedExpandedElements.toArray());
		cachedExpandedElements = Collections.emptyList();
	}

	@Override
	public boolean setEditorInput(Object input) {
		if (input instanceof IModel) {
			input = ((IModel) input).getOntology();
		}
		if (input == null || input instanceof IObject) {
			resource = (IObject) input;
			setStale(true);
			return true;
		}
		return super.setEditorInput(input);
	}

	public void setInput(Object input) {
		setEditorInput(input);
	}

	void setUriText() {
		if (resource != null) {
			if (resource.getURI() == null) {
				uriText.setText(ModelUtil.getLabel(resource));
				uriText.setEnabled(false);
				itemShowFull.setEnabled(false);
			} else {
				uriText.setEnabled(true);
				itemShowFull.setEnabled(true);

				if (itemShowFull.getSelection()) {
					uriText.setText(resource.getURI().toString());
				} else {
					uriText.setText(resource.getURI().localPart());
				}
			}
		}
		changeUri.setEnabled(false);
	}
}
