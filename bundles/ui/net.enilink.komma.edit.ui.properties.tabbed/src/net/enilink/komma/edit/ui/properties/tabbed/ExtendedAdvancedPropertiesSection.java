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
package net.enilink.komma.edit.ui.properties.tabbed;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.views.properties.tabbed.AdvancedPropertySection;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

import net.enilink.komma.edit.domain.AdapterFactoryEditingDomain;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.edit.domain.IEditingDomainProvider;
import net.enilink.komma.edit.ui.provider.AdapterFactoryContentProvider;

public class ExtendedAdvancedPropertiesSection extends AdvancedPropertySection {
	private boolean propertySourceProviderSet = false;

	@Override
	public void setInput(IWorkbenchPart part, ISelection selection) {
		if (!propertySourceProviderSet) {
			if (part instanceof IEditingDomainProvider) {
				IEditingDomain editingDomain = ((IEditingDomainProvider) part)
						.getEditingDomain();
				if (editingDomain instanceof AdapterFactoryEditingDomain) {
					AdapterFactoryContentProvider adapterFactoryContentProvider = new AdapterFactoryContentProvider(
							((AdapterFactoryEditingDomain) editingDomain)
									.getAdapterFactory());

					page
							.setPropertySourceProvider(adapterFactoryContentProvider);
					propertySourceProviderSet = true;
				}
			}
		}
        
		super.setInput(part, selection);
	}

	@Override
	public void createControls(Composite parent,
			TabbedPropertySheetPage tabbedPropertySheetPage) {
		super.createControls(parent, tabbedPropertySheetPage);
	}
}
