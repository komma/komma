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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.views.properties.tabbed.AbstractPropertySection;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

import net.enilink.commons.ui.editor.EditorForm;
import net.enilink.commons.ui.editor.EditorPartBook;
import net.enilink.commons.ui.editor.IEditorPart;
import net.enilink.commons.ui.editor.IEditorPartProvider;
import net.enilink.komma.edit.domain.IEditingDomainProvider;
import net.enilink.komma.edit.ui.properties.IEditUIPropertiesImages;
import net.enilink.komma.edit.ui.properties.KommaEditUIPropertiesPlugin;
import net.enilink.komma.edit.ui.properties.internal.context.IPropertiesContext;
import net.enilink.komma.edit.ui.properties.internal.context.PropertiesContext;
import net.enilink.komma.edit.ui.properties.internal.parts.PropertyTreePart;
import net.enilink.komma.edit.ui.provider.ExtendedImageRegistry;

public class DetailPropertiesSection extends AbstractPropertySection {
	private EditorPartBook detailsPartBook;
	private IPageSite pageSite;

	private PropertiesContext context;

	@Override
	public void createControls(Composite parent,
			TabbedPropertySheetPage tabbedPropertySheetPage) {
		context = new PropertiesContext();

		pageSite = tabbedPropertySheetPage.getSite();

		super.createControls(parent, tabbedPropertySheetPage);

		parent.setLayout(new FillLayout());

		EditorForm editorForm = new EditorForm(parent, getWidgetFactory()) {
			@SuppressWarnings("rawtypes")
			@Override
			public Object getAdapter(Class adapter) {
				if (IPropertiesContext.class.equals(adapter)) {
					return context;
				} else if (IEditingDomainProvider.class.equals(adapter)
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

	@Override
	public void aboutToBeShown() {
		if (pageSite
				.getActionBars()
				.getToolBarManager()
				.find(this.getClass().getPackage().getName()
						+ ".hideAnonymousProperties") == null) {

			Action hideAnonymousAction = new Action("", Action.AS_CHECK_BOX) {
				@Override
				public void run() {
					context.setExcludeAnonymous(isChecked());
				}
			};
			hideAnonymousAction.setImageDescriptor(ExtendedImageRegistry
					.getInstance().getImageDescriptor(
							KommaEditUIPropertiesPlugin.INSTANCE
									.getImage((IEditUIPropertiesImages.ADD))));
			hideAnonymousAction.setToolTipText("hide Anonymous Properties");
			hideAnonymousAction.setId(this.getClass().getPackage().getName()
					+ ".hideAnonymousProperties");

			pageSite.getActionBars().getToolBarManager()
					.add(hideAnonymousAction);

			pageSite.getActionBars().updateActionBars();
		}

		if (pageSite
				.getActionBars()
				.getToolBarManager()
				.find(this.getClass().getPackage().getName()
						+ ".hideInferedProperties") == null) {

			Action excludeInferenceAction = new Action("", Action.AS_CHECK_BOX) {
				@Override
				public void run() {
					context.setExcludeInferred(isChecked());
				}
			};
			excludeInferenceAction
					.setImageDescriptor(ExtendedImageRegistry
							.getInstance()
							.getImageDescriptor(
									KommaEditUIPropertiesPlugin.INSTANCE
											.getImage((IEditUIPropertiesImages.REMOVE))));
			excludeInferenceAction.setToolTipText("hide inferred Properties");
			excludeInferenceAction.setId(this.getClass().getPackage().getName()
					+ ".hideInferedProperties");

			pageSite.getActionBars().getToolBarManager()
					.add(excludeInferenceAction);

			pageSite.getActionBars().updateActionBars();
		}

	}

	@Override
	public void aboutToBeHidden() {
		pageSite.getActionBars()
				.getToolBarManager()
				.remove(this.getClass().getPackage().getName()
						+ ".hideAnonymousProperties");

		pageSite.getActionBars()
				.getToolBarManager()
				.remove(this.getClass().getPackage().getName()
						+ ".hideInferedProperties");

		pageSite.getActionBars().updateActionBars();
	}
}
