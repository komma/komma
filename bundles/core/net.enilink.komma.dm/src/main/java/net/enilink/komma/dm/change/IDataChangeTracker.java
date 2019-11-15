/*******************************************************************************
 * Copyright (c) 2009, 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.dm.change;

public interface IDataChangeTracker {
	/**
	 * Registers a listener which gets notified on repository changes
	 * 
	 * @param changeListener
	 *            repository change listener
	 */
	void addChangeListener(IDataChangeListener changeListener);

	/**
	 * Unregisters a repository change listener
	 * 
	 * @param changeListener
	 *            repository change listener
	 */
	void removeChangeListener(IDataChangeListener changeListener);
}