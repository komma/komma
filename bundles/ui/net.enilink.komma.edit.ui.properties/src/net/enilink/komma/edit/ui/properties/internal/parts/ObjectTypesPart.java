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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.ToolBar;

import net.enilink.vocab.rdf.RDF;
import net.enilink.komma.concepts.IClass;
import net.enilink.komma.concepts.IProperty;
import net.enilink.komma.edit.ui.dialogs.FilteredTreeSelectionDialog;
import net.enilink.komma.edit.ui.properties.IEditUIPropertiesImages;
import net.enilink.komma.edit.ui.properties.KommaEditUIPropertiesPlugin;
import net.enilink.komma.edit.ui.provider.AdapterFactoryLabelProvider;
import net.enilink.komma.edit.ui.provider.ExtendedImageRegistry;
import net.enilink.komma.edit.ui.provider.reflective.ClassHierarchyContentProvider;
import net.enilink.komma.edit.ui.provider.reflective.ObjectComparator;
import net.enilink.komma.edit.ui.provider.reflective.PropertyValuesContentProvider;
import net.enilink.komma.edit.ui.views.AbstractEditingDomainPart;
import net.enilink.komma.model.IObject;

public class ObjectTypesPart extends AbstractEditingDomainPart {
	private IObject resource;
	private TableViewer tableViewer;
	private IAction addAction, removeAction;

	private ILabelProvider resourceLabelProvider;;

	private PropertyValuesContentProvider contentProvider;

	public class AddTypeAction extends Action {
		IClass object;

		AddTypeAction(IClass object) {
			super(resourceLabelProvider.getText(object));

			setImageDescriptor(ExtendedImageRegistry
					.getInstance()
					.getImageDescriptor(
							KommaEditUIPropertiesPlugin.INSTANCE
									.getImage(IEditUIPropertiesImages.INDIVIDUAL)));

			this.object = object;
		}

		@Override
		public void run() {
			resource.getRdfTypes().add(object);
		}
	}

	public ObjectTypesPart() {
		this.contentProvider = new PropertyValuesContentProvider(
				RDF.PROPERTY_TYPE);
	}

	public void createContents(Composite parent) {
		resourceLabelProvider = new AdapterFactoryLabelProvider(
				getAdapterFactory());

		parent.setLayout(new GridLayout(2, false));

		Label propertyLabel = getWidgetFactory().createLabel(parent,
				"Asserted Types");

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

		int height = table.getItemHeight() * 6;

		Rectangle trim = table.computeTrim(0, 0, 0, height);

		gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.horizontalSpan = 2;
		gridData.heightHint = trim.height;
		table.setLayoutData(gridData);

		tableViewer = new TableViewer(table);
		tableViewer.setComparator(new ObjectComparator());
		tableViewer.setContentProvider(contentProvider);
		tableViewer.setLabelProvider(resourceLabelProvider);
		tableViewer
				.addSelectionChangedListener(new ISelectionChangedListener() {
					public void selectionChanged(SelectionChangedEvent event) {
						boolean removeEnabled = false;
						if (!event.getSelection().isEmpty()) {
							Object[] selectedObjs = ((IStructuredSelection) event
									.getSelection()).toArray();

							for (Object selectedObj : selectedObjs) {
								// test if selected type is in editable graph
							}
						}
						removeAction.setEnabled(removeEnabled);
					}
				});

		tableViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
			}
		});
	}

	private void createButtons(ToolBarManager toolBarMgr) {
		addAction = new Action("Add...") {
			@Override
			public void run() {
				FilteredTreeSelectionDialog dialog = new FilteredTreeSelectionDialog(
						getShell()) {
					@Override
					protected IDialogSettings getDialogSettings() {
						return KommaEditUIPropertiesPlugin.getPlugin()
								.getDialogSettings();
					}

					@Override
					protected Object getTreeInput() {
						return resource.getModel();
					}

					@Override
					public void create() {
						super.create();
					}
				};
				dialog.getFilteredTree().setTreeLabelProvider(
						new AdapterFactoryLabelProvider(getAdapterFactory()));
				dialog.getFilteredTree().setTreeContentProvider(
						new ClassHierarchyContentProvider(false));

				dialog.setTitle("Select Types");

				if (dialog.open() == Window.OK) {
					Object[] selectedResources = dialog.getResult();

					if (selectedResources != null) {
						for (Object selectedResource : selectedResources) {
							IProperty typeProperty = resource.getModel()
									.getManager().find(RDF.PROPERTY_TYPE,
											IProperty.class);
							if (!resource.hasProperty(typeProperty,
									(IClass) selectedResource, false)) {
								resource.addProperty(typeProperty,
										(IClass) selectedResource);
							}
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
				Collection<IClass> types = new ArrayList<IClass>();

				for (IClass type : types) {
					addActionToMenu(menu, new AddTypeAction(type));
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

				IProperty typeProperty = resource.getModel().getManager().find(
						RDF.PROPERTY_TYPE, IProperty.class);
				for (Object selected : selection.toArray()) {
					resource.removeProperty(typeProperty, (IClass) selected);
				}
			}
		};
		removeAction.setEnabled(false);
		toolBarMgr.add(removeAction);
		toolBarMgr.update(false);
	}

	protected void updateControls() {

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
		tableViewer.setInput(resource);
		updateControls();
		super.refresh();
	}

	public void setInput(Object input) {
		resource = (IObject) input;
	}
}
