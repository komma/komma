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
package net.enilink.komma.edit.ui.properties.tabbed.internal.sections;

import net.enilink.commons.ui.editor.EditorForm;
import net.enilink.commons.ui.editor.EditorPartBook;
import net.enilink.commons.ui.editor.IEditorPart;
import net.enilink.commons.ui.editor.IEditorPartProvider;
import net.enilink.komma.edit.domain.IEditingDomainProvider;
import net.enilink.komma.edit.ui.properties.internal.parts.PropertyTreePart;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.views.properties.tabbed.AbstractPropertySection;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

public class DetailPropertiesSection extends AbstractPropertySection {
	private EditorPartBook detailsPartBook;

	@Override
	public void createControls(Composite parent,
			TabbedPropertySheetPage tabbedPropertySheetPage) {
		super.createControls(parent, tabbedPropertySheetPage);
		parent.setLayout(new FillLayout());
		EditorForm editorForm = new EditorForm(parent, getWidgetFactory()) {
			@SuppressWarnings("rawtypes")
			@Override
			public Object getAdapter(Class adapter) {
				if (IEditingDomainProvider.class.equals(adapter)
						&& getPart() != null) {
					return getPart().getAdapter(adapter);
				}
				return null;
			}
		};
		detailsPartBook = new EditorPartBook(true, SWT.NONE);
		detailsPartBook.initialize(editorForm);
		detailsPartBook.createContents(parent);
		detailsPartBook.setPartProvider(new IEditorPartProvider() {
			@Override
			public Object getPartKey(Object object) {
				return DetailPropertiesSection.this.getPart();
			}

			@Override
			public IEditorPart getPart(Object key) {
				return new PropertyTreePart();
			}
		});
	}

	@Override
	public void setInput(IWorkbenchPart part, ISelection selection) {
		super.setInput(part, selection);
		detailsPartBook.selectionChanged(selection);
	}

	@Override
	public void refresh() {
		detailsPartBook.refresh();
	}

	public boolean shouldUseExtraSpace() {
		return true;
	}
}
