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
package net.enilink.commons.ui.editor;

import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.IManagedForm;

public class FormPart implements IFormPart {
	protected IManagedForm managedForm;
	protected EditorForm editorForm;

	public FormPart(EditorForm editorForm) {
		this.editorForm = editorForm;
		
		editorForm.addStateListener(new IEditorStateListener() {
			@Override
			public void dirtyStateChanged(IEditorForm editor) {
				managedForm.dirtyStateChanged();
			}

			@Override
			public void staleStateChanged(IEditorForm editor) {
				managedForm.staleStateChanged();
			}
		});
	}

	@Override
	public void commit(boolean onSave) {
		editorForm.commit(onSave);
	}

	@Override
	public void dispose() {

	}

	@Override
	public void initialize(IManagedForm form) {
		this.managedForm = form;
	}

	@Override
	public boolean isDirty() {
		return editorForm.isDirty();
	}

	@Override
	public boolean isStale() {
		return editorForm.isStale();
	}

	@Override
	public void refresh() {
		editorForm.refreshStale();
	}

	@Override
	public void setFocus() {
		editorForm.setFocus();
	}

	@Override
	public boolean setFormInput(Object input) {
		return editorForm.setInput(input);
	}
}
