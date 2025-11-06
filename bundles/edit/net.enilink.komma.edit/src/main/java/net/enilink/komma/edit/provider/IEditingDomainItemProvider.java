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
 * $Id: IEditingDomainItemProvider.java,v 1.4 2007/03/22 01:45:15 davidms Exp $
 */
package net.enilink.komma.edit.provider;

import java.util.Collection;

import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.common.util.ICollector;
import net.enilink.komma.edit.command.CommandParameter;
import net.enilink.komma.edit.domain.IEditingDomain;

/**
 * This is the interface used by {@link IEditingDomain} to impose a hierarchical
 * relation on the model objects, and to map operations on those objects onto an
 * underlying EMF model {@link ICommand}s. See {@link IEditingDomain} for more
 * details about how this is used.
 */
public interface IEditingDomainItemProvider {
	/**
	 * This does the same thing as
	 * {@link net.enilink.komma.edit.domain.IEditingDomain#getChildren
	 * EditingDomain.getChildren}, i.e., it imposes a hierarchical relation on a
	 * domain's model objects.
	 */
	Collection<?> getChildren(Object object);

	/**
	 * This does the same thing as
	 * {@link net.enilink.komma.edit.domain.IEditingDomain#getParent
	 * EditingDomain.getParent}, i.e., it imposes a hierarchical relation on a
	 * domain's model objects.
	 */
	Object getParent(Object object);

	/**
	 * This does the same thing as
	 * {@link net.enilink.komma.edit.domain.IEditingDomain#getNewChildDescriptors
	 * EditingDomain.getNewChildDescriptors}, i.e., it returns a collection of
	 * objects describing the children that can be added under an object in the
	 * editing domain.
	 */
	void getNewChildDescriptors(Object object,
	                            IEditingDomain editingDomain, Object sibling,
	                            ICollector<Object> descriptors);

	/**
	 * This does the same thing as
	 * {@link net.enilink.komma.edit.domain.IEditingDomain#createCommand
	 * EditingDomain.createCommand}, i.e., it creates commands for a domain's
	 * model objects.
	 */
	ICommand createCommand(Object object, IEditingDomain editingDomain,
	                       Class<? extends ICommand> commandClass,
	                       CommandParameter commandParameter);
}
