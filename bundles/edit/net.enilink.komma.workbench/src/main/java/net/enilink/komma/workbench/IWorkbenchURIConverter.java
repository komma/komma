/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 *  $$RCSfile: WorkbenchURIConverter.java,v $$
 *  $$Revision: 1.2 $$  $$Date: 2005/02/15 23:04:14 $$ 
 */
package net.enilink.komma.workbench;

import java.util.List;

import org.eclipse.core.resources.IContainer;

import net.enilink.komma.model.IURIConverter;

/**
 * Implementers of this interface are WorkbenchURI converters. Workbench URI
 * converters handle references to files in the project's containers.
 * 
 * @since 1.0.0
 */
public interface IWorkbenchURIConverter extends IURIConverter {
	/**
	 * Add input container to to the converter.
	 * 
	 * @param container
	 * 
	 * @since 1.0.0
	 */
	void addInputContainer(IContainer container);

	/**
	 * Get input containers.
	 * 
	 * @return all input containers.
	 * 
	 * @since 1.0.0
	 */
	List<IContainer> getInputContainers();

	/**
	 * Remove input container from list.
	 * 
	 * @param container
	 * @return <code>true</code> if removed.
	 * 
	 * @since 1.0.0
	 */
	boolean removeInputContainer(IContainer container);
}