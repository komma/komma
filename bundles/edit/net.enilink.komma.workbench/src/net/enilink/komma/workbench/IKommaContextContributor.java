/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 *  $$RCSfile: IEMFContextContributor.java,v $$
 *  $$Revision: 1.2 $$  $$Date: 2005/02/15 23:04:14 $$ 
 */

package net.enilink.komma.workbench;

/**
 * Komma Context Contributor interface. Implementers are called to contribute to the context.
 * 
 * @see WorkbenchModelHelperBase#createKommaContext(IProject, IKommaContextContributor)
 * @since 1.0.0
 */
public interface IKommaContextContributor {

	/**
	 * This is your opportunity to add a primary EMFNature. Typically you would add to the WorkbenchContext held by <code>aNature</code> in order to
	 * change the container for the WorkbenchURIConverter or add adapter factories to the ResourceSet or anything else that is needed.
	 * 
	 * @param aNature
	 * 
	 * @since 1.0.0
	 */
	void primaryContributeToContext(KommaWorkbenchContextBase nature);

	/**
	 * This is your opportunity to add a secondary EMFNature. Typically you would add to the WorkbenchContext held by <code>aNature</code> in order
	 * to change the container for the WorkbenchURIConverter or add adapter factories to the ResourceSet or anything else that is needed.
	 * 
	 * @param aNature
	 * 
	 * @since 1.0.0
	 */
	void secondaryContributeToContext(KommaWorkbenchContextBase nature);

}