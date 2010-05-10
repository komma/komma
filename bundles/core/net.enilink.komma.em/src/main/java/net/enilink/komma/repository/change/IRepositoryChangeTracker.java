/*******************************************************************************
 * Copyright (c) 2009, 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.repository.change;

public interface IRepositoryChangeTracker {
	/**
	 * Registers a listener which gets notified on repository changes
	 * 
	 * @param changeListener
	 *            repository change listener
	 */
	void addChangeListener(IRepositoryChangeListener changeListener);

	/**
	 * Unregisters a repository change listener
	 * 
	 * @param changeListener
	 *            repository change listener
	 */
	void removeChangeListener(IRepositoryChangeListener changeListener);
}