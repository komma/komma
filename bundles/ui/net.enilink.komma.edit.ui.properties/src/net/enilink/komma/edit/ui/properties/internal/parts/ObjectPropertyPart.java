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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.ToolBar;

import net.enilink.commons.ui.editor.IEditorForm;
import net.enilink.vocab.rdf.Property;
import net.enilink.vocab.rdfs.Resource;
import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.concepts.IClass;
import net.enilink.komma.concepts.IProperty;
import net.enilink.komma.concepts.IResource;
import net.enilink.komma.edit.ui.KommaEditUIPlugin;
import net.enilink.komma.edit.ui.action.CreateObjectAction;
import net.enilink.komma.edit.ui.dialogs.FilteredList.AbstractContentProvider;
import net.enilink.komma.edit.ui.dialogs.FilteredList.ItemsFilter;
import net.enilink.komma.edit.ui.dialogs.FilteredTreeAndListSelectionDialog;
import net.enilink.komma.edit.ui.properties.IEditUIPropertiesImages;
import net.enilink.komma.edit.ui.properties.KommaEditUIPropertiesPlugin;
import net.enilink.komma.edit.ui.provider.AdapterFactoryLabelProvider;
import net.enilink.komma.edit.ui.provider.ExtendedImageRegistry;
import net.enilink.komma.edit.ui.provider.reflective.ClassHierarchyContentProvider;
import net.enilink.komma.edit.ui.provider.reflective.PropertyValuesContentProvider;
import net.enilink.komma.edit.ui.views.AbstractEditingDomainPart;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IObject;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.util.Pair;

public class ObjectPropertyPart extends AbstractEditingDomainPart {
	private IObject resource;
	private IProperty property;
	private TableViewer tableViewer;
	private IAction createAction, addAction, removeAction;

	private ILabelProvider labelProvider;

	private int minCardinality;
	private int maxCardinality;
	private PropertyValuesContentProvider contentProvider;

	public class AddObjectAction extends Action {
		IResource object;

		AddObjectAction(IResource object) {
			super(labelProvider.getText(object));

			this.object = object;
		}

		@Override
		public void run() {
			resource.addProperty(property, object);
		}
	}

	public ObjectPropertyPart(IProperty property) {
		this.property = property;
	}

	@Override
	public void initialize(IEditorForm form) {
		super.initialize(form);

		this.labelProvider = new LabelProvider() {
			private ILabelProvider adapterFactoryLabelProvider = new AdapterFactoryLabelProvider(
					getAdapterFactory());

			@Override
			public String getText(Object element) {
				if (element == null) {
					return null;
				}
				if (element instanceof IStatement) {
					element = ((IStatement) element).getObject();
				}
				return adapterFactoryLabelProvider.getText(element);
			}

			@Override
			public Image getImage(Object element) {
				if (element == null) {
					return null;
				}
				if (element instanceof IStatement) {
					element = ((IStatement) element).getObject();
				}
				return adapterFactoryLabelProvider.getImage(element);
			}

		};

		this.contentProvider = new PropertyValuesContentProvider(property);
	}

	public void createContents(Composite parent) {
		parent.setLayout(new GridLayout(2, false));

		Label propertyLabel = getWidgetFactory().createLabel(parent,
				labelProvider.getText(property));

		GridData gridData = new GridData(SWT.FILL, SWT.BOTTOM, true, true);
		propertyLabel.setLayoutData(gridData);
		ToolBar toolBar = new ToolBar(parent, SWT.HORIZONTAL | SWT.FLAT);
		gridData = new GridData(SWT.TRAIL, SWT.BOTTOM, true, true);
		toolBar.setLayoutData(gridData);
		getWidgetFactory().adapt(toolBar, false, false);
		ToolBarManager toolBarMgr = new ToolBarManager(toolBar);
		createButtons(toolBarMgr);

		Table table = getWidgetFactory().createTable(parent,
				SWT.FULL_SELECTION | SWT.MULTI | SWT.V_SCROLL);

		int height = table.getItemHeight();

		if (maxCardinality > 1) {
			height = height * 6;
		}

		

		Rectangle trim = table.computeTrim(0, 0, 0, height);

		gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.horizontalSpan = 2;
		gridData.heightHint = trim.height;

		table.setLayoutData(gridData);

		tableViewer = new TableViewer(table);
		tableViewer.setContentProvider(contentProvider);
		tableViewer.setLabelProvider(labelProvider);
		tableViewer
				.addSelectionChangedListener(new ISelectionChangedListener() {
					public void selectionChanged(SelectionChangedEvent event) {
						removeAction
								.setEnabled(!(event.getSelection().isEmpty() || ((IStatement) ((IStructuredSelection) event
										.getSelection()).getFirstElement())
										.isInferred()));
					}
				});

		tableViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
			}
		});
	}

	protected Collection<IClass> getPropertyRange(Property property) {
		Set<net.enilink.vocab.rdfs.Class> ranges = property
				.getRdfsRanges();
		List<IClass> rangeClasses = new ArrayList<IClass>();

		if (ranges != null) {
			for (net.enilink.vocab.rdfs.Class range : ranges) {
				rangeClasses.add((IClass) range);
			}
		}
		return rangeClasses;
	}

	private Collection<IResource> getObjects(Collection<IClass> classes) {
		Set<IResource> individuals = new LinkedHashSet<IResource>();

		for (IClass clazz : classes) {
			for (Iterator<IResource> it = clazz.getInstances(true).iterator(); it
					.hasNext();) {
				IResource resource = it.next();
				if (resource.isOntLanguageTerm()) {
					continue;
				}
				individuals.add(resource);
			}
		}

		for (Object value : resource.getPropertyValues(property, true)) {
			individuals.remove(value);
		}

		return individuals;
	}

	private void createButtons(ToolBarManager toolBarMgr) {
		createAction = new CreateObjectAction(getShell()) {
			// @Override
			// protected void finishAction(IResource newResource) {
			// resource.addProperty(property, newResource);
			// }

			@Override
			protected IModel getModel() {
				return resource.getModel();
			}

			@Override
			protected IAdapterFactory getAdapterFactory() {
				return ObjectPropertyPart.this.getAdapterFactory();
			}
		};
		toolBarMgr.add(createAction);

		addAction = new Action("Add...") {
			@Override
			public void run() {
				FilteredTreeAndListSelectionDialog dialog = new FilteredTreeAndListSelectionDialog(
						getShell()) {
					IStructuredSelection treeSelection = StructuredSelection.EMPTY;

					@Override
					public void fillListContentProvider(
							AbstractContentProvider contentProvider,
							ItemsFilter itemsFilter,
							IProgressMonitor progressMonitor)
							throws CoreException {
						Collection<IClass> typeClasses;
						if (treeSelection.isEmpty()) {
							typeClasses = getPropertyRange(property);
						} else {
							typeClasses = new ArrayList<IClass>();
							for (Object selectedType : treeSelection.toArray()) {
								if (!(selectedType instanceof IClass)) {
									continue;
								}
								typeClasses.add((IClass) selectedType);
							}
						}

						final Collection<IResource> individuals = getObjects(typeClasses);
						for (IResource individual : individuals) {
							contentProvider.add(individual, itemsFilter);
						}
					}

					@Override
					protected IDialogSettings getDialogSettings() {
						return KommaEditUIPlugin.getPlugin().getDialogSettings();
					}

					@Override
					public String getListItemName(Object item) {
						return labelProvider.getText(item);
					}

					@Override
					public Comparator<Object> getListItemsComparator() {
						return new Comparator<Object>() {
							@Override
							public int compare(Object a, Object b) {
								String labelA = labelProvider.getText(a);
								String labelB = labelProvider.getText(b);
								if (labelA == null) {
									if (labelB == null) {
										return 0;
									}
									return -1;
								} else if (labelB == null) {
									return 1;
								}
								return labelA.compareTo(labelB);
							}
						};
					}

					@Override
					public IStatus validateListItem(Object item) {
						return Status.OK_STATUS;
					}

					@Override
					public Object getTreeInput() {
						return getPropertyRange(property);
					}

					@Override
					public void create() {
						super.create();

						getFilteredTree().getViewer()
								.addSelectionChangedListener(
										new ISelectionChangedListener() {
											@Override
											public void selectionChanged(
													SelectionChangedEvent event) {
												treeSelection = (IStructuredSelection) event
														.getSelection();
												getFilteredList()
														.clearAndRefresh();
											}
										});
					}

					@Override
							public ILabelProvider getLabelProvider() {
								// TODO Auto-generated method stub
								return null;
							}
					@Override
							public ITreeContentProvider getTreeContentProvider() {
								// TODO Auto-generated method stub
								return null;
							}
					
				};
				dialog.getFilteredList().setListLabelProvider(labelProvider);// .createStyledCellLabelProvider());
				dialog.getFilteredTree().setTreeLabelProvider(labelProvider);// .createStyledCellLabelProvider());
				dialog.getFilteredTree().setTreeContentProvider(
						new ClassHierarchyContentProvider(false));

				// dialog.setInitialSelections(individuals.toArray());
				// dialog.setElements(individuals.toArray());
				// dialog.setMultipleSelection(true);
				dialog.setTitle("Select Individuals");

				if (dialog.open() == Window.OK) {
					Object[] selectedResources = dialog.getResult();

					if (selectedResources != null) {
						for (Object selectedResource : selectedResources) {
							resource.addProperty(property,
									(Resource) selectedResource);
						}
					}
				}
			}
		};

		addAction
				.setImageDescriptor(ExtendedImageRegistry
						.getInstance()
						.getImageDescriptor(
								KommaEditUIPropertiesPlugin.INSTANCE
										.getImage(IEditUIPropertiesImages.INDIVIDUAL_ADD)));
		addAction.setMenuCreator(new IMenuCreator() {
			Menu menu;

			public void dispose() {
				if (menu != null) {
					menu.dispose();
					menu = null;
				}
			}

			public Menu getMenu(Control parent) {
				if (menu != null) {
					menu.dispose();
				}
				menu = new Menu(parent);
				createEntries(menu);

				return menu;
			}

			public Menu getMenu(Menu parent) {
				return menu;
			}

			private void createEntries(Menu menu) {
				Collection<IResource> objects = getObjects(getPropertyRange(property));
				IResource[] objectsArray = objects
						.toArray(new IResource[objects.size()]);
				tableViewer.getComparator().sort(tableViewer, objectsArray);

				for (IResource object : objectsArray) {
					addActionToMenu(menu, new AddObjectAction(object));
				}
			}

			/**
			 * Populate the menu with the given action item
			 * 
			 * @param parent
			 *            the menu to add an action for
			 * @param action
			 *            the action to be added
			 */
			private void addActionToMenu(Menu parent, IAction action) {
				ActionContributionItem item = new ActionContributionItem(action);
				item.fill(parent, parent.getItemCount());
			}
		});
		toolBarMgr.add(addAction);

		removeAction = new Action(
				"Remove",
				ExtendedImageRegistry
						.getInstance()
						.getImageDescriptor(
								KommaEditUIPropertiesPlugin.INSTANCE
										.getImage(IEditUIPropertiesImages.INDIVIDUAL_REMOVE))) {
			@Override
			public void run() {
				IStructuredSelection selection = (IStructuredSelection) tableViewer
						.getSelection();
				for (Object selected : selection.toArray()) {
					resource
							.removeProperty(
									property,
									(selected instanceof IStatement) ? ((IStatement) selected)
											.getObject()
											: selected);
				}
			}
		};
		removeAction.setEnabled(false);
		toolBarMgr.add(removeAction);
		toolBarMgr.update(false);
	}

	protected void updateControls() {
		int cardinality = resource.getCardinality(property);
		addAction.setEnabled(cardinality < maxCardinality);

		if (cardinality > maxCardinality) {
			getForm().getMessageManager().addMessage(
					"card:" + property.getURI(),
					"Maximum cardinality of " + maxCardinality + " exceeded",
					null, IMessageProvider.WARNING, tableViewer.getControl());
		} else if (cardinality < minCardinality) {
			getForm().getMessageManager().addMessage(
					"card:" + property.getURI(),
					"Minimum cardinality of " + minCardinality
							+ " not achieved", null, IMessageProvider.WARNING,
					tableViewer.getControl());
		} else {
			getForm().getMessageManager().removeMessage(
					"card:" + property.getURI(), tableViewer.getControl());
		}
	}

	@Override
	public void activate() {
		contentProvider.registerListener();
		super.activate();
	}

	@Override
	public void deactivate() {
		contentProvider.unregisterListener();
		getForm().getMessageManager().removeMessages(tableViewer.getControl());

		super.deactivate();
	}

	public void commit(boolean onSave) {
		setDirty(false);
	}

	@Override
	public void refresh() {
		Pair<Integer, Integer> cardinality = resource.getApplicableCardinality(
				property);
		minCardinality = cardinality.getFirst();
		maxCardinality = cardinality.getSecond();

		tableViewer.setInput(resource);
		updateControls();
		super.refresh();
	}

	public void setInput(Object input) {
		resource = (IObject) input;
	}
}
