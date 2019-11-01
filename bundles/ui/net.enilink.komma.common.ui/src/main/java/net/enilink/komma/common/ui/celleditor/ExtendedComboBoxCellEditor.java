/**
 * <copyright> 
 *
 * Copyright (c) 2002, 2010 IBM Corporation and others.
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
 * $Id: ExtendedComboBoxCellEditor.java,v 1.8 2008/12/22 14:26:02 emerks Exp $
 */
package net.enilink.komma.common.ui.celleditor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import net.enilink.komma.common.CommonPlugin;

/**
 * This uses a list of objects and a label provider to build a combo box based
 * on model objects rather than on strings. If sort is true, the list will be
 * modified to match the order of the sorted labels.
 */
public class ExtendedComboBoxCellEditor extends ComboBoxCellEditor {
	private static class StringPositionPair implements
			Comparable<StringPositionPair> {
		Comparator<String> comparator = CommonPlugin.INSTANCE.getComparator();

		public String key;

		public int position;

		StringPositionPair(String key, int position) {
			this.key = key;
			this.position = position;
		}

		public int compareTo(StringPositionPair object) {
			if (object == this) {
				return 0;
			} else {
				StringPositionPair that = object;
				return comparator.compare(key, that.key);
			}
		}
	}

	public static boolean select(String filter, String labelValue) {
		if (filter != null && filter.length() > 0) {
			if (filter.length() > labelValue.length()) {
				return false;
			}
			for (int i = 0; i < filter.length(); i++) {
				if (Character.toLowerCase(filter.charAt(i)) != Character
						.toLowerCase(labelValue.charAt(i))) {
					return false;
				}
			}
		}
		return true;
	}

	public static <T> String[] createItems(List<T> list,
			ILabelProvider labelProvider, boolean sorted) {
		return createItems(list, labelProvider, null, sorted);
	}

	public static <T> String[] createItems(List<T> list,
			ILabelProvider labelProvider, String filter, boolean sorted) {
		String[] result;

		if (filter != null && filter.length() > 0) {
			sorted = true;
		}

		// If there are objects to populate...
		if (list != null && list.size() > 0) {
			if (sorted) {
				List<T> unsortedList = new ArrayList<T>(list.size());
				if (filter != null && filter.length() > 0) {
					for (int i = 0; i < list.size(); i++) {
						if (select(filter, labelProvider.getText(list.get(i)))) {
							unsortedList.add(list.get(i));
						}
					}
				} else {
					unsortedList.addAll(list);
				}
				list.clear();

				StringPositionPair[] pairs = new StringPositionPair[unsortedList
						.size()];

				for (int i = 0, size = unsortedList.size(); i < size; ++i) {
					Object object = unsortedList.get(i);
					pairs[i] = new StringPositionPair(labelProvider
							.getText(object), i);
				}

				Arrays.sort(pairs);

				// Create a new array.
				result = new String[unsortedList.size()];
				// Fill in the result array with labels and re-populate the
				// original list in order.
				for (int i = 0, size = unsortedList.size(); i < size; ++i) {
					result[i] = pairs[i].key;
					list.add(unsortedList.get(pairs[i].position));
				}
			} else {
				// Create a new array.
				result = new String[list.size()];
				// Fill in the array with labels.
				for (int i = 0, size = list.size(); i < size; ++i) {
					Object object = list.get(i);
					result[i] = labelProvider.getText(object);
				}
			}
		} else {
			result = new String[] { "" };
		}

		return result;
	}

	/**
	 * This keeps track of the list of model objects.
	 */
	protected List<?> originalList;

	protected List<?> list;

	protected ILabelProvider labelProvider;

	protected boolean sorted;

	public ExtendedComboBoxCellEditor(Composite composite, List<?> list,
			ILabelProvider labelProvider) {
		this(composite, list, labelProvider, false, SWT.READ_ONLY);
	}

	public ExtendedComboBoxCellEditor(Composite composite, List<?> list,
			ILabelProvider labelProvider, boolean sorted) {
		this(composite, list, labelProvider, sorted, SWT.READ_ONLY);
	}

	public ExtendedComboBoxCellEditor(Composite composite, List<?> list,
			ILabelProvider labelProvider, int style) {
		this(composite, list, labelProvider, false, style);
	}

	@SuppressWarnings("unchecked")
	public ExtendedComboBoxCellEditor(Composite composite, List<?> list,
			ILabelProvider labelProvider, boolean sorted, int style) {
		super(composite, createItems((List<Object>)
				(sorted ? list = new ArrayList<Object>(list) : list),
				labelProvider, null, sorted), style);
		this.originalList = list;
		this.list = list;
		this.labelProvider = labelProvider;
		this.sorted = sorted;
		if ((style & SWT.READ_ONLY) != 0) {
			new FilteringAdapter(getControl());
		}
	}

	protected void refreshItems(String filter) {
		CCombo combo = (CCombo) getControl();
		if (combo != null && (!combo.isDisposed())) {
			String[] items = createItems(list = new ArrayList<Object>(
					originalList), labelProvider, filter, sorted);
			combo.setItems(items);
			if (items.length > 0) {
				combo.select(0);
			}
		}
	}

	@Override
	public Object doGetValue() {
		// Get the index into the list via this call to super.
		int index = (Integer) super.doGetValue();
		return index < list.size() && index >= 0 ? list.get((Integer) super
				.doGetValue()) : null;
	}

	@Override
	public void doSetValue(Object value) {
		// Set the index of the object value in the list via this call to super.
		int index = list.indexOf(value);
		if (index != -1) {
			super.doSetValue(list.indexOf(value));
		}
	}

	public class FilteringAdapter implements KeyListener, FocusListener {
		public FilteringAdapter(Control control) {
			control.addKeyListener(this);
			control.addFocusListener(this);
		}

		private StringBuffer filter = new StringBuffer();

		private void refreshItems() {
			ExtendedComboBoxCellEditor.this.refreshItems(filter.toString());
		}

		public void keyPressed(KeyEvent e) {
			e.doit = false;

			if (e.keyCode == SWT.DEL || e.keyCode == SWT.BS) {
				if (filter.length() > 0) {
					filter = new StringBuffer(filter.substring(0, filter
							.length() - 1));
				}
			} else if (e.keyCode == SWT.ARROW_UP || e.keyCode == SWT.ARROW_DOWN
					|| e.keyCode == SWT.CR || e.keyCode == SWT.LF) {
				e.doit = true;
			} else if (e.keyCode == SWT.ESC) {
				filter = new StringBuffer();
			} else if (e.character != '\0') {
				filter.append(e.character);
			}
			if (!e.doit) {
				refreshItems();
			}
		}

		public void keyReleased(KeyEvent e) {
			// Do nothing
		}

		public void focusGained(FocusEvent e) {
			filter = new StringBuffer();
		}

		public void focusLost(FocusEvent e) {
			filter = new StringBuffer();
			refreshItems();
		}
	}
}
