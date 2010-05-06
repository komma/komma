/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Tom Schindl <tom.schindl@bestsolution.at> - bugfix in 174739
 *     Ken Wenzel - adaption to CCombo
 *******************************************************************************/

package net.enilink.commons.ui.jface.table;

import java.text.MessageFormat;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import net.enilink.commons.ui.jface.viewers.CComboViewer;

/**
 * A cell editor that presents a list of items in a combo box.
 */
public class CComboBoxCellEditor extends CellEditor {
	private IBaseLabelProvider labelProvider;

	private IContentProvider contentProvider;
	
	private ViewerComparator viewerComparator;

	/**
	 * The list of items to present in the combo box.
	 */
	private Object[] items;

	/**
	 * The the selected item.
	 */
	protected Object selection;

	/**
	 * The custom combo box control.
	 */
	protected CCombo comboBox;

	/**
	 * The combo box viewer
	 */
	protected CComboViewer viewer;

	private Object input;

	/**
	 * Default ComboBoxCellEditor style
	 */
	private static final int defaultStyle = SWT.NONE;

	/**
	 * Creates a new cell editor with no control and no st of choices.
	 * Initially, the cell editor has no cell validator.
	 * 
	 * @since 2.1
	 * @see CellEditor#setStyle
	 * @see CellEditor#create
	 * @see CComboBoxCellEditor#setItems
	 * @see CellEditor#dispose
	 */
	public CComboBoxCellEditor() {
		setStyle(defaultStyle);
	}

	/**
	 * Creates a new cell editor with a combo containing the given list of
	 * choices and parented under the given control. The cell editor value is
	 * the zero-based index of the selected item. Initially, the cell editor has
	 * no cell validator and the first item in the list is selected.
	 * 
	 * @param parent
	 *            the parent control
	 * @param items
	 *            the list of strings for the combo box
	 * @param labelProvider
	 *            the label provider of the comboviewer
	 * @param style
	 *            the style bits
	 * @since 2.1
	 */
	public CComboBoxCellEditor(Composite parent, Object[] items,
			IBaseLabelProvider labelProvider, int style) {
		super(parent, style);
		this.labelProvider = labelProvider;
		setItems(items);
	}

	/**
	 * Creates a new cell editor with a combo containing the given list of
	 * choices and parented under the given control. The cell editor value is
	 * the zero-based index of the selected item. Initially, the cell editor has
	 * no cell validator and the first item in the list is selected.
	 * 
	 * @param parent
	 *            the parent control
	 * @param items
	 *            the list of strings for the combo box
	 * @param style
	 *            the style bits
	 * @since 2.1
	 */
	public CComboBoxCellEditor(Composite parent, ILabelProvider labelProvider,
			IContentProvider contentProvider, int style) {
		setStyle(style);
		this.labelProvider = labelProvider;
		this.contentProvider = contentProvider;
		create(parent);
	}

	/**
	 * Returns the list of choices for the combo box
	 * 
	 * @return the list of choices for the combo box
	 */
	public Object[] getItems() {
		return this.items;
	}

	/**
	 * Sets the list of choices for the combo box
	 * 
	 * @param items
	 *            the list of choices for the combo box
	 */
	public void setItems(Object[] items) {
		Assert.isNotNull(items);
		this.items = items;
		populateComboBoxItems();
	}

	/**
	 * Sets the list of choices for the combo box
	 * 
	 * @param items
	 *            the list of choices for the combo box
	 */
	public void setInput(Object input) {
		this.input = input;
		if (viewer != null) {
			viewer.setInput(input);

			setValueValid(true);
			selection = null;
		}
	}

	protected FocusListener createFocusListener() {
		return new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				CComboBoxCellEditor.this.focusLost();
			}
		};
	}

	/*
	 * (non-Javadoc) Method declared on CellEditor.
	 */
	protected Control createControl(Composite parent) {
		comboBox = new CCombo(parent, getStyle());
		comboBox.setFont(parent.getFont());

		comboBox.addKeyListener(new KeyAdapter() {
			// hook key pressed - see PR 14201
			public void keyPressed(KeyEvent e) {
				keyReleaseOccured(e);
			}
		});

		comboBox.addSelectionListener(new SelectionAdapter() {
			public void widgetDefaultSelected(SelectionEvent event) {
				applyEditorValueAndDeactivate();
			}

			public void widgetSelected(SelectionEvent event) {
				selection = ((IStructuredSelection) viewer.getSelection())
						.getFirstElement();
			}
		});

		comboBox.addTraverseListener(new TraverseListener() {
			public void keyTraversed(TraverseEvent e) {
				if (e.detail == SWT.TRAVERSE_ESCAPE
						|| e.detail == SWT.TRAVERSE_RETURN) {
					e.doit = false;
				}
			}
		});

		FocusListener focusListener = createFocusListener();
		if (focusListener != null) {
			comboBox.addFocusListener(focusListener);
		}

		viewer = new CComboViewer(comboBox);

		if (contentProvider == null) {
			contentProvider = new IStructuredContentProvider() {
				public void dispose() {

				}

				public void inputChanged(Viewer viewer, Object oldInput,
						Object newInput) {

				}

				public Object[] getElements(Object inputElement) {
					if (inputElement instanceof Object[]) {
						return (Object[]) inputElement;
					}
					return new Object[0];
				}
			};
		}
		viewer.setContentProvider(contentProvider);
		if (labelProvider != null) {
			viewer.setLabelProvider(labelProvider);
		}
		
		if (viewerComparator != null) {
			viewer.setComparator(viewerComparator);
		}

		return comboBox;
	}

	protected void selectionChanged() {
		
	}
	
	public void setLabelProvider(IBaseLabelProvider labelProvider) {
		this.labelProvider = labelProvider;
		if (viewer != null && labelProvider != null) {
			viewer.setLabelProvider(labelProvider);
		}
	}

	public void setContentProvider(IContentProvider contentProvider) {
		this.contentProvider = contentProvider;
		if (viewer != null && contentProvider != null) {
			viewer.setContentProvider(contentProvider);
		}
	}
	
	public void setViewerComparator(ViewerComparator viewerComparator) {
		this.viewerComparator = viewerComparator;
		if (viewer != null && viewerComparator != null) {
			viewer.setComparator(viewerComparator);
		}
	}

	/**
	 * The <code>ComboBoxCellEditor</code> implementation of this
	 * <code>CellEditor</code> framework method returns the current selection.
	 * 
	 * @return the current selection
	 */
	protected Object doGetValue() {
		return selection;
	}

	/*
	 * (non-Javadoc) Method declared on CellEditor.
	 */
	protected void doSetFocus() {
		comboBox.setFocus();
	}

	/**
	 * The <code>ComboBoxCellEditor</code> implementation of this
	 * <code>CellEditor</code> framework method sets the minimum width of the
	 * cell. The minimum width is 10 characters if <code>comboBox</code> is
	 * not <code>null</code> or <code>disposed</code> eles it is 60 pixels
	 * to make sure the arrow button and some text is visible. The list of
	 * CCombo will be wide enough to show its longest item.
	 */
	public LayoutData getLayoutData() {
		LayoutData layoutData = super.getLayoutData();
		if (comboBox == null || comboBox.isDisposed()) {
			layoutData.minimumWidth = 60;
		} else {
			// make the comboBox 10 characters wide
			GC gc = new GC(comboBox);
			layoutData.minimumWidth = (gc.getFontMetrics()
					.getAverageCharWidth() * 10) + 10;
			gc.dispose();
		}
		return layoutData;
	}

	/**
	 * The <code>ComboBoxCellEditor</code> implementation of this
	 * <code>CellEditor</code> framework method accepts a zero-based index of
	 * a selection.
	 * 
	 * @param value
	 *            the zero-based index of the selection wrapped as an
	 *            <code>Integer</code>
	 */
	protected void doSetValue(Object value) {
		Assert.isTrue(viewer != null);
		if (value != null) {
			viewer.setSelection(new StructuredSelection(value), true);
		} else {
			viewer.setSelection(StructuredSelection.EMPTY);
		}
		selection = value;
	}

	/**
	 * Updates the list of choices for the combo box for the current control.
	 */
	private void populateComboBoxItems() {
		if (viewer != null && items != null) {
			viewer.setInput(items);

			setValueValid(true);
			selection = null;
		}
	}

	/**
	 * Applies the currently selected value and deactiavates the cell editor
	 */
	void applyEditorValueAndDeactivate() {
		// must set the selection before getting value
		selection = ((IStructuredSelection) viewer.getSelection())
				.getFirstElement();
		Object newValue = doGetValue();
		markDirty();
		boolean isValid = isCorrect(newValue);
		setValueValid(isValid);

		if (!isValid) {
			// Only format if the selection is valid
			if (selection != null) {
				// try to insert the current value into the error message.
				setErrorMessage(MessageFormat.format(getErrorMessage(),
						new Object[] { selection }));
			} else {
				// Since we don't have a valid selection, assume we're using an
				// 'edit'
				// combo so format using its text value
				setErrorMessage(MessageFormat.format(getErrorMessage(),
						new Object[] { comboBox.getText() }));
			}
		}

		fireApplyEditorValue();
		deactivate();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.CellEditor#focusLost()
	 */
	protected void focusLost() {
		if (isActivated()) {
			applyEditorValueAndDeactivate();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.CellEditor#keyReleaseOccured(org.eclipse.swt.events.KeyEvent)
	 */
	protected void keyReleaseOccured(KeyEvent keyEvent) {
		if (keyEvent.character == '\u001b') { // Escape character
			fireCancelEditor();
		} else if (keyEvent.character == '\t') { // tab key
			applyEditorValueAndDeactivate();
		}
	}
}
