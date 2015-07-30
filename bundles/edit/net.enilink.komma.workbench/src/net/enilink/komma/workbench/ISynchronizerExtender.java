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
 *  $$RCSfile: ISynchronizerExtender.java,v $$
 *  $$Revision: 1.2 $$  $$Date: 2005/02/15 23:04:14 $$ 
 */
package net.enilink.komma.workbench;

import org.eclipse.core.resources.IResourceDelta;

/**
 * Allows clients to extend the behavior of the
 * {@link ModelSetWorkbenchSynchronizer}.
 * 
 * @see ModelSetWorkbenchSynchronizer#addExtender(ISynchronizerExtender)
 * @since 1.0.0
 */
public interface ISynchronizerExtender {

	/**
	 * Notification that project has changed.
	 * 
	 * @param delta
	 * 
	 * @since 1.0.0
	 */
	void projectChanged(IResourceDelta delta);

	/**
	 * Notification that project has been closed.
	 * 
	 * 
	 * @since 1.0.0
	 */
	void projectClosed();
}