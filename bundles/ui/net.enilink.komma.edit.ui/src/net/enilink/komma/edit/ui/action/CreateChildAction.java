/**
 * <copyright> 
 *
 * Copyright (c) 2002, 2009 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   IBM - Initial API and implementation
 *
 * </copyright>
 *
 * $Id: CreateChildAction.java,v 1.5 2007/09/30 10:31:29 emerks Exp $
 */
package net.enilink.komma.edit.ui.action;

import java.util.Collection;

import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.common.command.UnexecutableCommand;
import net.enilink.komma.edit.command.CreateChildCommand;
import net.enilink.komma.edit.domain.AdapterFactoryEditingDomain;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.edit.provider.AdapterFactory;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchPart;

/**
 * A child creation action is implemented by creating a
 * {@link CreateChildCommand}.
 */
public class CreateChildAction extends StaticSelectionCommandAction {
	/**
	 * This describes the child to be created.
	 */
	protected Object descriptor;

	/**
	 * This constructs an instance of an action that uses the workbench part's
	 * editing domain to create a child specified by <code>descriptor</code> for
	 * the single object in the <code>selection</code>.
	 */
	public CreateChildAction(IWorkbenchPart workbenchPart,
			ISelection selection, Object descriptor) {
		super(workbenchPart, selection);
		this.descriptor = descriptor;
	}

	/**
	 * This constructs an instance of an action that uses the given editing
	 * domain to create a child specified by <code>descriptor</code> for the
	 * single object in the <code>selection</code>.
	 */
	public CreateChildAction(IWorkbenchPart workbenchPart,
			IEditingDomain editingDomain, ISelection selection,
			Object descriptor) {
		super(workbenchPart, editingDomain, selection);
		this.descriptor = descriptor;
	}

	/**
	 * This creates the command for
	 * {@link StaticSelectionCommandAction#createActionCommand}.
	 */
	@Override
	protected ICommand createActionCommand(IEditingDomain editingDomain,
			Collection<?> collection) {
		if (collection.size() == 1) {
			final Object owner = collection.iterator().next();
			ICommand createChildCommand = createCreateChildCommand(
					editingDomain, owner, collection);
			return createChildCommand;
		}
		return UnexecutableCommand.INSTANCE;
	}

	protected ICommand createCreateChildCommand(IEditingDomain editingDomain,
			Object owner, Collection<?> collection) {
		return CreateChildCommand.create(editingDomain, owner, descriptor,
				collection);
	}

	protected IAdapterFactory getAdapterFactory() {
		if (editingDomain instanceof AdapterFactoryEditingDomain) {
			return ((AdapterFactoryEditingDomain) editingDomain)
					.getAdapterFactory();
		}
		return new AdapterFactory() {
			@Override
			protected Object createAdapter(Object object, Object type) {
				return null;
			}
		};
	}
}
