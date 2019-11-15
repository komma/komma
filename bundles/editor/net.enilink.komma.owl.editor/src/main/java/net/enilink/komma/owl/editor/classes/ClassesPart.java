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
package net.enilink.komma.owl.editor.classes;

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
import net.enilink.komma.edit.ui.provider.AdapterFactoryLabelProvider;
import net.enilink.komma.edit.ui.provider.ExtendedImageRegistry;
import net.enilink.komma.edit.ui.provider.LazyAdapterFactoryContentProvider;
import net.enilink.komma.edit.ui.util.SearchWidget;
import net.enilink.komma.edit.ui.views.AbstractEditingDomainPart;
import net.enilink.komma.edit.ui.wizards.NewObjectWizard;
import net.enilink.komma.em.concepts.IClass;
import net.enilink.komma.em.concepts.IResource;
import net.enilink.komma.em.util.KommaUtil;
import net.enilink.komma.model.IModel;
import net.enilink.komma.owl.editor.OWLEditorPlugin;
import net.enilink.vocab.owl.OWL;
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

public class ClassesPart extends AbstractEditingDomainPart {
	protected Tree tree;
	protected TreeViewer treeViewer;
	protected IModel model;
	private IAdapterFactory adapterFactory;
	Action deleteItemAction, addItemAction;
	protected boolean hideBuiltins = true;

	@Override
	public void createContents(Composite parent) {
		GridLayout gridLayout = new GridLayout(1, false);
		gridLayout.marginWidth = gridLayout.marginHeight = 0;
		parent.setLayout(gridLayout);
		createActions(parent);

		tree = getWidgetFactory().createTree(parent, SWT.VIRTUAL | SWT.MULTI);
		tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		treeViewer = new TreeViewer(tree);
		treeViewer.setUseHashlookup(true);

		treeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				getForm().fireSelectionChanged(ClassesPart.this,
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

		final IAction hideAction = new Action("Hide built-in classes",
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
					ICommand cmd = new SimpleCommand("Create class") {
						@Override
						protected CommandResult doExecuteWithResult(
								IProgressMonitor progressMonitor,
								IAdaptable info) throws ExecutionException {
							final IEntity entity = model.getManager()
									.createNamed(resourceName, OWL.TYPE_CLASS);
							model.getManager().add(
									new Statement(entity,
											RDFS.PROPERTY_SUBCLASSOF,
											OWL.TYPE_THING, true));
							return CommandResult.newOKCommandResult(entity);
						}
					};
					getEditingDomain().getCommandStack().execute(cmd, null,
							null);
					if (cmd.getCommandResult().getStatus().isOK()) {
						treeViewer.setSelection(new StructuredSelection(cmd
								.getCommandResult().getReturnValue()));
					}
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
			ICommand cmd = new SimpleCommand("Delete class") {
				@Override
				protected CommandResult doExecuteWithResult(
						IProgressMonitor progressMonitor, IAdaptable info)
						throws ExecutionException {
					final Object selected = ((IStructuredSelection) treeViewer
							.getSelection()).getFirstElement();
					if (selected instanceof IResource) {
						((IResource) selected).getEntityManager().remove(
								(IResource) selected);
						model.getManager().remove(
								new Statement((IResource) selected,
										RDFS.PROPERTY_SUBCLASSOF,
										OWL.TYPE_THING, true));
						return CommandResult.newOKCommandResult(selected);
					}
					return CommandResult.newCancelledCommandResult();
				}
			};
			getEditingDomain().getCommandStack().execute(cmd, null, null);

		} catch (ExecutionException e) {
			OWLEditorPlugin.INSTANCE.log(e);
		}
	}

	public boolean setFocus() {
		if (tree != null && tree.setFocus()) {
			return true;
		}
		return super.setFocus();
	}

	@Override
	public boolean setEditorInput(Object input) {
		if (input instanceof IClass && treeViewer != null) {
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

	@Override
	public void refresh() {
		if (model != null) {
			IAdapterFactory newAdapterFactory = getAdapterFactory();
			if (adapterFactory == null
					|| !adapterFactory.equals(newAdapterFactory)) {
				adapterFactory = newAdapterFactory;
				treeViewer
						.setContentProvider(new LazyAdapterFactoryContentProvider(
								getAdapterFactory()) {
							@Override
							protected Object[] internalGetChildren(
									Object element) {
								if (hideBuiltins
										&& (RDFS.TYPE_RESOURCE.equals(element) || OWL.TYPE_THING
												.equals(element))) {
									List<Object> children = new ArrayList<>(
											Arrays.asList(super
													.internalGetChildren(element)));
									for (Iterator<?> it = children.iterator(); it
											.hasNext();) {
										Object child = it.next();
										if (child instanceof IReference) {
											URI uri = ((IReference) child)
													.getURI();
											if (uri != null
													&& !(RDFS.TYPE_RESOURCE
															.equals(uri) || OWL.TYPE_THING
															.equals(uri))
													&& KommaUtil.isW3cNamespace(uri
															.namespace())) {
												it.remove();
											}
										}
									}
									return children.toArray();
								}
								return super.internalGetChildren(element);
							}

						});
				treeViewer
						.setLabelProvider(new AdapterFactoryLabelProvider.ColorProvider(
								getAdapterFactory(), treeViewer));
			}
			createContextMenuFor(treeViewer);

			treeViewer.setInput(new Object[] { model.getManager().find(
					RDFS.TYPE_RESOURCE //
					) });
		}

		super.refresh();
	}
}
