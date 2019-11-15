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
package net.enilink.komma.sparql.ui.views;

import java.util.Collection;
import java.util.Set;

import net.enilink.commons.ui.editor.EditorWidgetFactory;
import net.enilink.komma.core.INamespace;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Composite;

public interface IResultViewer extends ISelectionProvider {
	void createContents(EditorWidgetFactory widgetFactory, Composite parent);

	String getName();

	void setData(Set<INamespace> namespaces, String[] colNames,
			Collection<Object[]> data);
}
