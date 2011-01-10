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

import java.util.Collection;
import java.util.Iterator;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICellEditorValidator;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.ToolBar;

import net.enilink.commons.iterator.IMap;
import net.enilink.commons.iterator.WrappedIterator;
import net.enilink.commons.ui.jface.table.CComboBoxCellEditor;
import net.enilink.vocab.owl.DatatypeProperty;
import net.enilink.vocab.owl.Restriction;
import net.enilink.komma.concepts.IClass;
import net.enilink.komma.edit.ui.properties.IEditUIPropertiesImages;
import net.enilink.komma.edit.ui.properties.KommaEditUIPropertiesPlugin;
import net.enilink.komma.edit.ui.provider.AdapterFactoryLabelProvider;
import net.enilink.komma.edit.ui.provider.ExtendedImageRegistry;
import net.enilink.komma.edit.ui.provider.reflective.ObjectComparator;
import net.enilink.komma.edit.ui.provider.reflective.PropertyValuesContentProvider;
import net.enilink.komma.edit.ui.views.AbstractEditingDomainPart;
import net.enilink.komma.model.IObject;
import net.enilink.komma.core.ILiteral;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.URI;
import net.enilink.komma.util.KommaUtil;

public class DatatypePropertyPart extends AbstractEditingDomainPart {
	private final String VALUE = "value", TYPE = "type", LANG = "lang";

	private IObject resource;
	private Collection<IClass> classes;
	private DatatypeProperty dataProperty;
	private TableViewer tableViewer;
	private ILabelProvider resourceLabelProvider = new DataPropertiesLabelProvider();
	private PropertyValuesContentProvider contentProvider;
	private IAction createAction, removeAction;
	private int minCardinality;
	private int maxCardinality;

	private class DataPropertiesLabelProvider extends LabelProvider implements
			ITableLabelProvider {
		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (element instanceof IStatement) {
				element = ((IStatement) element).getObject();
			}
			switch (columnIndex) {
			case 0:
				return super.getText(element);
			case 1:
			case 2: {
				if (!(element instanceof ILiteral)) {
					element = resource.getModel().getManager().toValue(element);
				}

				if (element instanceof ILiteral) {
					ILiteral literal = (ILiteral) element;

					if (columnIndex == 1) {
						// TYPE
						URI type = literal.getDatatype();
						if (type != null) {
							String localName = type.localPart();
							if (localName != null) {
								return localName;
							}
							return type.toString();
						}
					} else {
						// LANG
						return literal.getLanguage();
					}
				}
			}
			}
			return null;
		}
	}

	public DatatypePropertyPart(Collection<IClass> classes,
			DatatypeProperty dataProperty) {
		this.classes = classes;
		this.dataProperty = dataProperty;
		// TODO
		this.contentProvider = new PropertyValuesContentProvider(dataProperty) {
			@Override
			protected void updatedViewer() {
				updateControls();
			}
		};
		this.contentProvider.setUseRawObjectsInStatements(true);
	}

	public void createContents(Composite parent) {
		parent.setLayout(new GridLayout(2, false));
		Label propertyLabel = getWidgetFactory().createLabel(parent,
				resourceLabelProvider.getText(dataProperty));
		GridData gridData = new GridData(SWT.FILL, SWT.BOTTOM, true, true);
		propertyLabel.setLayoutData(gridData);
		ToolBar toolBar = new ToolBar(parent, SWT.HORIZONTAL | SWT.FLAT);
		gridData = new GridData(SWT.TRAIL, SWT.BOTTOM, true, true);
		toolBar.setLayoutData(gridData);
		getWidgetFactory().adapt(toolBar, false, false);
		ToolBarManager toolBarMgr = new ToolBarManager(toolBar);
		createButtons(toolBarMgr);

		minCardinality = 0;
		maxCardinality = Integer.MAX_VALUE;
		for (IClass parentClass : classes) {
			if (parentClass instanceof Restriction) {
				Restriction restriction = (Restriction) parentClass;
				if (dataProperty.equals(restriction.getOwlOnProperty())) {
					if (restriction.getOwlMaxCardinality() != null) {
						maxCardinality = Math.min(restriction
								.getOwlMaxCardinality().intValue(),
								maxCardinality);

						if (maxCardinality < minCardinality) {
							maxCardinality = minCardinality;
						}
					} else if (restriction.getOwlMinCardinality() != null) {
						minCardinality = Math.max(restriction
								.getOwlMinCardinality().intValue(),
								minCardinality);

						if (minCardinality > maxCardinality) {
							minCardinality = maxCardinality;
						}
					} else if (restriction.getOwlCardinality() != null) {
						minCardinality = maxCardinality = restriction
								.getOwlCardinality().intValue();
					}
				}
			}
		}

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
		table.setHeaderVisible(true);

		TableColumn column = new TableColumn(table, SWT.NONE);
		column.setText("Value");
		column.pack();
		column = new TableColumn(table, SWT.NONE);
		column.setText("Type");
		column.pack();
		column = new TableColumn(table, SWT.NONE);
		column.setText("Language");
		column.pack();

		tableViewer = new TableViewer(table);
		tableViewer.setComparator(new ObjectComparator());
		tableViewer.setContentProvider(contentProvider);
		tableViewer.setLabelProvider(resourceLabelProvider);
		tableViewer.setColumnProperties(new String[] { VALUE, TYPE, LANG });

		tableViewer.setCellModifier(new ICellModifier() {
			@Override
			public boolean canModify(Object element, String property) {
				return true;
			}

			@Override
			public Object getValue(Object element, String property) {
				if (element instanceof Item) {
					element = ((Item) element).getData();
				}
				if (element instanceof IStatement) {
					element = ((IStatement) element).getObject();
				}
				if (!(element instanceof ILiteral)) {
					element = resource.getModel().getManager().toValue(element);
				}
				if (element instanceof ILiteral) {
					ILiteral literal = (ILiteral) element;

					if (VALUE.equals(property)) {
						return literal.getLabel();
					} else if (TYPE.equals(property)) {
						return literal.getDatatype();
					} else if (LANG.equals(property)) {
						return literal.getLanguage();
					}
				}
				return null;
			}

			@Override
			public void modify(Object element, String property, Object value) {
				if (value == null) {
					return;
				}
				if (element instanceof Item) {
					element = ((Item) element).getData();
				}
				if (element instanceof IStatement) {
					element = ((IStatement) element).getObject();
				}
				if (VALUE.equals(property)) {
					URI typeUri = null;
					if (element instanceof ILiteral) {
						typeUri = ((ILiteral) element).getDatatype();
					}
					if (typeUri == null) {
						Iterator<net.enilink.vocab.rdfs.Class> rangeIt = dataProperty
								.getRdfsRanges().iterator();
						if (rangeIt.hasNext()) {
							typeUri = rangeIt.next().getURI();
						}
					}

					if (typeUri != null) {
						value = convertToType(value, typeUri);
					}

					resource.getEntityManager().getTransaction().begin();
					resource.removeProperty(dataProperty, element);
					resource.addProperty(dataProperty, value);
					resource.getEntityManager().getTransaction().commit();
				} else if (TYPE.equals(property)) {
					resource.getEntityManager().getTransaction().begin();
					resource.removeProperty(dataProperty, element);
					resource.addProperty(dataProperty,
							convertToType(element, (URI) value));
					resource.getEntityManager().getTransaction().commit();
				} else if (LANG.equals(property)) {
					// resource.removeProperty(dataProperty, element);
					// resource.addProperty(dataProperty, convertToType(element,
					// (URI) value));
				}
			}
		});

		tableViewer
				.addSelectionChangedListener(new ISelectionChangedListener() {
					public void selectionChanged(SelectionChangedEvent event) {
						removeAction
								.setEnabled(!event.getSelection().isEmpty());
					}
				});

		tableViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
			}
		});

		CellEditor[] editors = new CellEditor[3];

		editors[0] = createValueEditor(tableViewer);
		editors[1] = createTypeEditor(tableViewer);
		editors[2] = createLanguageEditor(tableViewer);

		tableViewer.setCellEditors(editors);
	}

	private CellEditor createValueEditor(TableViewer tableViewer) {
		TextCellEditor textEditor = new TextCellEditor(tableViewer.getTable());
		textEditor.setValidator(new ICellEditorValidator() {
			@Override
			public String isValid(Object value) {
				try {
					// resource.getOntModel().createTypedLiteral(
					// value.toString(),
					// dataProperty.asDatatypeProperty().getRange()
					// .getURI()).getValue();
				} catch (Exception e) {
					return e.getMessage();
				}
				return null;
			}
		});
		return textEditor;
	}

	private CellEditor createTypeEditor(TableViewer tableViewer) {
		CComboBoxCellEditor typeEditor = new CComboBoxCellEditor();
		typeEditor.setStyle(SWT.READ_ONLY);
		typeEditor.setLabelProvider(new AdapterFactoryLabelProvider(
				getAdapterFactory()));
		typeEditor.setContentProvider(new IStructuredContentProvider() {
			@Override
			public Object[] getElements(Object inputElement) {
				if (inputElement instanceof DatatypeProperty) {
					Iterator<net.enilink.vocab.rdfs.Class> rangeIt = dataProperty
							.getRdfsRanges().iterator();
					return WrappedIterator
							.create(rangeIt)
							.mapWith(
									new IMap<net.enilink.vocab.rdfs.Class, URI>() {
										@Override
										public URI map(
												net.enilink.vocab.rdfs.Class value) {
											return value.getURI();
										}
									}).toList().toArray();
				}
				return new Object[0];
			}

			@Override
			public void dispose() {

			}

			@Override
			public void inputChanged(Viewer viewer, Object oldInput,
					Object newInput) {
			}
		});
		typeEditor.create(tableViewer.getTable());
		typeEditor.setInput(dataProperty);
		return typeEditor;
	}

	private CellEditor createLanguageEditor(TableViewer tableViewer) {
		CComboBoxCellEditor langEditor = new CComboBoxCellEditor();
		langEditor.setLabelProvider(new LabelProvider());
		langEditor.setContentProvider(new IStructuredContentProvider() {
			@Override
			public Object[] getElements(Object inputElement) {
				if (inputElement instanceof Object[]) {
					return (Object[]) inputElement;
				}
				return new Object[0];
			}

			@Override
			public void dispose() {

			}

			@Override
			public void inputChanged(Viewer viewer, Object oldInput,
					Object newInput) {
			}
		});
		langEditor.create(tableViewer.getTable());
		langEditor.setInput(KommaUtil.getDefaultLanguages());
		return langEditor;
	}

	private Object convertToType(Object value, URI typeUri) {
		if (value instanceof ILiteral) {
			value = ((ILiteral) value).getLabel();
		}
		return resource.getModel().getManager()
				.createLiteral(value.toString(), typeUri, null);
	}

	private Object convertToPropertyRange(Object value) {
		URI rangeUri = null;
		Iterator<net.enilink.vocab.rdfs.Class> rangeIt = dataProperty
				.getRdfsRanges().iterator();
		if (rangeIt.hasNext()) {
			rangeUri = rangeIt.next().getURI();
		}

		if (rangeUri != null) {
			return convertToType(value, rangeUri);
		}
		return value;
	}

	private void createButtons(ToolBarManager toolBarMgr) {
		createAction = new Action(
				"Create",
				ExtendedImageRegistry
						.getInstance()
						.getImageDescriptor(
								KommaEditUIPropertiesPlugin.INSTANCE
										.getImage(IEditUIPropertiesImages.INDIVIDUAL_CREATE))) {
			@Override
			public void run() {
				resource.addProperty(dataProperty, convertToPropertyRange("0"));
			}
		};

		toolBarMgr.add(createAction);

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
					resource.removeProperty(
							dataProperty,
							(selected instanceof IStatement) ? ((IStatement) selected)
									.getObject() : selected);
				}
			}
		};
		removeAction.setEnabled(false);
		toolBarMgr.add(removeAction);
		toolBarMgr.update(false);
	}

	protected void updateControls() {
		int cardinality = resource.getCardinality(dataProperty);
		createAction.setEnabled(cardinality < maxCardinality);

		if (cardinality > maxCardinality) {
			getForm().getMessageManager().addMessage(
					"card:" + dataProperty.getURI(),
					"Maximum cardinality of " + maxCardinality + " exceeded",
					null, IMessageProvider.WARNING, tableViewer.getControl());
		} else if (cardinality < minCardinality) {
			getForm().getMessageManager().addMessage(
					"card:" + dataProperty.getURI(),
					"Minimum cardinality of " + minCardinality
							+ " not achieved", null, IMessageProvider.WARNING,
					tableViewer.getControl());
		} else {
			getForm().getMessageManager().removeMessage(
					"card:" + dataProperty.getURI(), tableViewer.getControl());
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
		tableViewer.setInput(resource);
		updateControls();
		super.refresh();
	}

	public void setInput(Object input) {
		resource = (IObject) input;
	}
}