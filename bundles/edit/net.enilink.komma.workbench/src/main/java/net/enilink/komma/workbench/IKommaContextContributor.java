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
 *  $$RCSfile: IEMFContextContributor.java,v $$
 *  $$Revision: 1.2 $$  $$Date: 2005/02/15 23:04:14 $$ 
 */

package net.enilink.komma.workbench;

import net.enilink.komma.workbench.nature.KommaNature;

/**
 * KOMMA Context Contributor interface. Implementers are called to contribute to
 * the context.
 * 
 * @see WorkbenchModelHelperBase#createKommaContext(IProject,
 *      IKommaContextContributor)
 * @since 1.0.0
 */
public interface IKommaContextContributor {

	/**
	 * This is your opportunity to add a primary {@link KommaNature}. Typically
	 * you would add to the WorkbenchContext held by <code>nature</code> in
	 * order to change the container for the WorkbenchURIConverter or add
	 * adapter factories to the ResourceSet or anything else that is needed.
	 * 
	 * @param aNature
	 * 
	 * @since 1.0.0
	 */
	void primaryContributeToContext(KommaWorkbenchContextBase nature);

	/**
	 * This is your opportunity to add a secondary {@link KommaNature}.
	 * Typically you would add to the WorkbenchContext held by
	 * <code>nature</code> in order to change the container for the
	 * WorkbenchURIConverter or add adapter factories to the ResourceSet or
	 * anything else that is needed.
	 * 
	 * @param aNature
	 * 
	 * @since 1.0.0
	 */
	void secondaryContributeToContext(KommaWorkbenchContextBase nature);

}