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
package net.enilink.komma.edit.ui.properties.tabbed;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.views.properties.tabbed.AdvancedPropertySection;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

import net.enilink.komma.edit.domain.AdapterFactoryEditingDomain;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.edit.ui.provider.AdapterFactoryContentProvider;
import net.enilink.komma.edit.ui.util.EditUIUtil;

public class ExtendedAdvancedPropertiesSection extends AdvancedPropertySection {
	private boolean propertySourceProviderSet = false;

	@Override
	public void setInput(IWorkbenchPart part, ISelection selection) {
		if (!propertySourceProviderSet) {
			IEditingDomain editingDomain = AdapterFactoryEditingDomain.getEditingDomainFor(part);
			if (editingDomain instanceof AdapterFactoryEditingDomain) {
				AdapterFactoryContentProvider adapterFactoryContentProvider = new AdapterFactoryContentProvider(
						((AdapterFactoryEditingDomain) editingDomain).getAdapterFactory());

				page.setPropertySourceProvider(adapterFactoryContentProvider);
				propertySourceProviderSet = true;
			}
		}

		super.setInput(part, selection);
	}

	@Override
	public void createControls(Composite parent, TabbedPropertySheetPage tabbedPropertySheetPage) {
		super.createControls(parent, tabbedPropertySheetPage);
	}
}
