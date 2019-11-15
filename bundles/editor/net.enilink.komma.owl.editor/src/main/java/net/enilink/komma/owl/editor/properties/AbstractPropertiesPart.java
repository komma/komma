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
package net.enilink.komma.owl.editor.properties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.common.command.CommandResult;
import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.common.command.SimpleCommand;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.Statement;
import net.enilink.komma.core.URI;
import net.enilink.komma.edit.ui.properties.IEditUIPropertiesImages;
import net.enilink.komma.edit.ui.properties.KommaEditUIPropertiesPlugin;
import net.enilink.komma.edit.ui.provider.AdapterFactoryContentProvider;
import net.enilink.komma.edit.ui.provider.AdapterFactoryLabelProvider;
import net.enilink.komma.edit.ui.provider.ExtendedImageRegistry;
import net.enilink.komma.edit.ui.provider.reflective.ObjectComparator;
import net.enilink.komma.edit.ui.util.SearchWidget;
import net.enilink.komma.edit.ui.views.AbstractEditingDomainPart;
import net.enilink.komma.edit.ui.wizards.NewObjectWizard;
import net.enilink.komma.em.concepts.IProperty;
import net.enilink.komma.em.concepts.IResource;
import net.enilink.komma.model.IModel;
import net.enilink.komma.owl.editor.OWLEditorPlugin;
import net.enilink.vocab.owl.AnnotationProperty;
import net.enilink.vocab.owl.OWL;
import net.enilink.vocab.owl.OntologyProperty;
import net.enilink.vocab.rdf.RDF;
import net.enilink.vocab.rdfs.RDFS;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Tree;

public abstract class AbstractPropertiesPart extends AbstractEditingDomainPart {
	Action deleteItemAction, addItemAction;
	protected Tree tree;
	protected TreeViewer treeViewer;
	protected IModel model;
	protected IAdapterFactory adapterFactory;

	protected boolean hideBuiltins = true;

	abstract protected String getName();

	abstract protected URI getPropertyType();

	abstract protected URI getRootProperty();

	@Override
	public void createContents(Composite parent) {
		GridLayout gridLayout = new GridLayout(1, false);
		gridLayout.marginWidth = gridLayout.marginHeight = 0;
		parent.setLayout(gridLayout);
		createActions(parent);

		tree = getWidgetFactory().createTree(parent, SWT.MULTI);
		tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		treeViewer = new TreeViewer(tree);
		treeViewer.setUseHashlookup(true);
		treeViewer.setComparator(new ObjectComparator());
		treeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				getForm().fireSelectionChanged(AbstractPropertiesPart.this,
						event.getSelection());
			}
		});
		treeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				if (deleteItemAction != null)
					deleteItemAction
							.setEnabled(!event.getSelection().isEmpty());
			}
		});

		SearchWidget searchWidget = new SearchWidget();
		searchWidget.setViewer(treeViewer);
		searchWidget.createControl(parent);
		searchWidget.getControl().setLayoutData(
				new GridData(SWT.FILL, SWT.END, false, false));
	}

	public void createActions(Composite parent) {
		IToolBarManager toolBarManager = (IToolBarManager) getForm()
				.getAdapter(IToolBarManager.class);
		ToolBarManager ownManager = null;
		if (toolBarManager == null) {
			toolBarManager = ownManager = new ToolBarManager(SWT.HORIZONTAL);
			ToolBar toolBar = ownManager.createControl(parent);
			getWidgetFactory().adapt(toolBar);
			toolBar.setLayoutData(new GridData(SWT.RIGHT, SWT.DEFAULT, true,
					false));
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

		IAction refreshAction = new Action("Refresh") {
			@Override
			public void run() {
				refresh();
			}
		};
		refreshAction.setImageDescriptor(ExtendedImageRegistry.getInstance()
				.getImageDescriptor(
						KommaEditUIPropertiesPlugin.INSTANCE
								.getImage(IEditUIPropertiesImages.REFRESH)));
		toolBarManager.add(refreshAction);

		final IAction hideAction = new Action("Hide built-in properties",
				Action.AS_CHECK_BOX) {
			@Override
			public void run() {
				hideBuiltins = !hideBuiltins;
				setChecked(hideBuiltins);
				refresh();
			}
		};
		hideAction
				.setImageDescriptor(ExtendedImageRegistry
						.getInstance()
						.getImageDescriptor(
								KommaEditUIPropertiesPlugin.INSTANCE
										.getImage(IEditUIPropertiesImages.HIDE_BUILTINS)));
		hideAction.setChecked(hideBuiltins);
		toolBarManager.add(hideAction);

		if (ownManager != null) {
			ownManager.update(true);
		}
	}

	void addItem() {
		NewObjectWizard wizard = new NewObjectWizard(model) {
			@Override
			public boolean performFinish() {
				final URI resourceName = getObjectName();
				try {
					ICommand cmd = new SimpleCommand("Add property") {
						@Override
						protected CommandResult doExecuteWithResult(
								IProgressMonitor progressMonitor,
								IAdaptable info) throws ExecutionException {
							final IEntity property = model.getManager()
									.createNamed(resourceName,
											getPropertyType());
							// add inferred property
							model.getManager().add(
									new Statement(property,
											RDFS.PROPERTY_SUBPROPERTYOF,
											getRootProperty(), true));
							return CommandResult.newOKCommandResult(property);
						}
					};
					getEditingDomain().getCommandStack().execute(cmd, null,
							null);
					// update viewer and set selection
					// refresh();
					treeViewer.setSelection(new StructuredSelection(cmd
							.getCommandResult().getReturnValue()), true);
				} catch (ExecutionException e) {
					OWLEditorPlugin.INSTANCE.log(e);
				}

				return true;
			}
		};

		WizardDialog wizardDialog = new WizardDialog(getShell(), wizard);
		wizardDialog.open();
	}

	void deleteItem() {
		try {
			getEditingDomain().getCommandStack().execute(
					new SimpleCommand("Delete property") {
						@Override
						protected CommandResult doExecuteWithResult(
								IProgressMonitor progressMonitor,
								IAdaptable info) throws ExecutionException {
							final Object selected = ((IStructuredSelection) treeViewer.getSelection()).getFirstElement();
							if (selected instanceof IResource) {
								((IResource) selected).getEntityManager()
										.remove((IResource) selected);
								// add inferred property
								model.getManager().remove(
										new Statement((IResource) selected,
												RDFS.PROPERTY_SUBPROPERTYOF,
												getRootProperty(), true));
								return CommandResult
										.newOKCommandResult(selected);
							}
							return CommandResult.newCancelledCommandResult();
						}
					}, null, null);
		} catch (ExecutionException e) {
			OWLEditorPlugin.INSTANCE.log(e);
		}
	}

	@Override
	public boolean setFocus() {
		if (tree != null && tree.setFocus()) {
			return true;
		}
		return super.setFocus();
	}

	@Override
	public boolean setEditorInput(Object input) {
		if (input instanceof IProperty && treeViewer != null) {
			treeViewer.setSelection(new StructuredSelection(input), true);
		}
		return super.setEditorInput(input);
	}

	@Override
	public void setInput(Object input) {
		if (treeViewer != null && adapterFactory != null) {
			treeViewer.setInput(null);
		}
		this.model = (IModel) input;
		setStale(true);
	}

	protected Object[] filterElements(Object parent, Object[] elements) {
		if (hideBuiltins && getRootProperty().equals(parent)) {
			List<Object> list = new ArrayList<>(Arrays.asList(elements));
			for (Iterator<?> it = list.iterator(); it.hasNext();) {
				Object element = it.next();
				if (element instanceof IReference) {
					URI uri = ((IReference) element).getURI();
					if (uri != null
							&& RDF.NAMESPACE_URI.equals(uri.namespace())
							|| OWL.NAMESPACE_URI.equals(uri.namespace())
							&& !(element instanceof AnnotationProperty || element instanceof OntologyProperty)) {
						it.remove();
					}
				}
			}
			return list.toArray();
		}
		return elements;
	}

	@Override
	public void refresh() {
		if (model != null) {
			IAdapterFactory newAdapterFactory = getAdapterFactory();
			if (adapterFactory == null
					|| !adapterFactory.equals(newAdapterFactory)) {
				adapterFactory = newAdapterFactory;
				treeViewer
						.setContentProvider(new AdapterFactoryContentProvider(
								adapterFactory) {
							@Override
							public Object[] getElements(Object object) {
								if (object instanceof Object[]) {
									return filterElements(object,
											(Object[]) object);
								}
								return filterElements(object,
										super.getElements(object));
							}
						});
				treeViewer
						.setLabelProvider(new AdapterFactoryLabelProvider.ColorProvider(
								adapterFactory, treeViewer));
			}
			createContextMenuFor(treeViewer);
			treeViewer.setInput(model.getManager().find(getRootProperty()));
		}
	}
}
