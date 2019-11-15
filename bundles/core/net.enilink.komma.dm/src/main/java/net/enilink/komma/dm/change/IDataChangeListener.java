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

import java.util.List;

/**
 * Listener interface for changes of statements or namespaces.
 * 
 */
public interface IDataChangeListener {
	/**
	 * Called if some data changes occurred.
	 * 
	 * @param changes
	 *            The changes of some data set
	 */
	void dataChanged(List<IDataChange> changes);
}