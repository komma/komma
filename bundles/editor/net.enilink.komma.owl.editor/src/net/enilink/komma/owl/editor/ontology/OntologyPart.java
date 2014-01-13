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
package net.enilink.komma.owl.editor.ontology;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.Section;

import net.enilink.komma.edit.ui.views.AbstractEditingDomainPart;

/**
 * Extends FormPage that is itself an extension of EditorPart
 * 
 */
public class OntologyPart extends AbstractEditingDomainPart {
	protected ImportsPart importsPart;
	protected NamespacesPart namespacePart;

	@Override
	public void createContents(Composite parent) {
		parent.setLayout(new FillLayout(SWT.VERTICAL));

		importsPart = new ImportsPart();
		addPart(importsPart);
		importsPart.createContents(createSection(parent, "Imports"));

		namespacePart = new NamespacesPart();
		addPart(namespacePart);
		namespacePart.createContents(createSection(parent, "Namespaces"));
	}

	private Composite createSection(Composite parent, String name) {
		Section section = getWidgetFactory().createSection(parent,
				Section.TITLE_BAR | Section.EXPANDED);
		section.setText(name);

		Composite client = getWidgetFactory()
				.createComposite(section, SWT.NONE);
		section.setClient(client);
		return client;
	}

	@Override
	public void refresh() {
		refreshParts();
		super.refresh();
	}

	@Override
	public void setInput(Object input) {
		importsPart.setInput(input);
		namespacePart.setInput(input);
	}
}
