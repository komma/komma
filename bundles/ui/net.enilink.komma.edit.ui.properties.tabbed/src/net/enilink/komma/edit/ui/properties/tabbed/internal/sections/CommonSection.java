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

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.views.properties.tabbed.AbstractPropertySection;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

import net.enilink.commons.ui.editor.EditorForm;
import net.enilink.komma.edit.ui.properties.internal.parts.ObjectTypesPart;
import net.enilink.komma.core.IEntity;

public class CommonSection extends AbstractPropertySection {
	IEntity resource;
	boolean dirty = false;
	Text nameText;
	ObjectTypesPart typesPart;

	@Override
	public void createControls(Composite parent,
			TabbedPropertySheetPage tabbedPropertySheetPage) {
		super.createControls(parent, tabbedPropertySheetPage);

		parent.setLayout(new GridLayout(2, false));

		Label label = getWidgetFactory().createLabel(parent, "Name");
		GridData gridData = new GridData(SWT.BEGINNING, SWT.CENTER, false,
				false);
		label.setLayoutData(gridData);

		nameText = getWidgetFactory().createText(parent, "");
		nameText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dirty = true;
			}
		});
		nameText.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {
			}

			@Override
			public void focusLost(FocusEvent e) {
				renameResource();
			}
		});
		nameText.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				renameResource();
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
			}
		});

		gridData = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
		nameText.setLayoutData(gridData);

		gridData = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
		gridData.horizontalSpan = 2;
		Composite composite = getWidgetFactory().createComposite(parent);
		composite.setLayoutData(gridData);
		EditorForm editorForm = new EditorForm(parent, getWidgetFactory());
		typesPart = new ObjectTypesPart();
		typesPart.initialize(editorForm);
		typesPart.createContents(composite);
	}

	@Override
	public void setInput(IWorkbenchPart part, ISelection selection) {
		super.setInput(part, selection);
		resource = (IEntity) ((IStructuredSelection) getSelection())
				.getFirstElement();
		typesPart.setInput(resource);
	}

	@Override
	public void refresh() {
		refreshName(resource);
		typesPart.refresh();
		super.refresh();
	}

	protected void renameResource() {
		if (dirty) {
			String newName = nameText.getText();
			if (newName.length() > 0) {
				// String newUri = resource.getNameSpace() + newName;
				//
				// OntResource other =
				// resource.getOntModel().getOntResource(newUri);
				// if (other == null || !other.equals(resource)) {
				// OntResource oldResource = resource;
				// resource = OntUtil.renameOntResource(
				// oldResource, newUri);
				// }
			} else {
				refreshName(resource);
			}
		}
		dirty = false;
	}

	protected void refreshName(IEntity resource) {
		if (resource.getURI() != null) {
			nameText.setText(resource.getURI().localPart());
		} else {
			nameText.setText("");
		}
	}
}
