/**
 * <copyright> 
 *
 * Copyright (c) 2002, 2009 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 * 
 * Contributors: 
 *   IBM - Initial API and implementation
 *
 * </copyright>
 *
 * $Id: CreateSiblingAction.java,v 1.5 2007/09/30 10:31:30 emerks Exp $
 */
package net.enilink.komma.edit.ui.action;

import java.util.Collection;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchPart;

import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.edit.command.CreateChildCommand;
import net.enilink.komma.edit.domain.IEditingDomain;

/**
 * A sibling creation action is implemented by creating a
 * {@link CreateChildCommand}.
 */
public class CreateSiblingAction extends CreateChildAction {
	/**
	 * This constructs an instance of an action that uses the workbench part's
	 * editing domain to create a sibling specified by <code>descriptor</code>.
	 */
	public CreateSiblingAction(IWorkbenchPart workbenchPart,
			ISelection selection, Object descriptor) {
		super(workbenchPart, selection, descriptor);
	}

	/**
	 * This constructs an instance of an action that uses the given editing
	 * domain to create a sibling specified by <code>descriptor</code>.
	 */
	public CreateSiblingAction(IWorkbenchPart workbenchPart,
			IEditingDomain editingDomain, ISelection selection,
			Object descriptor) {
		super(workbenchPart, editingDomain, selection, descriptor);
	}

	@Override
	protected ICommand createCreateChildCommand(IEditingDomain editingDomain,
			Object owner, Collection<?> collection) {
		return CreateChildCommand.create(editingDomain, null, descriptor,
				collection);
	}
}
