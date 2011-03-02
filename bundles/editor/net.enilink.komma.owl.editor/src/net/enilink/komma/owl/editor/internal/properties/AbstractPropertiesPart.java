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
package net.enilink.komma.owl.editor.internal.properties;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;

import net.enilink.vocab.owl.OWL;
import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.common.command.CommandResult;
import net.enilink.komma.common.command.SimpleCommand;
import net.enilink.komma.concepts.IProperty;
import net.enilink.komma.concepts.IResource;
import net.enilink.komma.edit.ui.properties.IEditUIPropertiesImages;
import net.enilink.komma.edit.ui.properties.KommaEditUIPropertiesPlugin;
import net.enilink.komma.edit.ui.provider.AdapterFactoryContentProvider;
import net.enilink.komma.edit.ui.provider.AdapterFactoryLabelProvider;
import net.enilink.komma.edit.ui.provider.ExtendedImageRegistry;
import net.enilink.komma.edit.ui.provider.reflective.ObjectComparator;
import net.enilink.komma.edit.ui.views.AbstractEditingDomainPart;
import net.enilink.komma.edit.ui.wizards.NewObjectWizard;
import net.enilink.komma.model.IModel;
import net.enilink.komma.owl.editor.OWLEditorPlugin;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.URI;

public abstract class AbstractPropertiesPart extends AbstractEditingDomainPart {
	Action deleteItemAction, addItemAction;
	protected Tree tree;
	protected TreeViewer treeViewer;
	protected IModel model;
	protected IAdapterFactory adapterFactory;

	abstract protected String getName();

	abstract protected URI getPropertyType();

	@Override
	public void createContents(Composite parent) {
		parent.setLayout(new FillLayout());
		createActions();

		tree = getWidgetFactory().createTree(parent, SWT.NONE);

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
		NewObjectWizard wizard = new NewObjectWizard(model) {
			@Override
			public boolean performFinish() {
				final String resourceName = getObjectName();
				try {
					getEditingDomain().getCommandStack().execute(
							new SimpleCommand() {
								@Override
								protected CommandResult doExecuteWithResult(
										IProgressMonitor progressMonitor,
										IAdaptable info)
										throws ExecutionException {
									final IEntity property = model
											.getManager()
											.createNamed(
													model.getURI()
															.appendFragment(
																	resourceName),
													getPropertyType());

									getShell().getDisplay().asyncExec(
											new Runnable() {
												@Override
												public void run() {
													treeViewer
															.setSelection(new StructuredSelection(
																	property));
												}
											});

									return CommandResult.newOKCommandResult(property);
								}
							}, null, null);
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
			getEditingDomain().getCommandStack().execute(new SimpleCommand() {
				@Override
				protected CommandResult doExecuteWithResult(
						IProgressMonitor progressMonitor, IAdaptable info)
						throws ExecutionException {

					final Object selected = ((IStructuredSelection) treeViewer.getSelection()).getFirstElement();

					if (selected instanceof IResource) {
						((IResource) selected).getEntityManager().remove(
								(IResource) selected);
						getShell().getDisplay().asyncExec(new Runnable() {
							@Override
							public void run() {
								treeViewer
										.setSelection(new StructuredSelection(
												selected));
							}
						});
						return CommandResult.newOKCommandResult(selected);
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
						.setContentProvider(new AdapterFactoryContentProvider(
								adapterFactory) {
							@Override
							public Object[] getElements(Object object) {
								if (object instanceof Object[]) {
									return (Object[]) object;
								}
								return super.getElements(object);
							}
						});
				treeViewer.setLabelProvider(new AdapterFactoryLabelProvider(
						adapterFactory));

				createContextMenuFor(treeViewer);
			}

			treeViewer.setInput(new Object[] { model.getManager().find(
					OWL.TYPE_THING) });
		} else if (adapterFactory != null) {
			treeViewer.setInput(new Object[0]);
		}
	}
}
