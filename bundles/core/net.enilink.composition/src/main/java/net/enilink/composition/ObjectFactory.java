/*******************************************************************************
 * Copyright (c) 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.composition;

import java.util.Collection;

/**
 * Interface for a factory that creates composite objects.
 * 
 * @param <T>
 *            The identifier type.
 */
public interface ObjectFactory<T> {
	/**
	 * Creates an object for the given Java types.
	 */
	<C> C createObject(Class<C> type, Class<?>... types);

	/**
	 * Creates an object for given rdf:types.
	 */
	Object createObject(Collection<T> types);

	/**
	 * Creates an object with assumed rdf:types.
	 */
	Object createObject(T... types);
}