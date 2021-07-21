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
package net.enilink.komma.owl.editor.ontology;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.enilink.commons.iterator.WrappedIterator;
import net.enilink.composition.properties.traits.Filterable;
import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.common.command.CommandResult;
import net.enilink.komma.common.command.SimpleCommand;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIs;
import net.enilink.komma.edit.ui.dialogs.FilteredList;
import net.enilink.komma.edit.ui.properties.IEditUIPropertiesImages;
import net.enilink.komma.edit.ui.properties.KommaEditUIPropertiesPlugin;
import net.enilink.komma.edit.ui.provider.AdapterFactoryContentProvider;
import net.enilink.komma.edit.ui.provider.AdapterFactoryLabelProvider;
import net.enilink.komma.edit.ui.provider.ExtendedImageRegistry;
import net.enilink.komma.edit.ui.provider.reflective.ObjectComparator;
import net.enilink.komma.edit.ui.util.EditUIUtil;
import net.enilink.komma.edit.ui.views.AbstractEditingDomainPart;
import net.enilink.komma.em.concepts.IOntology;
import net.enilink.komma.em.concepts.IResource;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.model.IObject;
import net.enilink.komma.model.ModelDescription;
import net.enilink.komma.model.ModelPlugin;
import net.enilink.komma.model.base.IURIMapRule;
import net.enilink.komma.model.base.SimpleURIMapRule;
import net.enilink.komma.owl.edit.IOWLEditImages;
import net.enilink.komma.owl.edit.OWLEditPlugin;
import net.enilink.komma.owl.editor.OWLEditorPlugin;
import net.enilink.vocab.owl.Ontology;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SelectionDialog;

public class ImportsPart extends AbstractEditingDomainPart {
	private Ontology ontology;
	private TreeViewer importsViewer;
	private Action deleteItemAction, addItemAction;

	private Collection<ModelDescription> modelDescriptions;

	private IAdapterFactory adapterFactory;

	public void createContents(Composite parent) {
		parent.setLayout(new GridLayout(1, false));
		createActions(parent);

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
						if (deleteItemAction != null) {
							deleteItemAction.setEnabled(deleteEnabled);
						}
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
		importsViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				if (PlatformUI.isWorkbenchRunning()) {
					Object selected = ((IStructuredSelection) event
							.getSelection()).getFirstElement();
					try {
						EditUIUtil.openEditor((IReference) selected);
					} catch (PartInitException e) {
						// ignore
					}
				}
			}
		});
		importsViewer.getControl().setLayoutData(
				new GridData(SWT.FILL, SWT.FILL, true, true));
		modelDescriptions = ModelPlugin.getBaseModels();
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

		if (ownManager != null) {
			ownManager.update(true);
		}
	}

	void addItem() {
		final FilteredList list = new FilteredList() {
			@Override
			protected void updateStatus(IStatus status) {
			}

			@Override
			protected IStatus validateItem(Object item) {
				return Status.OK_STATUS;
			}

			@Override
			protected ItemsFilter createFilter() {
				final Set<String> existingImports = getExistingImports();
				return new ItemsFilter() {
					@Override
					public boolean isConsistentItem(Object item) {
						return true;
					}

					@Override
					public boolean matchItem(Object item) {
						// filter existing imports
						if (existingImports.contains(((ModelDescription) item)
								.getNamespace())) {
							return false;
						}
						return matches(getElementName(item));
					}

					@Override
					public boolean isSubFilter(ItemsFilter filter) {
						// never treat as subfilter due to using limit
						return false;
					}
				};
			}

			@Override
			protected Comparator<Object> getItemsComparator() {
				return new Comparator<Object>() {
					@Override
					public int compare(Object o1, Object o2) {
						return getElementName(o1).compareToIgnoreCase(
								getElementName(o2));
					}
				};
			}

			@SuppressWarnings("unchecked")
			@Override
			protected void fillContentProvider(
					AbstractContentProvider contentProvider,
					ItemsFilter itemsFilter, IProgressMonitor progressMonitor)
					throws CoreException {
				if (ontology != null) {
					Set<String> seen = new HashSet<String>();

					IModelSet modelSet = ((IObject) ontology).getModel()
							.getModelSet();
					try {
						modelSet.getUnitOfWork().begin();
						Set<IModel> models = modelSet.getModels();
						Iterator<IModel> filteredModels;
						if (models instanceof Filterable<?>) {
							String pattern = itemsFilter.getPattern()
									.replaceAll("\\*", "")
									.replaceAll("\\?", "");
							filteredModels = ((Filterable<IModel>) models)
									.filter(pattern, 20);
						} else {
							filteredModels = models.iterator();
						}
						for (IModel model : WrappedIterator
								.create(filteredModels)) {
							String uri = model.getURI().toString();
							if (seen.add(uri)) {
								contentProvider.add(new ModelDescription(null,
										uri), itemsFilter);
							}
						}
					} finally {
						modelSet.getUnitOfWork().end();
					}
					List<ModelDescription> descriptions = new ArrayList<ModelDescription>(
							modelDescriptions);
					for (IURIMapRule rule : ((IObject) ontology).getModel()
							.getModelSet().getURIConverter().getURIMapRules()) {
						if (rule instanceof SimpleURIMapRule) {
							String modelUri = ((SimpleURIMapRule) rule)
									.getPattern();
							descriptions.add(new ModelDescription(null,
									modelUri));
						}
					}
					for (ModelDescription description : descriptions) {
						if (seen.add(description.getUri())) {
							contentProvider.add(description, itemsFilter);
						}
					}
				}
			}

			@Override
			public String getElementName(Object item) {
				return ((ModelDescription) item).getUri();
			}
		};
		list.setListLabelProvider(new LabelProvider() {
			@Override
			public Image getImage(Object element) {
				return ExtendedImageRegistry.getInstance().getImage(
						OWLEditPlugin.INSTANCE
								.getImage(IOWLEditImages.ONTOLOGY));
			}

			@Override
			public String getText(Object element) {
				if (element instanceof ModelDescription) {
					return ((ModelDescription) element).getUri();
				}
				return super.getText(element);
			}

		});

		SelectionDialog dialog = new SelectionDialog(getShell()) {
			protected void okPressed() {
				// allow for textual input of URI
				if (list.getSelection().isEmpty()) {
					String input = ((Text) list.getPatternControl()).getText();
					try {
						URI uri = URIs.createURI(input);
						if (!uri.isRelative()) {
							setSelectionResult(new Object[] { new ModelDescription(
									null, uri.toString()) });
						}
					} catch (IllegalArgumentException e) {
						// ignore
					}
				} else {
					setSelectionResult(list.getSelection().toArray());
				}
				super.okPressed();
			}

			protected Control createDialogArea(Composite container) {
				Composite parent = (Composite) super
						.createDialogArea(container);
				createMessageArea(parent);
				Control listControl = list.createControl(parent);
				GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
				gridData.widthHint = convertWidthInCharsToPixels(55);
				gridData.heightHint = convertHeightInCharsToPixels(15);
				listControl.setLayoutData(gridData);
				applyDialogFont(parent);
				return parent;
			}
		};
		dialog.setTitle("Add Import");
		dialog.setTitle("Select ontology to import.");
		if (dialog.open() == Window.OK) {
			final Object[] selectedElements = dialog.getResult();
			if (selectedElements != null) {
				try {
					getEditingDomain().getCommandStack().execute(
							new SimpleCommand("Add import") {
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
											model.addImport(URIs
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
			getEditingDomain().getCommandStack().execute(
					new SimpleCommand("Delete import") {
						IModel model;

						@Override
						protected CommandResult doExecuteWithResult(
								IProgressMonitor progressMonitor,
								IAdaptable info) throws ExecutionException {
							model = ((IObject) ontology).getModel();
							for (Object element : selectedElements) {
								IResource importedOntology = (IResource) element;
								URI importedOntUri = importedOntology.getURI();
								if (!ontology.getOwlImports().contains(element)) {
									continue;
								}
								try {
									((IObject) ontology).getModel()
											.removeImport(importedOntUri);
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
								IAdaptable info) throws ExecutionException {
							if (model != null) {
								reloadManager(model);
							}
							return CommandResult.newOKCommandResult();
						}

						@Override
						protected CommandResult doUndoWithResult(
								IProgressMonitor progressMonitor,
								IAdaptable info) throws ExecutionException {
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

	public Set<String> getExistingImports() {
		Set<? extends IEntity> importedOntologies = ontology != null ? ontology
				.getOwlImports() : null;
		if (importedOntologies == null) {
			importedOntologies = Collections.emptySet();
		}
		Set<String> importedOntologyUris = new HashSet<String>(
				importedOntologies.size());
		for (IEntity ont : importedOntologies) {
			importedOntologyUris.add(ont.getURI().toString());
		}
		return importedOntologyUris;
	}

	@Override
	public boolean setEditorInput(Object input) {
		IOntology ontology = null;
		if (input instanceof IModel) {
			ontology = ((IModel) input).getOntology();
		} else if (input instanceof IObject) {
			ontology = ((IObject) input).getModel().getOntology();
		}
		if (ontology != null) {
			return setOntology(ontology);
		}
		return false;
	}

	protected boolean setOntology(IOntology ontology) {
		if (this.ontology != ontology) {
			this.ontology = ontology;
			setStale(true);
			return true;
		}
		return false;
	}

	public void setInput(Object input) {
		if (input == null) {
			setOntology(null);
		} else {
			setEditorInput(input);
		}
	}

	@Override
	public void refresh() {
		IAdapterFactory newAdapterFactory = getAdapterFactory();
		if (adapterFactory == null || !adapterFactory.equals(newAdapterFactory)) {
			adapterFactory = newAdapterFactory;
			importsViewer.setContentProvider(new AdapterFactoryContentProvider(
					getAdapterFactory()));
			importsViewer.setLabelProvider(new AdapterFactoryLabelProvider(
					getAdapterFactory()) {
				@Override
				public String getText(Object object) {
					if (object instanceof IReference) {
						URI uri = ((IReference) object).getURI();
						if (uri != null) {
							String label = null;
							if (object instanceof IResource) {
								label = ((IResource) object).getRdfsLabel();
							}
							return uri.toString()
									+ (label != null ? " [" + label + "]" : "");
						}
					}
					return super.getText(object);
				}
			});
			createContextMenuFor(importsViewer);
		}
		importsViewer.setInput(ontology);
		super.refresh();
	}
}
