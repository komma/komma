/*******************************************************************************
 *  Copyright (c) 2003, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Fraunhofer IWU - nestable editor parts
 *******************************************************************************/
package net.enilink.commons.ui.editor;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.IMessageManager;


public interface IEditorForm extends IAdaptable {
	/**
	 * Initializes the form by looping through the managed parts and
	 * initializing them. Has no effect if already called once.
	 */
	public void initialize();

	/**
	 * Returns the toolkit used by this form.
	 * 
	 * @return the toolkit
	 */
	public EditorWidgetFactory getWidgetFactory();

	/**
	 * A part can use this method to notify other parts that implement
	 * IPartSelectionListener about selection changes.
	 * 
	 * @param part
	 *            the part that broadcasts the selection
	 * @param selection
	 *            the selection in the part
	 */
	public void fireSelectionChanged(IEditorPart part, ISelection selection);

	/**
	 * Returns all the parts currently managed by this form.
	 * 
	 * @return the managed parts
	 */
	IEditorPart[] getParts();

	/**
	 * Adds the new part to the form.
	 * 
	 * @param part
	 *            the part to add
	 */
	void addPart(IEditorPart part);

	/**
	 * Removes the part from the form.
	 * 
	 * @param part
	 *            the part to remove
	 */
	void removePart(IEditorPart part);

	/**
	 * Sets the input of this page to the provided object.
	 * 
	 * @param input
	 *            the new page input
	 * @return <code>true</code> if the form contains this object,
	 *         <code>false</code> otherwise.
	 */
	boolean setInput(Object input);

	/**
	 * Returns the current page input.
	 * 
	 * @return page input object or <code>null</code> if not applicable.
	 */
	Object getInput();

	/**
	 * Tests if form is dirty. A managed form is dirty if at least one managed
	 * part is dirty.
	 * 
	 * @return <code>true</code> if at least one managed part is dirty,
	 *         <code>false</code> otherwise.
	 */
	boolean isDirty();

	/**
	 * Notifies the form that the dirty state of one of its parts has changed.
	 * The global dirty state of the form can be obtained by calling 'isDirty'.
	 * 
	 * @see #isDirty
	 */
	void dirtyStateChanged();

	/**
	 * Commits the dirty form. All pending changes in the widgets are flushed
	 * into the model.
	 * 
	 * @param onSave
	 */
	void commit(boolean onSave);

	/**
	 * Tests if form is stale. A managed form is stale if at least one managed
	 * part is stale. This can happen when the underlying model changes,
	 * resulting in the presentation of the part being out of sync with the
	 * model and needing refreshing.
	 * 
	 * @return <code>true</code> if the form is stale, <code>false</code>
	 *         otherwise.
	 */
	boolean isStale();

	/**
	 * Notifies the form that the stale state of one of its parts has changed.
	 * The global stale state of the form can be obtained by calling 'isStale'.
	 */
	void staleStateChanged();

	/**
	 * Refreshes the form by refreshing every part that is stale.
	 */
	void refreshStale();

	/**
	 * Returns the message manager that will keep track of messages in this
	 * form.
	 * 
	 * @return the message manager instance
	 * @since 3.3
	 */
	IMessageManager getMessageManager();

	Shell getShell();

	Composite getBody();

	boolean addStateListener(IEditorStateListener stateListener);

	boolean removeStateListener(IEditorStateListener stateListener);

	void reflow(boolean changed);
}
