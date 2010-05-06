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
package net.enilink.komma.edit.ui.properties.tabbed.internal.sections;

import java.util.Collection;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.views.properties.tabbed.AbstractPropertySection;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

import net.enilink.commons.ui.editor.EditorForm;
import net.enilink.commons.ui.editor.EditorPartBook;
import net.enilink.commons.ui.editor.IEditorPart;
import net.enilink.komma.concepts.IClass;
import net.enilink.komma.concepts.IProperty;
import net.enilink.komma.concepts.IResource;
import net.enilink.komma.edit.domain.IEditingDomainProvider;
import net.enilink.komma.edit.ui.properties.internal.parts.ObjectDetailsPartProvider;
import net.enilink.komma.edit.ui.properties.internal.parts.ObjectPropertiesPart;

public class PropertiesSection extends AbstractPropertySection {
	IResource resource;
	EditorPartBook detailsPartBook;

	@Override
	public void createControls(Composite parent,
			TabbedPropertySheetPage tabbedPropertySheetPage) {
		super.createControls(parent, tabbedPropertySheetPage);

		parent.setLayout(new FillLayout());

		EditorForm editorForm = new EditorForm(parent, getWidgetFactory()) {
			@Override
			public Object getAdapter(Class adapter) {
				if (IEditingDomainProvider.class.equals(adapter)) {
					if (getPart() instanceof IEditingDomainProvider) {
						return (IEditingDomainProvider) getPart();
					}
				}
				return null;
			}
		};
		
		detailsPartBook = new EditorPartBook(true, SWT.NONE);
		detailsPartBook.initialize(editorForm);
		detailsPartBook.createContents(parent);
		detailsPartBook.setPartProvider(new ObjectDetailsPartProvider() {
			@Override
			protected IEditorPart createEditorPart(Collection<IClass> classes,
					Collection<IProperty> properties) {
				return new ObjectPropertiesPart(classes, properties);
			}
		});
	}

	@Override
	public void setInput(IWorkbenchPart part, ISelection selection) {
		super.setInput(part, selection);
		resource = (IResource) ((IStructuredSelection) getSelection())
				.getFirstElement();
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
