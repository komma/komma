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
package net.enilink.komma.edit.command;

import java.util.Collection;

import net.enilink.komma.core.URI;

/**
 * This interface may be used for child descriptions if the new child requires
 * an unique name.
 * 
 * @author Ken Wenzel
 */
public interface IChildDescriptor {
	/**
	 * Returns whether a name is required for the child described by this
	 * description or not.
	 * 
	 * @param childDescription
	 *            The description for a new child.
	 * 
	 * @return <code>true</code> if a name is required for the new child, else
	 *         <code>false</code>.
	 */
	boolean requiresName();

	/**
	 * Returns whether a type is required for the child described by this
	 * description or not.
	 * 
	 * @param childDescription
	 *            The description for a new child.
	 * 
	 * @return <code>true</code> if a name is required for the new child, else
	 *         <code>false</code>.
	 */
	boolean requiresType();

	/**
	 * Assigns a <code>name</code> to the this child description.
	 * 
	 * @param name
	 *            The name for the new child.
	 */
	void setName(URI name);

	/**
	 * Assigns one or more <code>types</code> to the this child description.
	 * 
	 * @param types
	 *            The types of the new child.
	 */
	void setTypes(Collection<? extends Object> types);
}
