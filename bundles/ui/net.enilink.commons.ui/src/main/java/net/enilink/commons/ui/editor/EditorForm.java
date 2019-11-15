/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package net.enilink.commons.ui.editor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.IMessage;
import org.eclipse.ui.forms.IMessageManager;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.SharedScrolledComposite;

/**
 * Managed form wraps a form widget and adds life cycle methods for form parts.
 * A form part is a portion of the form that participates in form life cycle
 * events.
 * <p>
 * There is requirement for 1/1 mapping between widgets and form parts. A widget
 * like Section can be a part by itself, but a number of widgets can join around
 * one form part.
 * <p>
 * Note to developers: this class is left public to allow its use beyond the
 * original intention (inside a multi-page editor's page). You should limit the
 * use of this class to make new instances inside a form container (wizard page,
 * dialog etc.). Clients that need access to the class should not do it
 * directly. Instead, they should do it through IManagedForm interface as much
 * as possible.
 */
public class EditorForm implements IEditorForm {
	private Object input;

	private EditorWidgetFactory widgetFactory;

	private boolean ownsWidgetFactory;

	private boolean initialized;

	private MessageManager messageManager;

	private List<IEditorPart> parts = new ArrayList<IEditorPart>();

	private Composite parent;

	private List<IEditorStateListener> stateListeners = new ArrayList<IEditorStateListener>();

	/**
	 * Creates a managed form in the provided parent. Form toolkit and widget
	 * will be created and owned by this object.
	 * 
	 * @param parent
	 *            the parent widget
	 */
	public EditorForm(Composite parent) {
		this.parent = parent;
		widgetFactory = new EditorWidgetFactory(parent.getDisplay());
		ownsWidgetFactory = true;
	}

	/**
	 * Creates a managed form that will use the provided widget factory
	 * 
	 * @param widgetFactory
	 */
	public EditorForm(Composite parent, EditorWidgetFactory widgetFactory) {
		this.parent = parent;
		this.widgetFactory = widgetFactory;
	}

	/**
	 * Creates a managed form that will use the provided widget factory
	 * 
	 * @param widgetFactory
	 */
	public EditorForm(Composite parent, FormToolkit formToolkit) {
		this(parent, new EditorWidgetFactory(formToolkit));
	}

	public void addPart(IEditorPart part) {
		parts.add(part);
		part.initialize(this);
	}

	public void removePart(IEditorPart part) {
		parts.remove(part);
	}

	public IEditorPart[] getParts() {
		return parts.toArray(new IEditorPart[parts.size()]);
	}

	public EditorWidgetFactory getWidgetFactory() {
		return widgetFactory;
	}

	/**
	 * Initializes the form by looping through the managed parts and
	 * initializing them. Has no effect if already called once.
	 */
	public void initialize() {
		if (initialized)
			return;
		for (int i = 0; i < parts.size(); i++) {
			IEditorPart part = parts.get(i);
			part.initialize(this);
		}
		initialized = true;
	}

	/**
	 * Disposes all the parts in this form.
	 */
	public void dispose() {
		for (int i = 0; i < parts.size(); i++) {
			IEditorPart part = parts.get(i);
			part.dispose();
		}
		if (ownsWidgetFactory) {
			widgetFactory.dispose();
		}
	}

	/**
	 * Refreshes the form by refreshes all the stale parts. Since 3.1, this
	 * method is performed on a UI thread when called from another thread so it
	 * is not needed to wrap the call in <code>Display.syncExec</code> or
	 * <code>asyncExec</code>.
	 */
	public void refreshStale() {
		Thread t = Thread.currentThread();
		Thread dt = widgetFactory.getColors().getDisplay().getThread();
		if (t.equals(dt))
			doRefreshStale();
		else {
			widgetFactory.getColors().getDisplay().asyncExec(new Runnable() {
				public void run() {
					doRefreshStale();
				}
			});
		}
	}

	private void doRefreshStale() {
		int nrefreshed = 0;
		for (int i = 0; i < parts.size(); i++) {
			IEditorPart part = parts.get(i);
			if (part.isStale()) {
				part.refresh();
				nrefreshed++;
			}
		}
		if (nrefreshed > 0) {
			reflow(true);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IManagedForm#commit(boolean)
	 */
	public void commit(boolean onSave) {
		for (IEditorPart part : parts) {
			if (part.isDirty()) {
				part.commit(onSave);
			}
		}
	}

	public void reflow(boolean changed) {
		if (parent instanceof SharedScrolledComposite) {
			((SharedScrolledComposite) parent).reflow(changed);
		} else if (parent.getParent() instanceof SharedScrolledComposite) {
			((SharedScrolledComposite) parent.getParent()).reflow(changed);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IManagedForm#setInput(java.lang.Object)
	 */
	public boolean setInput(Object input) {
		boolean partResult = false;

		this.input = input;
		for (IEditorPart part : parts) {
			boolean result = part.setEditorInput(input);
			if (result) {
				partResult = true;
			}
		}
		return partResult;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IManagedForm#getInput()
	 */
	public Object getInput() {
		return input;
	}

	/**
	 * Transfers the focus to the first form part which wants to own it.
	 */
	public void setFocus() {
		for (IEditorPart part : parts) {
			if (part.setFocus()) {
				return;
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IManagedForm#isDirty()
	 */
	public boolean isDirty() {
		for (IEditorPart part : parts) {
			if (part.isDirty()) {
				return true;
			}
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IManagedForm#isStale()
	 */
	public boolean isStale() {
		for (IEditorPart part : parts) {
			if (part.isStale()) {
				return true;
			}
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IManagedForm#dirtyStateChanged()
	 */
	public void dirtyStateChanged() {
		fireDirtyStateChanged();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IManagedForm#staleStateChanged()
	 */
	public void staleStateChanged() {
		fireStaleStateChanged();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IManagedForm#getMessageManager()
	 */
	public IMessageManager getMessageManager() {
		if (messageManager == null) {
			messageManager = new MessageManager(new IMessageContainer() {
				@Override
				public Composite getComposite() {
					if (parent instanceof ScrolledForm) {
						return ((ScrolledForm) parent).getBody();
					}
					return parent;
				}

				@Override
				public void setMessage(String newMessage, int newType,
						IMessage[] messages) {
					if (parent instanceof ScrolledForm) {
						((ScrolledForm) parent).setMessage(newMessage, newType,
								messages);
					} else if (parent.getParent() instanceof ScrolledForm) {
						((ScrolledForm) parent.getParent()).setMessage(
								newMessage, newType, messages);
					}
				}

				@Override
				public void setMessage(String newMessage, int newType) {
					setMessage(newMessage, newType, null);
				}
			});
		}
		return messageManager;
	}

	public Shell getShell() {
		return parent.getShell();
	}

	private void fireDirtyStateChanged() {
		for (IEditorStateListener listener : new ArrayList<IEditorStateListener>(
				stateListeners)) {
			listener.dirtyStateChanged(this);
		}
	}

	private void fireStaleStateChanged() {
		for (IEditorStateListener listener : new ArrayList<IEditorStateListener>(
				stateListeners)) {
			listener.staleStateChanged(this);
		}
	}

	/**
	 * A part can use this method to notify other parts that implement
	 * IPartSelectionListener about selection changes.
	 * 
	 * @param firingPart
	 *            the part that broadcasts the selection
	 * @param selection
	 *            the selection in the part
	 * @see IPartSelectionListener
	 */
	public void fireSelectionChanged(IEditorPart firingPart,
			ISelection selection) {
		for (IEditorPart part : parts) {
			if (firingPart.equals(part)) {
				continue;
			}
			if (part instanceof IPartSelectionListener) {
				((IPartSelectionListener) part).selectionChanged(firingPart,
						selection);
			}
		}
	}

	@Override
	public Composite getBody() {
		if (parent instanceof ScrolledForm) {
			return ((ScrolledForm) parent).getBody();
		}
		return parent;
	}

	public boolean addStateListener(IEditorStateListener stateListener) {
		if (!stateListeners.contains(stateListener)) {
			stateListeners.add(stateListener);
			return true;
		}
		return false;
	}

	public boolean removeStateListener(IEditorStateListener stateListener) {
		return stateListeners.remove(stateListener);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Object getAdapter(Class adapter) {
		return null;
	}
}