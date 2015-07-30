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
package net.enilink.commons.ui.editor;

import org.eclipse.swt.widgets.Composite;

public interface IEditorPart {
	void addPart(IEditorPart part);

	void createContents(Composite parent);

	void commit(boolean onSave);

	void dispose();

	void initialize(IEditorForm form);

	boolean isDirty();

	boolean isStale();

	void refresh();

	void removePart(IEditorPart part);

	boolean setFocus();

	boolean setEditorInput(Object input);

	void setInput(Object input);

	void activate();

	void deactivate();
}
