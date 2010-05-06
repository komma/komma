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
package net.enilink.komma.edit.ui.properties.internal.parts;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import net.enilink.commons.ui.editor.AbstractEditorPart;
import net.enilink.commons.ui.editor.IEditorPart;
import net.enilink.komma.concepts.IResource;

public class DetailPropertiesPart extends AbstractEditorPart {
	private List<IEditorPart> propertyParts = new ArrayList<IEditorPart>();

	protected IResource resource;

	@Override
	public void activate() {
	}

	public void commit(boolean onSave) {
		super.commit(onSave);

		setDirty(false);
	}

	public void createContents(Composite parent) {
		parent.setLayout(new GridLayout(2, true));

		PropertyTreePart propertyPart = new PropertyTreePart();
		propertyParts.add(propertyPart);
		addPart(propertyPart);

		propertyPart.createContents(parent);
	}

	@Override
	public void deactivate() {
	}

	public IResource getResource() {
		return resource;
	}

	@Override
	public void refresh() {
		getForm().getMessageManager().setAutoUpdate(false);
		refreshParts();
		super.refresh();
		getForm().getMessageManager().setAutoUpdate(true);
	}

	public void setInput(Object input) {
		resource = (IResource) input;

		for (IEditorPart propertyPart : propertyParts) {
			propertyPart.setInput(input);
		}
	}
}
