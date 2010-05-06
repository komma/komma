/**
 * <copyright>
 *
 * Copyright (c) 2002, 2009 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   IBM - Initial API and implementation
 *
 * </copyright>
 *
 * $Id: FeatureEditorDialog.java,v 1.12 2009/02/09 12:59:29 emerks Exp $
 */
package net.enilink.komma.edit.ui.celleditor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PatternFilter;

import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.common.ui.celleditor.ExtendedComboBoxCellEditor;
import net.enilink.komma.concepts.IClass;
import net.enilink.komma.concepts.IProperty;
import net.enilink.komma.edit.provider.ItemProvider;
import net.enilink.komma.edit.ui.KommaEditUIPlugin;
import net.enilink.komma.edit.ui.provider.AdapterFactoryContentProvider;
import net.enilink.komma.model.IObject;
import net.enilink.komma.util.KommaUtil;

public class PropertyEditorDialog extends Dialog {
	protected IAdapterFactory adapterFactory;
	protected ILabelProvider labelProvider;
	protected IContentProvider contentProvider;
	protected IObject object;
	protected Collection<? extends IClass> classes;
	protected String displayName;
	protected ItemProvider values;
	protected List<?> choiceOfValues;
	protected List<?> result;
	protected boolean multiLine;

	public PropertyEditorDialog(Shell parent, IAdapterFactory adapterFactory,
			ILabelProvider labelProvider, IObject object,
			Collection<? extends IClass> classes, Collection<?> currentValues,
			String displayName, List<?> choiceOfValues, boolean multiLine,
			boolean sortChoices) {
		super(parent);
		setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);
		this.adapterFactory = adapterFactory;
		this.labelProvider = labelProvider;
		this.object = object;
		this.classes = classes;
		this.displayName = displayName;
		this.choiceOfValues = choiceOfValues;
		this.multiLine = multiLine;

		values = new ItemProvider(adapterFactory, currentValues);
		contentProvider = new AdapterFactoryContentProvider(adapterFactory);
		if (sortChoices && choiceOfValues != null) {
			this.choiceOfValues = new ArrayList<Object>(choiceOfValues);
			ExtendedComboBoxCellEditor.createItems(this.choiceOfValues,
					labelProvider, true);
		}
	}

	public PropertyEditorDialog(Shell parent, IAdapterFactory adapterFactory,
			ILabelProvider labelProvider, IObject object,
			Collection<? extends IClass> classes, Collection<?> currentValues,
			String displayName, List<?> choiceOfValues) {
		this(parent, adapterFactory, labelProvider, object, classes,
				currentValues, displayName, choiceOfValues, false, false);
	}

	public PropertyEditorDialog(Shell parent, IAdapterFactory adapterFactory,
			ILabelProvider labelProvider, IObject object, IProperty property,
			String displayName, List<?> choiceOfValues) {
		this(parent, adapterFactory, labelProvider, object, property
				.getNamedRanges(object, true).toSet(), (Collection<?>) object
				.get(property), displayName, choiceOfValues);
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText(KommaEditUIPlugin.INSTANCE.getString(
				"_UI_FeatureEditorDialog_title", new Object[] { displayName,
						labelProvider.getText(object) }));
		shell.setImage(labelProvider.getImage(object));
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite contents = (Composite) super.createDialogArea(parent);

		GridLayout contentsGridLayout = (GridLayout) contents.getLayout();
		contentsGridLayout.numColumns = 3;

		GridData contentsGridData = (GridData) contents.getLayoutData();
		contentsGridData.horizontalAlignment = SWT.FILL;
		contentsGridData.verticalAlignment = SWT.FILL;

		Text patternText = null;

		if (choiceOfValues != null) {
			Group filterGroupComposite = new Group(contents, SWT.NONE);
			filterGroupComposite.setText(KommaEditUIPlugin.INSTANCE
					.getString("_UI_Choices_pattern_group"));
			filterGroupComposite.setLayout(new GridLayout(2, false));
			filterGroupComposite.setLayoutData(new GridData(SWT.FILL,
					SWT.DEFAULT, true, false, 3, 1));

			Label label = new Label(filterGroupComposite, SWT.NONE);
			label.setText(KommaEditUIPlugin.INSTANCE
					.getString("_UI_Choices_pattern_label"));

			patternText = new Text(filterGroupComposite, SWT.BORDER);
			patternText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		}

		Composite choiceComposite = new Composite(contents, SWT.NONE);
		{
			GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
			data.horizontalAlignment = SWT.END;
			choiceComposite.setLayoutData(data);

			GridLayout layout = new GridLayout();
			data.horizontalAlignment = SWT.FILL;
			layout.marginHeight = 0;
			layout.marginWidth = 0;
			layout.numColumns = 1;
			choiceComposite.setLayout(layout);
		}

		Label choiceLabel = new Label(choiceComposite, SWT.NONE);
		choiceLabel.setText(choiceOfValues == null ? KommaEditUIPlugin.INSTANCE
				.getString("_UI_Value_label") : KommaEditUIPlugin.INSTANCE
				.getString("_UI_Choices_label"));
		GridData choiceLabelGridData = new GridData();
		choiceLabelGridData.verticalAlignment = SWT.FILL;
		choiceLabelGridData.horizontalAlignment = SWT.FILL;
		choiceLabel.setLayoutData(choiceLabelGridData);

		final Table choiceTable = choiceOfValues == null ? null : new Table(
				choiceComposite, SWT.MULTI | SWT.BORDER);
		if (choiceTable != null) {
			GridData choiceTableGridData = new GridData();
			choiceTableGridData.widthHint = Display.getCurrent().getBounds().width / 5;
			choiceTableGridData.heightHint = Display.getCurrent().getBounds().height / 3;
			choiceTableGridData.verticalAlignment = SWT.FILL;
			choiceTableGridData.horizontalAlignment = SWT.FILL;
			choiceTableGridData.grabExcessHorizontalSpace = true;
			choiceTableGridData.grabExcessVerticalSpace = true;
			choiceTable.setLayoutData(choiceTableGridData);
		}

		final TableViewer choiceTableViewer = choiceOfValues == null ? null
				: new TableViewer(choiceTable);
		if (choiceTableViewer != null) {
			choiceTableViewer
					.setContentProvider(new AdapterFactoryContentProvider(
							adapterFactory));
			choiceTableViewer.setLabelProvider(labelProvider);
			final PatternFilter filter = new PatternFilter() {
				@Override
				protected boolean isParentMatch(Viewer viewer, Object element) {
					return viewer instanceof AbstractTreeViewer
							&& super.isParentMatch(viewer, element);
				}
			};
			choiceTableViewer.addFilter(filter);
			assert patternText != null;
			patternText.addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					filter.setPattern(((Text) e.widget).getText());
					choiceTableViewer.refresh();
				}
			});
			choiceTableViewer.setInput(new ItemProvider(choiceOfValues));
		}

		// We use multi even for a single line because we want to respond to the
		// enter key.
		int style = multiLine ? SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL
				| SWT.BORDER : SWT.MULTI | SWT.BORDER;
		final Text choiceText = choiceOfValues == null ? new Text(
				choiceComposite, style) : null;
		if (choiceText != null) {
			GridData choiceTextGridData = new GridData();
			choiceTextGridData.widthHint = Display.getCurrent().getBounds().width / 5;
			choiceTextGridData.verticalAlignment = SWT.BEGINNING;
			choiceTextGridData.horizontalAlignment = SWT.FILL;
			choiceTextGridData.grabExcessHorizontalSpace = true;
			if (multiLine) {
				choiceTextGridData.verticalAlignment = SWT.FILL;
				choiceTextGridData.grabExcessVerticalSpace = true;
			}
			choiceText.setLayoutData(choiceTextGridData);
		}

		Composite controlButtons = new Composite(contents, SWT.NONE);
		GridData controlButtonsGridData = new GridData();
		controlButtonsGridData.verticalAlignment = SWT.FILL;
		controlButtonsGridData.horizontalAlignment = SWT.FILL;
		controlButtons.setLayoutData(controlButtonsGridData);

		GridLayout controlsButtonGridLayout = new GridLayout();
		controlButtons.setLayout(controlsButtonGridLayout);

		new Label(controlButtons, SWT.NONE);

		final Button addButton = new Button(controlButtons, SWT.PUSH);
		addButton
				.setText(KommaEditUIPlugin.INSTANCE.getString("_UI_Add_label"));
		GridData addButtonGridData = new GridData();
		addButtonGridData.verticalAlignment = SWT.FILL;
		addButtonGridData.horizontalAlignment = SWT.FILL;
		addButton.setLayoutData(addButtonGridData);

		final Button removeButton = new Button(controlButtons, SWT.PUSH);
		removeButton.setText(KommaEditUIPlugin.INSTANCE
				.getString("_UI_Remove_label"));
		GridData removeButtonGridData = new GridData();
		removeButtonGridData.verticalAlignment = SWT.FILL;
		removeButtonGridData.horizontalAlignment = SWT.FILL;
		removeButton.setLayoutData(removeButtonGridData);

		Label spaceLabel = new Label(controlButtons, SWT.NONE);
		GridData spaceLabelGridData = new GridData();
		spaceLabelGridData.verticalSpan = 2;
		spaceLabel.setLayoutData(spaceLabelGridData);

		final Button upButton = new Button(controlButtons, SWT.PUSH);
		upButton.setText(KommaEditUIPlugin.INSTANCE.getString("_UI_Up_label"));
		GridData upButtonGridData = new GridData();
		upButtonGridData.verticalAlignment = SWT.FILL;
		upButtonGridData.horizontalAlignment = SWT.FILL;
		upButton.setLayoutData(upButtonGridData);

		final Button downButton = new Button(controlButtons, SWT.PUSH);
		downButton.setText(KommaEditUIPlugin.INSTANCE
				.getString("_UI_Down_label"));
		GridData downButtonGridData = new GridData();
		downButtonGridData.verticalAlignment = SWT.FILL;
		downButtonGridData.horizontalAlignment = SWT.FILL;
		downButton.setLayoutData(downButtonGridData);

		Composite propertyComposite = new Composite(contents, SWT.NONE);
		{
			GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
			data.horizontalAlignment = SWT.END;
			propertyComposite.setLayoutData(data);

			GridLayout layout = new GridLayout();
			data.horizontalAlignment = SWT.FILL;
			layout.marginHeight = 0;
			layout.marginWidth = 0;
			layout.numColumns = 1;
			propertyComposite.setLayout(layout);
		}

		Label propertyLabel = new Label(propertyComposite, SWT.NONE);
		propertyLabel.setText(KommaEditUIPlugin.INSTANCE
				.getString("_UI_Feature_label"));
		GridData propertyLabelGridData = new GridData();
		propertyLabelGridData.horizontalSpan = 2;
		propertyLabelGridData.horizontalAlignment = SWT.FILL;
		propertyLabelGridData.verticalAlignment = SWT.FILL;
		propertyLabel.setLayoutData(propertyLabelGridData);

		final Table propertyTable = new Table(propertyComposite, SWT.MULTI
				| SWT.BORDER);
		GridData propertyTableGridData = new GridData();
		propertyTableGridData.widthHint = Display.getCurrent().getBounds().width / 5;
		propertyTableGridData.heightHint = Display.getCurrent().getBounds().height / 3;
		propertyTableGridData.verticalAlignment = SWT.FILL;
		propertyTableGridData.horizontalAlignment = SWT.FILL;
		propertyTableGridData.grabExcessHorizontalSpace = true;
		propertyTableGridData.grabExcessVerticalSpace = true;
		propertyTable.setLayoutData(propertyTableGridData);

		final TableViewer propertyTableViewer = new TableViewer(propertyTable);
		propertyTableViewer.setContentProvider(contentProvider);
		propertyTableViewer.setLabelProvider(labelProvider);
		propertyTableViewer.setInput(values);
		if (!values.getChildren().isEmpty()) {
			propertyTableViewer.setSelection(new StructuredSelection(values
					.getChildren().get(0)));
		}

		if (choiceTableViewer != null) {
			choiceTableViewer
					.addDoubleClickListener(new IDoubleClickListener() {
						public void doubleClick(DoubleClickEvent event) {
							if (addButton.isEnabled()) {
								addButton.notifyListeners(SWT.Selection, null);
							}
						}
					});

			propertyTableViewer
					.addDoubleClickListener(new IDoubleClickListener() {
						public void doubleClick(DoubleClickEvent event) {
							if (removeButton.isEnabled()) {
								removeButton.notifyListeners(SWT.Selection,
										null);
							}
						}
					});
		}

		if (choiceText != null) {
			choiceText.addKeyListener(new KeyAdapter() {
				@Override
				public void keyPressed(KeyEvent event) {
					if (!multiLine
							&& (event.character == '\r' || event.character == '\n')) {
						try {
							Object value = KommaUtil.convertToRange(object
									.getKommaManager(), classes, choiceText
									.getText());
							values.getChildren().add(value);
							choiceText.setText("");
							propertyTableViewer
									.setSelection(new StructuredSelection(value));
							event.doit = false;
						} catch (RuntimeException exception) {
							// Ignore
						}
					} else if (event.character == '\33') {
						choiceText.setText("");
						event.doit = false;
					}
				}
			});
		}

		upButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				IStructuredSelection selection = (IStructuredSelection) propertyTableViewer
						.getSelection();
				int minIndex = 0;
				for (Iterator<?> i = selection.iterator(); i.hasNext();) {
					Object value = i.next();
					int index = values.getChildren().indexOf(value);
					values.getChildren().move(Math.max(index - 1, minIndex++),
							value);
				}
			}
		});

		downButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				IStructuredSelection selection = (IStructuredSelection) propertyTableViewer
						.getSelection();
				int maxIndex = values.getChildren().size() - selection.size();
				for (Iterator<?> i = selection.iterator(); i.hasNext();) {
					Object value = i.next();
					int index = values.getChildren().indexOf(value);
					values.getChildren().move(Math.min(index + 1, maxIndex++),
							value);
				}
			}
		});

		addButton.addSelectionListener(new SelectionAdapter() {
			// event is null when choiceTableViewer is double clicked
			@Override
			public void widgetSelected(SelectionEvent event) {
				if (choiceTableViewer != null) {
					IStructuredSelection selection = (IStructuredSelection) choiceTableViewer
							.getSelection();
					for (Iterator<?> i = selection.iterator(); i.hasNext();) {
						Object value = i.next();
						if (!values.getChildren().contains(value)) {
							values.getChildren().add(value);
						}
					}
					propertyTableViewer.setSelection(selection);
				} else if (choiceText != null) {
					try {
						Object value = KommaUtil.convertToRange(object
								.getKommaManager(), classes, choiceText
								.getText());
						values.getChildren().add(value);
						choiceText.setText("");
						propertyTableViewer
								.setSelection(new StructuredSelection(value));
					} catch (RuntimeException exception) {
						// Ignore
					}
				}
			}
		});

		removeButton.addSelectionListener(new SelectionAdapter() {
			// event is null when featureTableViewer is double clicked
			@Override
			public void widgetSelected(SelectionEvent event) {
				IStructuredSelection selection = (IStructuredSelection) propertyTableViewer
						.getSelection();
				Object firstValue = null;
				for (Iterator<?> i = selection.iterator(); i.hasNext();) {
					Object value = i.next();
					if (firstValue == null) {
						firstValue = value;
					}
					values.getChildren().remove(value);
				}

				if (!values.getChildren().isEmpty()) {
					propertyTableViewer.setSelection(new StructuredSelection(
							values.getChildren().get(0)));
				}

				if (choiceTableViewer != null) {
					choiceTableViewer.setSelection(selection);
				} else if (choiceText != null) {
					if (firstValue != null) {
						String value = KommaUtil.getLabel(firstValue);
						choiceText.setText(value);
					}
				}
			}
		});

		return contents;
	}

	@Override
	protected void okPressed() {
		result = new ArrayList<Object>(values.getChildren());
		super.okPressed();
	}

	@Override
	public boolean close() {
		contentProvider.dispose();
		return super.close();
	}

	public List<?> getResult() {
		return result;
	}
}
