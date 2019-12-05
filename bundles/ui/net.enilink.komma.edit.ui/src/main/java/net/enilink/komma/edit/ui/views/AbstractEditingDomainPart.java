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
package net.enilink.komma.edit.ui.views;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredViewer;

import net.enilink.commons.ui.editor.AbstractEditorPart;
import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.edit.domain.AdapterFactoryEditingDomain;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.edit.domain.IEditingDomainProvider;
import net.enilink.komma.edit.provider.AdapterFactory;

public abstract class AbstractEditingDomainPart extends AbstractEditorPart {
	protected IAdapterFactory getAdapterFactory() {
		IEditingDomain editingDomain = getEditingDomain();
		if (editingDomain instanceof AdapterFactoryEditingDomain) {
			return ((AdapterFactoryEditingDomain) editingDomain)
					.getAdapterFactory();
		}
		return new AdapterFactory() {
			@Override
			protected Object createAdapter(Object object, Object type) {
				return null;
			}
		};
	}

	protected IEditingDomain getEditingDomain() {
		IEditingDomainProvider editingDomainProvider = (IEditingDomainProvider) getForm()
				.getAdapter(IEditingDomainProvider.class);
		if (editingDomainProvider != null) {
			return editingDomainProvider.getEditingDomain();
		}
		return null;
	}

	protected void createContextMenuFor(StructuredViewer viewer) {
		IViewerMenuSupport menuSupport = (IViewerMenuSupport) getForm()
				.getAdapter(IViewerMenuSupport.class);
		if (menuSupport != null) {
			menuSupport.createContextMenuFor(viewer, getForm().getBody(), null);
		}
	}

	protected void setSelectionProvider(ISelectionProvider selectionProvider) {
		selectionProvider
				.addSelectionChangedListener(new ISelectionChangedListener() {
					@Override
					public void selectionChanged(SelectionChangedEvent event) {
						getForm().fireSelectionChanged(
								AbstractEditingDomainPart.this,
								event.getSelection());
					}
				});
	}
}
