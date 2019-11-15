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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.widgets.FormToolkit;

public abstract class AbstractEditorPart implements IEditorPart {
	private Set<IEditorPart> parts = new HashSet<IEditorPart>();
	private IEditorForm form;
	private boolean dirty = false, stale = false;
	private boolean ignoreChanges = false;
	private Display display;

	@Override
	public void activate() {
		for (IEditorPart part : parts) {
			part.activate();
		}
	}

	public void addPart(IEditorPart part) {
		addPart(part, true);
	}

	public void addPart(IEditorPart part, boolean initialize) {
		if (initialize) {
			initialize(part);
		}
		parts.add(part);
	}

	protected void addTextBorder(Control control) {
		control.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
	}

	protected void asyncExec(Runnable runnable) {
		display.asyncExec(runnable);
	}

	public void commit(boolean onSave) {
		commitParts(onSave);
	}

	protected Collection<IEditorPart> getParts() {
		return parts;
	}

	protected void commitParts(boolean onSave) {
		for (IEditorPart part : parts) {
			if (part.isDirty()) {
				part.commit(onSave);
			}
		}
	}

	@Override
	public void deactivate() {
		for (IEditorPart part : parts) {
			part.deactivate();
		}
	}

	public void dispose() {
		disposeParts();
		form = null;
	}

	protected void disposeParts() {
		for (IEditorPart part : parts) {
			part.dispose();
		}
	}

	protected IEditorForm getForm() {
		return form;
	}

	protected Shell getShell() {
		return form.getShell();
	}

	protected EditorWidgetFactory getWidgetFactory() {
		return getForm().getWidgetFactory();
	}

	protected void initialize(IEditorPart subPart) {
		subPart.initialize(getForm());
	}

	public void initialize(IEditorForm form) {
		this.form = form;
		this.display = form.getShell().getDisplay();
	}

	public boolean isDirty() {
		return dirty || partsDirty();
	}

	public boolean isStale() {
		return stale;
	}

	private boolean partsDirty() {
		for (IEditorPart part : parts) {
			if (part.isDirty()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean setFocus() {
		for (IEditorPart part : parts) {
			if (part.setFocus()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void setInput(Object input) {

	}

	public void refresh() {
		setDirty(false);
		setStale(false);
	}

	protected void refreshParts() {
		for (IEditorPart part : parts) {
			part.refresh();
		}
	}

	public void removePart(IEditorPart part) {
		parts.remove(part);
	}

	public void setDirty(boolean dirty) {
		if (ignoreChanges) {
			return;
		}

		if (this.dirty != dirty) {
			this.dirty = dirty;

			syncExec(new Runnable() {
				public void run() {
					getForm().dirtyStateChanged();
				}
			});
		}
	}

	public boolean setEditorInput(Object input) {
		boolean partResult = false;

		for (IEditorPart part : parts) {
			boolean result = part.setEditorInput(input);
			if (result) {
				partResult = true;
			}
		}
		return partResult;
	}

	protected void setIgnoreChanges(boolean ignoreChanges) {
		this.ignoreChanges = ignoreChanges;
	}

	protected boolean getIgnoreChanges() {
		return ignoreChanges;
	}

	public void setStale(boolean stale) {
		if (ignoreChanges)
			return;

		if (this.stale != stale) {
			this.stale = stale;

			syncExec(new Runnable() {
				public void run() {
					getForm().staleStateChanged();
				}
			});
		}
	}

	protected void syncExec(Runnable runnable) {
		if (Display.getCurrent() != null) {
			runnable.run();
		} else {
			this.display.syncExec(runnable);
		}
	}
}
