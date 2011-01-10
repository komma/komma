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
package net.enilink.komma.owl.editor.internal.individuals;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;

import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.common.command.CommandResult;
import net.enilink.komma.common.command.SimpleCommand;
import net.enilink.komma.concepts.IClass;
import net.enilink.komma.concepts.IResource;
import net.enilink.komma.edit.ui.properties.IEditUIPropertiesImages;
import net.enilink.komma.edit.ui.properties.KommaEditUIPropertiesPlugin;
import net.enilink.komma.edit.ui.provider.AdapterFactoryLabelProvider;
import net.enilink.komma.edit.ui.provider.ExtendedImageRegistry;
import net.enilink.komma.edit.ui.provider.reflective.IndividualsContentProvider;
import net.enilink.komma.edit.ui.provider.reflective.ObjectComparator;
import net.enilink.komma.edit.ui.views.AbstractEditingDomainPart;
import net.enilink.komma.edit.ui.wizards.NewObjectWizard;
import net.enilink.komma.model.IObject;
import net.enilink.komma.owl.editor.OWLEditorPlugin;
import net.enilink.komma.core.IReference;

public class IndividualsPart extends AbstractEditingDomainPart {
	private StructuredViewer viewer;
	private IAdapterFactory adapterFactory;
	private IClass input;
	Action deleteItemAction, addItemAction;

	@Override
	public void createContents(Composite parent) {
		parent.setLayout(new FillLayout());
		createActions();
		createIndividualsPart(parent);
	}

	private void createIndividualsPart(Composite parent) {
		viewer = createViewer(parent);

		viewer.setComparator(new ObjectComparator());
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				getForm().fireSelectionChanged(IndividualsPart.this,
						event.getSelection());
			}
		});
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				if (deleteItemAction != null) {
					deleteItemAction
							.setEnabled(!event.getSelection().isEmpty());
				}
			}
		});
	}

	protected StructuredViewer createViewer(Composite parent) {
		Table table = getWidgetFactory().createTable(parent,
				SWT.V_SCROLL | SWT.VIRTUAL);

		viewer = new TableViewer(table);
		viewer.setContentProvider(new IndividualsContentProvider());

		return viewer;
	}

	protected StructuredViewer getViewer() {
		return viewer;
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
		Object type = viewer.getInput();
		if (type instanceof IClass) {
			final IClass parent = (IClass) type;
			NewObjectWizard wizard = new NewObjectWizard(
					((IObject) parent).getModel()) {
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
										final IResource individual = parent.newInstance(model
												.getURI().appendFragment(
														resourceName));

										getShell().getDisplay().asyncExec(
												new Runnable() {
													@Override
													public void run() {
														viewer.setSelection(new StructuredSelection(
																individual));
													}
												});

										return CommandResult.newOKCommandResult(individual);
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
	}

	void editItem() {
		// TODO
	}

	void deleteItem() {
		try {
			getEditingDomain().getCommandStack().execute(new SimpleCommand() {
				@Override
				protected CommandResult doExecuteWithResult(
						IProgressMonitor progressMonitor, IAdaptable info)
						throws ExecutionException {
					final Object selected = ((IStructuredSelection) viewer.getSelection()).getFirstElement();

					if (selected instanceof IResource) {
						((IResource) selected).getEntityManager().remove(
								(IResource) selected);
						getShell().getDisplay().asyncExec(new Runnable() {
							@Override
							public void run() {
								viewer.setSelection(new StructuredSelection(
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
		if (viewer != null && viewer.getControl().setFocus()) {
			return true;
		}
		return super.setFocus();
	}

	public void reveal(IResource resource) {
		if (resource instanceof IObject) {
			viewer.setInput(resource);
			refresh();
		}
	}

	@Override
	public boolean setEditorInput(Object input) {
		if (input == null || input instanceof IClass) {
			this.input = (IClass) input;
			setStale(true);
			return true;
		} else if (input instanceof IReference) {
			if (viewer != null) {
				viewer.setSelection(new StructuredSelection(input), true);
			}
		}
		return false;
	}

	@Override
	public void refresh() {
		IAdapterFactory newAdapterFactory = getAdapterFactory();
		if (adapterFactory == null || !adapterFactory.equals(newAdapterFactory)) {
			adapterFactory = newAdapterFactory;

			adapterFactoryChanged();
		}

		setInputToViewer(viewer, input);
	}

	protected void setInputToViewer(StructuredViewer viewer, IClass input) {
		viewer.setInput(input);
	}

	protected void adapterFactoryChanged() {
		viewer.setLabelProvider(new AdapterFactoryLabelProvider(
				getAdapterFactory()));

		createContextMenuFor(viewer);
	}
}
