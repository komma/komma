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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;

import net.enilink.commons.ui.dialogs.ListDialog;
import net.enilink.vocab.owl.Ontology;
import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.common.command.CommandResult;
import net.enilink.komma.common.command.SimpleCommand;
import net.enilink.komma.concepts.IResource;
import net.enilink.komma.edit.ui.properties.IEditUIPropertiesImages;
import net.enilink.komma.edit.ui.properties.KommaEditUIPropertiesPlugin;
import net.enilink.komma.edit.ui.provider.AdapterFactoryContentProvider;
import net.enilink.komma.edit.ui.provider.AdapterFactoryLabelProvider;
import net.enilink.komma.edit.ui.provider.ExtendedImageRegistry;
import net.enilink.komma.edit.ui.provider.reflective.ObjectComparator;
import net.enilink.komma.edit.ui.views.AbstractEditingDomainPart;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IObject;
import net.enilink.komma.model.ModelCore;
import net.enilink.komma.model.ModelDescription;
import net.enilink.komma.owl.edit.IOWLEditImages;
import net.enilink.komma.owl.edit.OWLEditPlugin;
import net.enilink.komma.owl.editor.OWLEditorPlugin;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIImpl;

public class ImportsPart extends AbstractEditingDomainPart {
	private Ontology ontology;
	private TreeViewer importsViewer;
	Action deleteItemAction, addItemAction;

	private Collection<ModelDescription> modelDescriptions;

	private IAdapterFactory adapterFactory;

	public void createContents(Composite parent) {
		parent.setLayout(new FillLayout());

		createActions();

		Tree tree = getWidgetFactory().createTree(parent,
				SWT.V_SCROLL | SWT.MULTI);

		importsViewer = new TreeViewer(tree);
		importsViewer.setComparator(new ObjectComparator());

		importsViewer
				.addSelectionChangedListener(new ISelectionChangedListener() {
					public void selectionChanged(SelectionChangedEvent event) {
						boolean deleteEnabled = false;
						for (Object element : ((IStructuredSelection) event
								.getSelection()).toArray()) {
							if (ontology.getOwlImports().contains(element)) {
								deleteEnabled = true;
								break;
							}
						}
						if (deleteItemAction != null)
							deleteItemAction.setEnabled(deleteEnabled);
					}
				});

		importsViewer
				.addSelectionChangedListener(new ISelectionChangedListener() {
					@Override
					public void selectionChanged(SelectionChangedEvent event) {
						getForm().fireSelectionChanged(ImportsPart.this,
								event.getSelection());
					}
				});

		modelDescriptions = ModelCore.getBaseModels();
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
		ListDialog dialog = new ListDialog(getShell());
		dialog.setTitle("Add Import");
		dialog.setTitle("Select ontology to import.");
		dialog.setLabelProvider(new LabelProvider() {
			@Override
			public Image getImage(Object element) {
				return ExtendedImageRegistry.getInstance().getImage(
						OWLEditPlugin.INSTANCE
								.getImage(IOWLEditImages.ONTOLOGY));
			}

			@Override
			public String getText(Object element) {
				if (element instanceof ModelDescription) {
					String prefix = ((ModelDescription) element).getPrefix();
					if (prefix != null) {
						return prefix;
					}
					return ((ModelDescription) element).getNamespace();
				}
				return super.getText(element);
			}

		});
		dialog.setElements(filterExistingDescriptions(modelDescriptions)
				.toArray());
		dialog.setSingleSelectionMode(false);
		if (dialog.open() == Window.OK) {
			final Object[] selectedElements = dialog.getResult();
			if (selectedElements != null) {
				try {
					getEditingDomain().getCommandStack().execute(
							new SimpleCommand() {
								IModel model;

								@Override
								protected CommandResult doExecuteWithResult(
										IProgressMonitor progressMonitor,
										IAdaptable info)
										throws ExecutionException {
									model = ((IObject) ontology).getModel();
									for (Object element : selectedElements) {
										ModelDescription modelDescription = (ModelDescription) element;

										try {

											model.addImport(URIImpl
													.createURI(modelDescription
															.getUri()),
													modelDescription
															.getPrefix());
										} catch (Exception ex) {
											OWLEditorPlugin.INSTANCE.log(ex);
										}
									}

									// ensure update of imports
									reloadManager(model);

									return CommandResult.newOKCommandResult();
								}

								@Override
								protected CommandResult doRedoWithResult(
										IProgressMonitor progressMonitor,
										IAdaptable info)
										throws ExecutionException {
									if (model != null) {
										reloadManager(model);
									}
									return CommandResult.newOKCommandResult();
								}

								@Override
								protected CommandResult doUndoWithResult(
										IProgressMonitor progressMonitor,
										IAdaptable info)
										throws ExecutionException {
									if (model != null) {
										reloadManager(model);
									}
									return CommandResult.newOKCommandResult();
								}
							}, null, null);
				} catch (ExecutionException exception) {
					OWLEditorPlugin.INSTANCE.log(exception);
				}
			}
		}
	}

	void deleteItem() {
		final Object[] selectedElements = ((IStructuredSelection) importsViewer
				.getSelection()).toArray();
		try {
			getEditingDomain().getCommandStack().execute(new SimpleCommand() {
				IModel model;

				@Override
				protected CommandResult doExecuteWithResult(
						IProgressMonitor progressMonitor, IAdaptable info)
						throws ExecutionException {
					model = ((IObject) ontology).getModel();

					for (Object element : selectedElements) {
						IResource importedOntology = (IResource) element;
						URI importedOntUri = importedOntology.getURI();
						if (!ontology.getOwlImports().contains(element)) {
							continue;
						}
						try {
							((IObject) ontology).getModel().removeImport(
									importedOntUri);
						} catch (Exception ex) {
							OWLEditorPlugin.INSTANCE.log(ex);
						}
					}

					// ensure update of imports
					reloadManager(model);

					return CommandResult.newOKCommandResult();
				}

				@Override
				protected CommandResult doRedoWithResult(
						IProgressMonitor progressMonitor, IAdaptable info)
						throws ExecutionException {
					if (model != null) {
						reloadManager(model);
					}
					return CommandResult.newOKCommandResult();
				}

				@Override
				protected CommandResult doUndoWithResult(
						IProgressMonitor progressMonitor, IAdaptable info)
						throws ExecutionException {
					if (model != null) {
						reloadManager(model);
					}
					return CommandResult.newOKCommandResult();
				}
			}, null, null);
		} catch (ExecutionException exception) {
			OWLEditorPlugin.INSTANCE.log(exception);
		}
	}

	void reloadManager(IModel model) {
		model.getManager().close();
		model.getManager();
	}

	public Collection<ModelDescription> filterExistingDescriptions(
			Collection<ModelDescription> descriptions) {
		Set<? extends IEntity> importedOntologies = ontology != null ? ontology
				.getOwlImports() : null;
		if (importedOntologies == null) {
			importedOntologies = Collections.emptySet();
		}
		Set<String> importedOntologyUris = new HashSet<String>(
				importedOntologies.size());
		for (IEntity ont : importedOntologies) {
			importedOntologyUris.add(ont.getURI().namespace().toString());
		}

		List<ModelDescription> additionalDescriptions = new ArrayList<ModelDescription>();
		for (ModelDescription description : descriptions) {
			if (!importedOntologyUris.contains(description.getNamespace())) {
				additionalDescriptions.add(description);
			}
		}
		return additionalDescriptions;
	}

	public void setInput(Object input) {
		if (input instanceof IModel) {
			ontology = ((IModel) input).getOntology();
		} else if (input instanceof IObject) {
			ontology = ((IObject) input).getModel().getOntology();
		} else {
			ontology = null;
		}
		setStale(true);
	}

	@Override
	public void refresh() {
		IAdapterFactory newAdapterFactory = getAdapterFactory();
		if (adapterFactory == null || !adapterFactory.equals(newAdapterFactory)) {
			adapterFactory = newAdapterFactory;

			importsViewer.setContentProvider(new AdapterFactoryContentProvider(
					getAdapterFactory()));
			importsViewer.setLabelProvider(new AdapterFactoryLabelProvider(
					getAdapterFactory()));

			createContextMenuFor(importsViewer);
		}

		importsViewer.setInput(ontology);

		super.refresh();
	}
}
