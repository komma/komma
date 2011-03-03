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
package net.enilink.komma.edit.ui.wizards;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import net.enilink.komma.core.URI;

public abstract class ObjectNamePage extends WizardPage {
	protected Composite composite;
	protected Text nameText;
	protected URI name;

	public ObjectNamePage() {
		super("New Object");
		setTitle("Details of object.");
		setPageComplete(false);
	}

	abstract protected URI validate(String nameText);

	public void createControl(Composite parent) {
		composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));
		setControl(composite);

		Label nameLabel = new Label(composite, SWT.NONE);
		nameLabel.setText("Name");
		nameLabel
				.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		nameText = new Text(composite, SWT.BORDER);
		nameText
				.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false));
		nameText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				name = validate(getNameAsText());
			}
		});

		name = validate(getNameAsText());
	}

	protected String getNameAsText() {
		return nameText != null ? nameText.getText().trim() : "";
	}
	
	protected URI getObjectName() {
		return name;
	}
}
