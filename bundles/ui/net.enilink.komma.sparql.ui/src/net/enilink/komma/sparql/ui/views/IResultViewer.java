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
package net.enilink.komma.sparql.ui.views;

import java.util.Collection;

import org.eclipse.swt.widgets.Composite;

import net.enilink.commons.ui.editor.EditorWidgetFactory;

public interface IResultViewer {
	void createContents(EditorWidgetFactory widgetFactory, Composite parent);

	String getName();

	void setData(String[] colNames, Collection<Object[]> data);

	Collection<?> getSelection();
}
