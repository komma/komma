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
package net.enilink.komma.common.adapter;

import java.util.Set;

public interface IAdapterSet extends Set<IAdapter> {
	/**
	 * Returns the adapter of the specified type.
	 * 
	 * @param type
	 *            the type of adapter.
	 * 
	 * @return an adapter from the set or null.
	 */
	IAdapter getAdapter(Object type);
}
