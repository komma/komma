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

import org.eclipse.jface.viewers.ISelection;

/**
 * Form parts can implement this interface if they want to be 
 * notified when another part on the same form changes selection 
 * state.
 * 
 */
public interface IPartSelectionListener {
	/**
	 * Called when the provided part has changed selection state.
	 * 
	 * @param part
	 *            the selection source
	 * @param selection
	 *            the new selection
	 */
	public void selectionChanged(IEditorPart part, ISelection selection);
}
