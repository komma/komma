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
package net.enilink.commons.ui.editor;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.Section;

/**
 * Section part implements IEditorPart interface based on the Section widget. It
 * can either wrap the widget or create one itself.
 * <p>
 * Subclasses should extend <code>SectionPart</code> and implement life cycle
 * methods like <code>refresh</code>, <code>commit</code>, <code>setFocus</code>
 * etc. Note that most of these methods are not empty - calling
 * <code>super</code> is required.
 * 
 * @see Section
 */
public class SectionPart extends AbstractEditorPart {
	private int style;
	private Section section;

	/**
	 * Creates a new section part based on the provided section.
	 * 
	 * @param section
	 *            the section to use
	 */
	public SectionPart(Section section) {
		this.section = section;
		hookListeners();
	}

	public SectionPart(int style) {
		this.style = style;
	}

	public SectionPart() {
		this.style = SWT.NONE;
	}

	@Override
	public void createContents(Composite parent) {
		if (section == null) {
			section = getWidgetFactory().createSection(parent, style);
		}
	}

	@Override
	public void setInput(Object input) {
	}

	/**
	 * Adds listeners to the underlying widget.
	 */
	protected void hookListeners() {
		if ((section.getExpansionStyle() & Section.TWISTIE) != 0
				|| (section.getExpansionStyle() & Section.TREE_NODE) != 0) {
			section.addExpansionListener(new ExpansionAdapter() {
				public void expansionStateChanging(ExpansionEvent e) {
					SectionPart.this.expansionStateChanging(e.getState());
				}

				public void expansionStateChanged(ExpansionEvent e) {
					SectionPart.this.expansionStateChanged(e.getState());
				}
			});
		}
	}

	/**
	 * Returns the section widget used in this part.
	 * 
	 * @return the section widget
	 */
	public Section getSection() {
		return section;
	}

	/**
	 * The section is about to expand or collapse.
	 * 
	 * @param expanding
	 *            <code>true</code> for expansion, <code>false</code> for
	 *            collapse.
	 */
	protected void expansionStateChanging(boolean expanding) {
	}

	/**
	 * The section has expanded or collapsed.
	 * 
	 * @param expanded
	 *            <code>true</code> for expansion, <code>false</code> for
	 *            collapse.
	 */
	protected void expansionStateChanged(boolean expanded) {
		getForm().reflow(false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.AbstractFormPart#setFocus()
	 */
	public boolean setFocus() {
		Control client = section.getClient();
		if (client != null) {
			client.setFocus();
			return true;
		}
		return false;
	}
}
